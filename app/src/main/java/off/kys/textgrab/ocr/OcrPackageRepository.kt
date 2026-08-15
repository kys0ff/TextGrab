package off.kys.textgrab.ocr

import android.app.ActivityManager
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import off.kys.textgrab.core.model.OcrLanguage
import off.kys.textgrab.ocr.model.DownloadState
import off.kys.textgrab.ocr.model.OcrPackage
import off.kys.textgrab.ocr.model.OcrVersion
import off.kys.textgrab.ocr.model.TesseractVersion
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

private val Context.ocrDataStore by preferencesDataStore(
    name = "ocr_settings"
)

class OcrPackageRepository(
    private val context: Context
) {

    companion object {
        private const val FAST_BASE_URL =
            "https://github.com/tesseract-ocr/tessdata_fast/raw/main"

        private const val STANDARD_BASE_URL =
            "https://github.com/tesseract-ocr/tessdata/raw/main"

        private const val BEST_BASE_URL =
            "https://github.com/tesseract-ocr/tessdata_best/raw/main"

        /*
         * Model sizes can change when tessdata is updated.
         * Refresh cached values after this period.
         */
        private const val SIZE_CACHE_MAX_AGE_DAYS = 7L

        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 15_000

        private const val SIZE_PREFIX = "ocr_model_size_"
        private const val SIZE_UPDATED_PREFIX = "ocr_model_size_updated_"
    }

    /**
     * Internal description of a model whose remote size we can query.
     */
    private data class ModelReference(
        val tessCode: String,
        val version: TesseractVersion,
        val url: String
    ) {
        val key: String
            get() = "${tessCode}_${version.name}"
    }

    private val allModels: List<ModelReference> by lazy {
        val languages = listOf(
            "eng",
            "ara",
            "fra",
            "deu",
            "chi_sim",
            "jpn",
            "kor"
        )

        languages.flatMap { code ->
            listOf(
                ModelReference(
                    tessCode = code,
                    version = TesseractVersion.FAST,
                    url = "$FAST_BASE_URL/$code.traineddata"
                ),

                ModelReference(
                    tessCode = code,
                    version = TesseractVersion.STANDARD,
                    url = "$STANDARD_BASE_URL/$code.traineddata"
                ),

                ModelReference(
                    tessCode = code,
                    version = TesseractVersion.BEST,
                    url = "$BEST_BASE_URL/$code.traineddata"
                )
            )
        }
    }

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private val _downloadStates =
        MutableStateFlow<Map<String, DownloadState>>(emptyMap())

    val downloadStates: StateFlow<Map<String, DownloadState>> =
        _downloadStates.asStateFlow()

    /*
     * Cached model sizes currently available in memory.
     *
     * Key:
     *     eng_FAST
     *     eng_STANDARD
     *     eng_BEST
     */
    private val modelSizes =
        MutableStateFlow<Map<String, Long>>(emptyMap())

    val cachedModelSizes: StateFlow<Map<String, Long>> =
        modelSizes.asStateFlow()

    private val defaultVersionsState: StateFlow<Map<String, TesseractVersion>> =
        context.ocrDataStore.data.map { prefs ->
            val map = mutableMapOf<String, TesseractVersion>()
            val recommended = getRecommendedVersion()

            allModels.map { it.tessCode }.distinct().forEach { code ->
                val name = prefs[stringPreferencesKey("default_$code")]
                map[code] = name?.let {
                    runCatching { TesseractVersion.valueOf(it) }.getOrNull()
                } ?: recommended
            }
            map
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap()
        )

    val defaultVersions: Flow<Map<String, TesseractVersion>> =
        defaultVersionsState

    val packages: StateFlow<List<OcrPackage>> = combine(
        cachedModelSizes,
        downloadStates,
        defaultVersions
    ) { sizes, downloads, defaults ->
        buildPackages(sizes, downloads, defaults)
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = buildPackages(emptyMap(), emptyMap(), emptyMap())
    )

    init {
        scope.launch {
            launch { loadCachedSizes() }
            refreshStaleModelSizes()
            refreshInstallationStates()
        }
    }

    suspend fun getDefaultVersion(tessCode: String): TesseractVersion {
        val key = stringPreferencesKey("default_$tessCode")
        val name = context.ocrDataStore.data.map { it[key] }.first()
        return name?.let { runCatching { TesseractVersion.valueOf(it) }.getOrNull() }
            ?: getRecommendedVersion()
    }

    suspend fun setDefaultVersion(
        tessCode: String,
        version: TesseractVersion
    ) {
        context.ocrDataStore.edit { prefs ->
            prefs[stringPreferencesKey("default_$tessCode")] =
                version.name
        }
    }

    /**
     * Returns the current package list.
     *
     * Cached/known sizes are included immediately.
     *
     * The actual remote sizes are refreshed automatically in the
     * background by the repository.
     */
    fun getAvailablePackages(): List<OcrPackage> = packages.value

    /**
     * Manually trigger a background model-size refresh.
     *
     * Useful for pull-to-refresh or settings screens.
     */
    fun refreshModelSizesInBackground() {
        scope.launch {
            refreshStaleModelSizes(force = true)
        }
    }

    /**
     * Loads persisted model sizes from DataStore.
     */
    private suspend fun loadCachedSizes() {
        context.ocrDataStore.data
            .map { prefs ->
                val sizes = mutableMapOf<String, Long>()

                allModels.forEach { model ->
                    val size = prefs[
                        sizeKey(
                            model.tessCode,
                            model.version
                        )
                    ]

                    if (size != null && size > 0L) {
                        sizes[model.key] = size
                    }
                }

                sizes
            }
            .collect { sizes ->
                modelSizes.value = sizes
            }
    }

    private suspend fun refreshStaleModelSizes(
        force: Boolean = false
    ) {
        val now = System.currentTimeMillis()

        for (model in allModels) {
            // Skip remote refresh if model is already installed locally
            if (!force && isInstalled(model.tessCode, model.version))
                continue

            if (!force && !needsRefresh(model.key, now))
                continue

            val size = fetchRemoteFileSize(model.url)

            if (size != null && size > 0L) {
                saveModelSize(
                    model = model,
                    size = size,
                    timestamp = now
                )
            }
        }
    }

    /**
     * Determines whether a model's cached size should be refreshed.
     */
    private suspend fun needsRefresh(
        key: String,
        now: Long
    ): Boolean {
        val updatedKey = longPreferencesKey("$SIZE_UPDATED_PREFIX$key")
        val lastUpdated = context.ocrDataStore.data.map { it[updatedKey] ?: 0L }.first()

        if (lastUpdated <= 0L) {
            return true
        }

        val maxAge = TimeUnit.DAYS.toMillis(SIZE_CACHE_MAX_AGE_DAYS)
        return now - lastUpdated >= maxAge
    }

    /**
     * Persists the actual remote size and updates the in-memory cache.
     */
    private suspend fun saveModelSize(
        model: ModelReference,
        size: Long,
        timestamp: Long
    ) {
        context.ocrDataStore.edit { prefs ->
            prefs[sizeKey(model.tessCode, model.version)] = size
            prefs[
                updatedKey(
                    model.tessCode,
                    model.version
                )
            ] = timestamp
        }

        val updated = modelSizes.value.toMutableMap()
        updated[model.key] = size
        modelSizes.value = updated
    }

    private fun sizeKey(
        tessCode: String,
        version: TesseractVersion
    ) = longPreferencesKey(
        "$SIZE_PREFIX${tessCode}_${version.name}"
    )

    private fun updatedKey(
        tessCode: String,
        version: TesseractVersion
    ) = longPreferencesKey(
        "$SIZE_UPDATED_PREFIX${tessCode}_${version.name}"
    )

    /**
     * Fetches the actual file size.
     *
     * First attempts HEAD.
     *
     * GitHub/CDNs don't always provide Content-Length for HEAD,
     * so we fall back to a one-byte ranged GET.
     */
    private suspend fun fetchRemoteFileSize(
        url: String
    ): Long? = withContext(Dispatchers.IO) {

        fetchWithHead(url)?.let {
            return@withContext it
        }

        fetchWithRange(url)
    }

    private fun fetchWithHead(
        url: String
    ): Long? {
        var connection: HttpURLConnection? = null

        return try {
            connection =
                URL(url).openConnection() as HttpURLConnection

            connection.requestMethod = "HEAD"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.useCaches = false

            val responseCode =
                connection.responseCode

            if (responseCode in 200..299) {
                connection.contentLengthLong
                    .takeIf { it > 0L }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun fetchWithRange(
        url: String
    ): Long? {
        var connection: HttpURLConnection? = null

        return try {
            connection =
                URL(url).openConnection() as HttpURLConnection

            connection.requestMethod = "GET"

            /*
             * Only download one byte.
             *
             * Content-Range should contain the total file size:
             *
             * bytes 0-0/3921234
             */
            connection.setRequestProperty(
                "Range",
                "bytes=0-0"
            )

            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.useCaches = false

            val responseCode =
                connection.responseCode

            when (responseCode) {
                HttpURLConnection.HTTP_PARTIAL -> {
                    parseContentRange(
                        connection.getHeaderField(
                            "Content-Range"
                        )
                    )
                }
                in 200..299 -> {
                    connection.contentLengthLong
                        .takeIf { it > 0L }
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Parses:
     *
     * bytes 0-0/3921234
     *
     * and returns:
     *
     * 3921234
     */
    private fun parseContentRange(
        contentRange: String?
    ): Long? {
        if (contentRange.isNullOrBlank()) {
            return null
        }

        return contentRange
            .substringAfterLast("/")
            .toLongOrNull()
            ?.takeIf { it > 0L }
    }

    private fun buildPackages(
        sizes: Map<String, Long>,
        downloads: Map<String, DownloadState>,
        defaults: Map<String, TesseractVersion>
    ): List<OcrPackage> {
        return listOf(
            createPackage(
                OcrLanguage.LATIN,
                "English",
                "eng",
                sizes,
                downloads,
                defaults
            ),

            createPackage(
                OcrLanguage.ARABIC,
                "Arabic",
                "ara",
                sizes,
                downloads,
                defaults
            ),

            createPackage(
                OcrLanguage.FRENCH,
                "French",
                "fra",
                sizes,
                downloads,
                defaults
            ),

            createPackage(
                OcrLanguage.GERMAN,
                "German",
                "deu",
                sizes,
                downloads,
                defaults
            ),

            createPackage(
                OcrLanguage.CHINESE,
                "Chinese (Simplified)",
                "chi_sim",
                sizes,
                downloads,
                defaults
            ),

            createPackage(
                OcrLanguage.JAPANESE,
                "Japanese",
                "jpn",
                sizes,
                downloads,
                defaults
            ),

            createPackage(
                OcrLanguage.KOREAN,
                "Korean",
                "kor",
                sizes,
                downloads,
                defaults
            )
        )
    }

    private fun createPackage(
        lang: OcrLanguage,
        name: String,
        code: String,
        sizes: Map<String, Long>,
        downloads: Map<String, DownloadState>,
        defaults: Map<String, TesseractVersion>
    ): OcrPackage {

        fun size(version: TesseractVersion): Long = sizes["${code}_${version.name}"] ?: 0L

        fun download(version: TesseractVersion): DownloadState = downloads["${code}_$version"] ?: DownloadState.NotDownloaded

        val recommended = getRecommendedVersion()
        val defaultVersion = defaults[code] ?: recommended

        return OcrPackage(
            language = lang,
            displayName = name,
            tessCode = code,

            versions = TesseractVersion.entries.map { v ->
                OcrVersion(
                    version = v,
                    url = when (v) {
                        TesseractVersion.FAST -> "$FAST_BASE_URL/$code.traineddata"
                        TesseractVersion.STANDARD -> "$STANDARD_BASE_URL/$code.traineddata"
                        TesseractVersion.BEST -> "$BEST_BASE_URL/$code.traineddata"
                    },
                    sizeBytes = size(v),
                    isRecommended = v == recommended,
                    downloadState = download(v),
                    isDefault = v == defaultVersion
                )
            }
        )
    }

    private fun getRecommendedVersion(): TesseractVersion {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        val memoryInfo = ActivityManager.MemoryInfo()

        activityManager.getMemoryInfo(memoryInfo)

        val totalRamGb = memoryInfo.totalMem / (1024f * 1024f * 1024f)

        val cores = Runtime.getRuntime().availableProcessors()

        val isLowEnd = activityManager.isLowRamDevice

        return when {
            !isLowEnd && totalRamGb >= 6 && cores >= 8 ->
                TesseractVersion.BEST

            !isLowEnd && totalRamGb >= 3 && cores >= 4
                        -> TesseractVersion.STANDARD

            else -> TesseractVersion.FAST
        }
    }

    fun isInstalled(
        tessCode: String,
        version: TesseractVersion
    ): Boolean = TessDataStore.isInstalled(
        context,
        tessCode,
        version
    )

    fun updateDownloadState(key: String, state: DownloadState) {
        val updated = _downloadStates.value.toMutableMap()
        updated[key] = state
        _downloadStates.value = updated
    }

    fun refreshInstallationStates() {
        val states = mutableMapOf<String, DownloadState>()
        val sizes = modelSizes.value.toMutableMap()
        var sizesChanged = false

        getAvailablePackages().forEach { pkg ->
            pkg.versions.forEach { version ->
                val key = "${pkg.tessCode}_${version.version}"

                if (isInstalled(pkg.tessCode, version.version)) {
                    states[key] = DownloadState.Downloaded
                    
                    val localSize = TessDataStore.getInstalledSize(
                        context, pkg.tessCode, version.version
                    )
                    
                    if (localSize > 0 && sizes[key] != localSize) {
                        sizes[key] = localSize
                        sizesChanged = true
                    }
                } else if (_downloadStates.value[key] is DownloadState.Downloading) {
                    states[key] = _downloadStates.value[key]!!
                } else {
                    states[key] = DownloadState.NotDownloaded
                }
            }
        }

        _downloadStates.value = states
        if (sizesChanged) {
            modelSizes.value = sizes
        }
    }

    /**
     * Call this if the repository has a lifecycle independent of
     * the rest of the application, and you want to release its scope.
     */
    fun close() = scope.coroutineContext.cancel()
}
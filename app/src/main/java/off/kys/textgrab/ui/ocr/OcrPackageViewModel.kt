package off.kys.textgrab.ui.ocr

import android.content.Context
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import off.kys.textgrab.ocr.OcrDownloadService
import off.kys.textgrab.ocr.OcrEngine
import off.kys.textgrab.ocr.OcrPackageRepository
import off.kys.textgrab.ocr.TessDataStore
import off.kys.textgrab.ocr.model.DownloadState
import off.kys.textgrab.ocr.model.OcrPackage
import off.kys.textgrab.ocr.model.TesseractVersion

data class OcrPackageState(
    val packages: List<OcrPackage> = emptyList()
)

sealed interface OcrPackageEvent {
    data class Download(val pkg: OcrPackage, val version: TesseractVersion, val url: String) : OcrPackageEvent
    data class Delete(val pkg: OcrPackage, val version: TesseractVersion) : OcrPackageEvent
    data class SetDefault(val pkg: OcrPackage, val version: TesseractVersion) : OcrPackageEvent
    data object Refresh : OcrPackageEvent
}

class OcrPackageViewModel(
    private val context: Context,
    private val repository: OcrPackageRepository,
    private val ocrEngine: OcrEngine
) : ScreenModel {

    val state: StateFlow<OcrPackageState> = repository.packages
        .map { OcrPackageState(it) }
        .stateIn(
            scope = screenModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OcrPackageState()
        )

    init {
        repository.refreshInstallationStates()
    }

    fun onEvent(event: OcrPackageEvent) {
        when (event) {
            is OcrPackageEvent.Download -> download(event.pkg, event.version, event.url)
            is OcrPackageEvent.Delete -> delete(event.pkg, event.version)
            is OcrPackageEvent.SetDefault -> setDefault(event.pkg, event.version)
            OcrPackageEvent.Refresh -> refresh()
        }
    }

    override fun onDispose() {
        super.onDispose()
        screenModelScope.launch {
            ocrEngine.close()
        }
    }

    private fun refresh() {
        repository.refreshModelSizesInBackground()
    }

    private fun download(pkg: OcrPackage, version: TesseractVersion, url: String) {
        OcrDownloadService.start(
            context = context,
            tessCode = pkg.tessCode,
            version = version,
            url = url,
            displayName = pkg.displayName
        )
    }

    private fun delete(pkg: OcrPackage, version: TesseractVersion) {
        val key = "${pkg.tessCode}_$version"
        if (TessDataStore.delete(context, pkg.tessCode, version)) {
            repository.updateDownloadState(key, DownloadState.NotDownloaded)
            repository.refreshInstallationStates()
        }
    }

    private fun setDefault(pkg: OcrPackage, version: TesseractVersion) {
        screenModelScope.launch {
            repository.setDefaultVersion(pkg.tessCode, version)
        }
    }
}

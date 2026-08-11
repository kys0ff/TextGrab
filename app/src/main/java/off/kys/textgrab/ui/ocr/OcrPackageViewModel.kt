package off.kys.textgrab.ui.ocr

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import off.kys.textgrab.ServiceLocator
import off.kys.textgrab.ocr.TessDataStore
import off.kys.textgrab.ocr.model.DownloadState
import off.kys.textgrab.ocr.model.OcrPackage
import off.kys.textgrab.ocr.model.TesseractVersion

class OcrPackageViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ServiceLocator.ocrPackages

    private val _packages = MutableStateFlow<List<OcrPackage>>(repository.getAvailablePackages())
    val packages: StateFlow<List<OcrPackage>> = _packages.asStateFlow()

    val downloadStates = repository.downloadStates

    init {
        repository.refreshInstallationStates()
    }

    fun download(pkg: OcrPackage, version: TesseractVersion, url: String) {
        val key = "${pkg.tessCode}_$version"
        viewModelScope.launch {
            // Update state to downloading
            updateDownloadState(key, DownloadState.Downloading(0f))

            val success = TessDataStore.download(
                getApplication(),
                pkg.tessCode,
                version,
                url,
                onProgress = { progress ->
                    updateDownloadState(key, DownloadState.Downloading(progress))
                }
            )

            if (success) {
                updateDownloadState(key, DownloadState.Downloaded)
                repository.refreshInstallationStates()
            } else {
                updateDownloadState(key, DownloadState.Error("Download failed"))
            }
        }
    }

    fun delete(pkg: OcrPackage, version: TesseractVersion) {
        val key = "${pkg.tessCode}_$version"
        if (TessDataStore.delete(getApplication(), pkg.tessCode, version)) {
            updateDownloadState(key, DownloadState.NotDownloaded)
            repository.refreshInstallationStates()
        }
    }

    private fun updateDownloadState(key: String, state: DownloadState) {
        repository.updateDownloadState(key, state)
    }
}

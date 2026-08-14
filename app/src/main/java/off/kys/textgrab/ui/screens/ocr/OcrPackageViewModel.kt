package off.kys.textgrab.ui.screens.ocr

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

class OcrPackageViewModel(
    private val application: Application,
    private val repository: OcrPackageRepository,
    private val ocrEngine: OcrEngine
) : AndroidViewModel(application) {

    val state: StateFlow<OcrPackageState> = repository.packages
        .map { OcrPackageState(it) }
        .stateIn(
            scope = viewModelScope,
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

    override fun onCleared() {
        viewModelScope.launch {
            ocrEngine.close()
        }
    }

    private fun refresh() {
        repository.refreshModelSizesInBackground()
    }

    private fun download(pkg: OcrPackage, version: TesseractVersion, url: String) {
        OcrDownloadService.start(
            context = application,
            tessCode = pkg.tessCode,
            version = version,
            url = url,
            displayName = pkg.displayName
        )
    }

    private fun delete(pkg: OcrPackage, version: TesseractVersion) {
        val key = "${pkg.tessCode}_$version"
        if (TessDataStore.delete(application, pkg.tessCode, version)) {
            repository.updateDownloadState(key, DownloadState.NotDownloaded)
            repository.refreshInstallationStates()
        }
    }

    private fun setDefault(pkg: OcrPackage, version: TesseractVersion) {
        viewModelScope.launch {
            repository.setDefaultVersion(pkg.tessCode, version)
        }
    }
}

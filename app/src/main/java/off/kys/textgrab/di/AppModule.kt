package off.kys.textgrab.di

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import off.kys.textgrab.core.clipboard.ClipboardHelper
import off.kys.textgrab.core.permission.PermissionManager
import off.kys.textgrab.data.HistoryRepository
import off.kys.textgrab.ocr.OcrEngine
import off.kys.textgrab.ocr.OcrPackageRepository
import off.kys.textgrab.ui.screens.main.MainViewModel
import off.kys.textgrab.ui.screens.ocr.OcrPackageViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.koin.dsl.onClose

@OptIn(DelicateCoroutinesApi::class)
val appModule = module {
    single { HistoryRepository(androidContext()) }
    single { OcrPackageRepository(androidContext()) }.onClose { it?.close() }
    single { ClipboardHelper(androidContext(), get()) }
    single { PermissionManager(androidContext()) }
    single { OcrEngine(get()) }.onClose { engine ->
        GlobalScope.launch { engine?.close() }
    }

    factoryOf(::MainViewModel)
    factoryOf(::OcrPackageViewModel)
}

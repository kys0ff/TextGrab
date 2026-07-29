package off.kys.textgrab.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.OcrLanguage
import off.kys.textgrab.overlay.ui.OverlayScreen

/**
 * Adds and removes the Compose-powered floating overlay window
 * (`TYPE_APPLICATION_OVERLAY`, backed by SYSTEM_ALERT_WINDOW).
 *
 * A [ComposeView] cannot be attached to an arbitrary `WindowManager` window unless
 * it is given ViewTree owners for lifecycle, view-model store and saved state — the
 * three Compose relies on. [OverlayLifecycleOwner] supplies all three.
 */
class OverlayController(
    private val context: Context,
    private val onCopyAll: (List<String>, ExtractionMode) -> Unit,
    private val onSwitchMode: (ExtractionMode) -> Unit,
    private val onSwitchLanguage: (OcrLanguage) -> Unit,
    private val onRescan: () -> Unit,
    private val onClose: () -> Unit,
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val lifecycleOwner = OverlayLifecycleOwner()
    private var composeView: ComposeView? = null
    private var attached = false

    private fun buildLayoutParams(): WindowManager.LayoutParams {
        val type =
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                }
            }
        }
    }

    fun show() {
        if (attached) return
        val view = composeView ?: createView().also { composeView = it }
        lifecycleOwner.moveToResumed()
        runCatching { windowManager.addView(view, buildLayoutParams()) }
            .onSuccess { attached = true }
    }

    fun hide() {
        val view = composeView ?: return
        if (!attached) return
        runCatching { windowManager.removeViewImmediate(view) }
        attached = false
        lifecycleOwner.moveToCreated()
    }

    fun destroy() {
        hide()
        lifecycleOwner.moveToDestroyed()
        composeView = null
    }

    private fun createView(): ComposeView {
        lifecycleOwner.moveToCreated()
        return ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                OverlayScreen(
                    onCopyAll = onCopyAll,
                    onSwitchMode = onSwitchMode,
                    onSwitchLanguage = onSwitchLanguage,
                    onRescan = onRescan,
                    onClose = onClose,
                )
            }
        }
    }
}

/**
 * A self-contained lifecycle / view-model / saved-state owner for a window that has
 * no Activity behind it.
 */
private class OverlayLifecycleOwner : SavedStateRegistryOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private var restored = false

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    fun moveToCreated() {
        if (!restored) {
            savedStateController.performRestore(null)
            restored = true
        }
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun moveToResumed() {
        moveToCreated()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun moveToDestroyed() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}

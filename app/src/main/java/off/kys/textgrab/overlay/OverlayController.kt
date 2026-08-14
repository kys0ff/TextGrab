package off.kys.textgrab.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
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
    private val onClose: () -> Unit,
    private val onOpenDownload: () -> Unit,
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val lifecycleOwner = OverlayLifecycleOwner()
    private var overlayView: View? = null
    private var attached = false

    private fun buildLayoutParams(isScrollMode: Boolean): WindowManager.LayoutParams {
        val type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

        val (w, h) = if (isScrollMode) {
            WindowManager.LayoutParams.WRAP_CONTENT to WindowManager.LayoutParams.WRAP_CONTENT
        } else {
            WindowManager.LayoutParams.MATCH_PARENT to WindowManager.LayoutParams.MATCH_PARENT
        }

        val flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

        return WindowManager.LayoutParams(
            w,
            h,
            type,
            if (isScrollMode) flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL else flags,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = if (isScrollMode) Gravity.BOTTOM or Gravity.END else Gravity.TOP or Gravity.START
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
        val view = overlayView ?: createView().also { overlayView = it }
        lifecycleOwner.moveToResumed()
        runCatching { windowManager.addView(view, buildLayoutParams(OverlayBus.isScrollMode.value)) }
            .onSuccess { attached = true }
    }

    fun updateScrollMode(enabled: Boolean) {
        val view = overlayView ?: return
        if (!attached) return
        runCatching { windowManager.updateViewLayout(view, buildLayoutParams(enabled)) }
    }

    fun hide() {
        val view = overlayView ?: return
        if (!attached) return
        runCatching { windowManager.removeViewImmediate(view) }
        attached = false
        lifecycleOwner.moveToCreated()
    }

    fun destroy() {
        hide()
        lifecycleOwner.moveToDestroyed()
        overlayView = null
    }

    private fun createView(): View {
        lifecycleOwner.moveToCreated()
        val composeView = ComposeView(context).apply {
            setContent {
                OverlayScreen(
                    onCopyAll = onCopyAll,
                    onOpenDownload = onOpenDownload,
                    onClose = onClose,
                )
            }
        }

        return object : FrameLayout(context) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    if (lifecycleOwner.onBackPressedDispatcher.hasEnabledCallbacks()) {
                        lifecycleOwner.onBackPressedDispatcher.onBackPressed()
                        return true
                    }
                }
                return super.dispatchKeyEvent(event)
            }
        }.apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeOnBackPressedDispatcherOwner(lifecycleOwner)
            addView(composeView)
        }
    }
}

/**
 * A self-contained lifecycle / view-model / saved-state owner for a window that has
 * no Activity behind it.
 */
private class OverlayLifecycleOwner : SavedStateRegistryOwner, ViewModelStoreOwner, OnBackPressedDispatcherOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private var restored = false

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    override val onBackPressedDispatcher = OnBackPressedDispatcher()

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

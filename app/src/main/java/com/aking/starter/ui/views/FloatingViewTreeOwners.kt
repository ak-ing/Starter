package com.aking.starter.ui.views

import android.view.View
import android.view.View.OnAttachStateChangeListener
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * @author Ak
 * 2025/3/24  11:48
 */
class FloatingViewTreeOwners : SavedStateRegistryOwner, OnAttachStateChangeListener {
    // LifecycleRegistry
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    // SavedStateRegistry
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    // ReComposer
    private val coroutineContext = AndroidUiDispatcher.CurrentThread
    private val runRecomposeScope = CoroutineScope(coroutineContext)
    val reComposer = Recomposer(coroutineContext).also {
        it.pauseCompositionFrameClock()
    }

    override fun onViewAttachedToWindow(v: View) {
        initializeViewTreeOwners(v)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        reComposer.resumeCompositionFrameClock()
    }

    override fun onViewDetachedFromWindow(v: View) {
        reComposer.pauseCompositionFrameClock()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    fun release() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        reComposer.cancel()
    }

    private fun initializeViewTreeOwners(target: View) {
        target.setViewTreeLifecycleOwner(this)
        target.setViewTreeSavedStateRegistryOwner(this)

        if (savedStateRegistry.isRestored) return
        // init ViewTreeOwners
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        runRecomposeScope.launch(start = CoroutineStart.UNDISPATCHED) {
            reComposer.runRecomposeAndApplyChanges()
        }
    }
}
package com.aking.starter.screens.floating

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
    private lateinit var lifecycleRegistry: LifecycleRegistry
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    // SavedStateRegistry
    private lateinit var savedStateRegistryController: SavedStateRegistryController
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    // ReComposer
    private val coroutineContext = AndroidUiDispatcher.CurrentThread
    private val runRecomposeScope = CoroutineScope(coroutineContext)
    val reComposer = Recomposer(coroutineContext)

    init {
        runRecomposeScope.launch(start = CoroutineStart.UNDISPATCHED) {
            reComposer.runRecomposeAndApplyChanges()
        }
    }

    override fun onViewAttachedToWindow(v: View) {
        initializeViewTreeOwners(v)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        reComposer.resumeCompositionFrameClock()
    }

    override fun onViewDetachedFromWindow(v: View) {
        reComposer.pauseCompositionFrameClock()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    private fun initializeViewTreeOwners(target: View) {
        // resetState
        lifecycleRegistry = LifecycleRegistry(this)
        savedStateRegistryController = SavedStateRegistryController.create(this)
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        target.setViewTreeLifecycleOwner(this)
        target.setViewTreeSavedStateRegistryOwner(this)
    }

    fun release() {
        reComposer.cancel()
    }
}
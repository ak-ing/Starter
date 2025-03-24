package com.aking.starter.screens.floating

import android.view.View
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

/**
 * @author Ak
 * 2025/3/24  11:48
 */
class FloatingViewTreeOwnersHolder : SavedStateRegistryOwner, ViewModelStoreOwner {
    // LifecycleRegistry
    private val lifecycleRegistry: LifecycleRegistry by lazy {
        LifecycleRegistry(this)
    }
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    // SavedStateRegistry
    private val savedStateRegistryController: SavedStateRegistryController =
        SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    // ViewModelStore
    override val viewModelStore = ViewModelStore()

    init {
        savedStateRegistryController.performAttach()
    }

    fun attachToWindow(target: View) {
        initializeViewTreeOwners(target)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun detachFromWindow() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    private fun initializeViewTreeOwners(target: View) {
        target.setViewTreeLifecycleOwner(this)
        target.setViewTreeSavedStateRegistryOwner(this)
        target.setViewTreeViewModelStoreOwner(this)
    }
}
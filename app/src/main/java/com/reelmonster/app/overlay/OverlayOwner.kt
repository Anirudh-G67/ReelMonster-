package com.reelmonster.app.overlay

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/** Lets ComposeView live inside a Service-owned window (no Activity). */
class OverlayOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    init {
        controller.performAttach()
        controller.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED
    }

    fun resume() {
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        registry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }

    override val lifecycle: Lifecycle get() = registry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry
}

package com.aking.starter.ui.views

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.reflect.KClass

/**
 * @author Ak
 * 2025/3/28  16:31
 */
object FloatingViewManager {

    private val floatingViews: MutableMap<KClass<out BaseFloatingComposeView>, BaseFloatingComposeView> = mutableMapOf()

    /**
     * 获取已存在的悬浮窗实例，如果不存在则创建一个新的。
     *
     * @param block 用于在悬浮窗不存在时创建一个新的实例。
     * @return 已存在的或新创建的悬浮窗实例。
     */
    internal inline fun <reified T : BaseFloatingComposeView> getOrCreate(block: () -> T): T {
        return floatingViews.getOrPut(T::class) {
            block().also { addOnDestroyCleanup(it) }
        } as T
    }

    /**
     * 观察者侦听ON_DESTROY事件。
     * 它会从floatingViews中删除 target，从而在其关联的生命周期被销毁时有效地清理悬浮窗引用。
     */
    private fun <T : BaseFloatingComposeView> addOnDestroyCleanup(target: T) {
        target.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event != Lifecycle.Event.ON_DESTROY) return
                floatingViews.remove(target::class)
            }
        })
    }

}


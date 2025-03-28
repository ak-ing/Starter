package com.aking.starter.ui.views

/**
 * @author Ak
 * 2025/3/28  16:31
 */
object FloatingViewManager {

    private val floatingViews: MutableMap<Class<out BaseFloatingComposeView>, BaseFloatingComposeView> = mutableMapOf()

    /**
     * 获取已存在的浮动视图实例，如果不存在则创建一个新的。
     *
     * @param block 一个 lambda 函数，用于在浮动视图不存在时创建一个新的实例。
     * @return 已存在的或新创建的浮动视图实例。
     */
    internal inline fun <reified T : BaseFloatingComposeView> getOrCreate(block: () -> T): T {
        return floatingViews.getOrPut(T::class.java) { block() } as T
    }


}


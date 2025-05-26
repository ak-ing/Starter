package com.aking.starter.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * @author Created by Ak on 2025-05-25 15:57.
 */

/**
 * Debounce function that cancels the previous job before starting a new one.
 */
fun debounceCancelBefore(
    coroutineScope: CoroutineScope,
    callback: suspend () -> Unit
): () -> Unit {
    var job: Job? = null
    return {
        job?.cancel()
        job = coroutineScope.launch { callback() }
    }
}

/**
 * Debounce function that retains the newest value.
 */
fun <T> debounceRetainNewest(
    coroutineScope: CoroutineScope,
    callback: suspend (T) -> Unit
): (T) -> Unit {
    var job: Job? = null
    var awaitFun: (suspend () -> Unit)? = null
    return { t: T ->
        awaitFun = {
            callback(t)
            if (awaitFun != null) {
                job = coroutineScope.launch {
                    awaitFun?.invoke()
                }
                awaitFun = null
            }
        }
        if (job?.isActive != true) {
            job = coroutineScope.launch {
                awaitFun?.let {
                    awaitFun = null
                    it()
                }
            }
        }
    }
}
package com.hifnawy.alquran.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration

/**
 * An object that provides extension functions for [Flow].
 *
 * This object groups together custom operators for reactive streams using Kotlin's [Flow].
 * The extensions aim to provide functionality not available in the standard library or to
 * simplify common reactive programming patterns.
 *
 * The primary functions offered are `throttleFirst`, which helps in rate-limiting emissions
 * from a flow, particularly useful for handling rapid user inputs like button clicks to
 * prevent multiple actions from being triggered in a short period.
 *
 * Overloads are provided for convenience, allowing the time window for throttling to be
 * specified either as a `Long` in milliseconds or as a [Duration] object, offering better
 * readability and type safety.
 *
 * Example Usage:
 * ```kotlin
 * someFlow
 *     .throttleFirst(500L) // or throttleFirst(500.milliseconds)
 *     .onEach { item ->
 *         // This block will only execute at most once every 500ms
 *         processItem(item)
 *     }
 *     .launchIn(someScope)
 * ```
 *
 * All functions within this object are implemented as extension functions on `Flow<T>`,
 * allowing for seamless chaining with other standard flow operators.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see Flow
 * @see throttleFirst
 */
object FlowEx {

    /**
     * Returns a flow that emits only the first item emitted by the original flow during a specified time window.
     *
     * This operator ignores subsequent items from the source flow until the time window has passed,
     * after which it will emit the next item.
     *
     * @param windowMs [Long] The time window in milliseconds during which to ignore subsequent items.
     *
     * @return [Flow< T >][Flow] A [Flow] that performs the throttle operation.
     */
    fun <T> Flow<T>.throttleFirst(windowMs: Long): Flow<T> = flow {
        var lastTime = 0L

        collect { value ->
            val now = System.currentTimeMillis()
            if (now - lastTime >= windowMs) {
                lastTime = now
                emit(value)
            }
        }
    }

    /**
     * Returns a flow that emits only the first item emitted by the original flow during a specified time window.
     *
     * This operator ignores subsequent items from the source flow until the time window has passed,
     * after which it will emit the next item.
     *
     * @param window [Duration] The time window [Duration] during which to ignore subsequent items.
     *
     * @return [Flow<T>][Flow] A flow that performs the throttle operation.
     */
    fun <T> Flow<T>.throttleFirst(window: Duration): Flow<T> = throttleFirst(window.inWholeMilliseconds)
}

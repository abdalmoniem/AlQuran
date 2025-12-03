package com.hifnawy.alquran

import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.activity.OnBackPressedDispatcher

/**
 * A composable function that handles predictive back gestures, providing a way to create
 * custom animations and state changes in response to the user's back navigation.
 *
 * This function integrates with the AndroidX [OnBackPressedDispatcher] to listen for
 * back gesture events. It allows the UI to react progressively as the gesture is performed,
 * for instance, by animating a component's size or position.
 *
 * The handler is controlled by the [isBackHandlerEnabled] flag. When a gesture starts,
 * it invokes [onBackStarted]. As the user continues the gesture, [onBackProgress] is called
 * repeatedly with the progress of the gesture (a float from 0.0 to 1.0). If the gesture is
 * completed, [onBackPressed] is called. If it's cancelled, the progress is reset to 0f via
 * [onBackProgress].
 *
 * This composable uses a [DisposableEffect] to properly add and remove the back-press callback
 * from the [OnBackPressedDispatcher], ensuring there are no memory leaks.
 *
 * @param isBackHandlerEnabled [Boolean] A boolean to enable or disable this back handler. When `false`,
 *   the handler will not intercept back gestures.
 * @param onBackStarted [() -> Unit][onBackStarted] A lambda that is invoked when a predictive back gesture has started.
 * @param onBackProgress [suspend CoroutineScope.(Float) -> Unit][onBackProgress] A suspend lambda that receives the progress
 *   of the back gesture as a [Float].
 *   This is called multiple times as the gesture is being performed. The value ranges from 0.0 to 1.0.
 *   It is also called with 0f when the gesture is cancelled.
 * @param onBackPressed [() -> Unit][onBackPressed] A lambda that is invoked when the back gesture is completed and the back
 *   event should be fully handled (e.g., navigating back or closing a component).
 */
@Composable
fun PredictiveBackHandler(
        isBackHandlerEnabled: Boolean,
        onBackStarted: () -> Unit = {},
        onBackProgress: suspend CoroutineScope.(Float) -> Unit = {},
        onBackPressed: () -> Unit = {}
) {
    val onBackCallback = remember {
        object : OnBackPressedCallback(isBackHandlerEnabled) {
            private val coroutineScope = CoroutineScope(Dispatchers.Main)
            override fun handleOnBackStarted(backEvent: BackEventCompat) = onBackStarted()

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                coroutineScope.launch { onBackProgress(backEvent.progress) }
            }

            override fun handleOnBackPressed() = onBackPressed()

            override fun handleOnBackCancelled() {
                coroutineScope.launch { onBackProgress(0f) }
            }
        }
    }

    val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    LaunchedEffect(isBackHandlerEnabled) {
        onBackCallback.isEnabled = isBackHandlerEnabled
    }

    DisposableEffect(backPressedDispatcher) {
        backPressedDispatcher?.addCallback(onBackCallback)
        onDispose { onBackCallback.remove() }
    }
}

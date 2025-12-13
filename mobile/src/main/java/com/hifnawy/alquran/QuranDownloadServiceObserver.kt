package com.hifnawy.alquran

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.hifnawy.alquran.shared.domain.QuranDownloadService
import com.hifnawy.alquran.shared.domain.QuranDownloadService.QuranDownloadManager
import com.hifnawy.alquran.shared.domain.QuranDownloadService.QuranDownloadManager.DownloadState
import com.hifnawy.alquran.shared.domain.QuranDownloadService.QuranDownloadManager.DownloadStatusObserver

/**
 * A Composable that observes the state of [QuranDownloadService] downloads.
 *
 * This function provides a declarative and lifecycle-aware way to subscribe to download status
 * updates from the [QuranDownloadService] within a Jetpack Compose UI. It uses a [DisposableEffect]
 * to register the provided [observer] when the Composable enters the composition and automatically
 * unregisters it when it leaves, preventing memory leaks.
 *
 * When the download state changes (e.g., `starts`, `progresses`, `completes`, or `fails`), the [observer]'s
 * [onDownloadStateChanged][DownloadStatusObserver.onDownloadStateChanged] callback will be invoked
 * with the new [DownloadState].
 *
 * **Example Usage:**
 * ```kotlin
 * @Composable
 * fun DownloadScreen() {
 *     var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.State.STOPPED) }
 *
 *     QuranDownloadServiceObserver { newState ->
 *         // This block will run whenever the download state changes.
 *         downloadState = newState
 *     }
 *
 *     // UI that reacts to the downloadState, e.g., showing a progress bar.
 *     when (val state = downloadState) {
 *         is DownloadState.State.DOWNLOADING -> {
 *             Text("Downloading: ${state.progress}%")
 *             LinearProgressIndicator(progress = state.progress / 100f)
 *         }
 *         is DownloadState.State.COMPLETED -> Text("Download complete!")
 *         is DownloadState.State.FAILED -> Text("Error: ${state.failureReason}")
 *    }
 * }
 */

@Composable
@SuppressLint("UnsafeOptInUsageError")
fun QuranDownloadServiceObserver(observer: DownloadStatusObserver) {
    val quranDownloadManager = remember { QuranDownloadManager }

    DisposableEffect(quranDownloadManager) {
        quranDownloadManager.downloadStatusObservers.add(observer)

        onDispose { quranDownloadManager.downloadStatusObservers.remove(observer) }
    }
}

package com.hifnawy.alquran

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.hifnawy.alquran.shared.domain.QuranDownloadService
import com.hifnawy.alquran.shared.domain.QuranDownloadService.QuranDownloadManager.DownloadState
import com.hifnawy.alquran.shared.domain.QuranDownloadService.QuranDownloadManager.DownloadStatusObserver

@Composable
@SuppressLint("UnsafeOptInUsageError")
fun QuranDownloadServiceObserver(observer: DownloadStatusObserver) {
    val quranDownloadService = remember { QuranDownloadService }

    val observer = remember {
        object : DownloadStatusObserver {
            override fun onDownloadStateChanged(downloadState: DownloadState) {
                observer.onDownloadStateChanged(downloadState)
            }
        }
    }

    DisposableEffect(quranDownloadService) {
        quranDownloadService.downloadServiceObservers.add(observer)

        onDispose { quranDownloadService.downloadServiceObservers.remove(observer) }
    }
}

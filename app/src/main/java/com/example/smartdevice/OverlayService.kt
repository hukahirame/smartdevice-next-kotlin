package com.example.smartdevice

import android.app.Service
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class OverlayService : Service() {
    private lateinit var overlay: OverlayComponent

    override fun onCreate() {
        super.onCreate()
        overlay = OverlayComponent(this)
        overlay.createOverlay()
        overlay.showOverlay("OverlayService")

        startForeground(1, NotificationComponent().createNotification(this))
        isOverlayActive = true

        // WorkManagerで1時間後にサービス停止をスケジュール（起動ごとに予約し直す）
        val stopServiceWork = OneTimeWorkRequest.Builder(
            StopServiceWorker::class.java
        )
            .setInitialDelay(1, TimeUnit.HOURS) // 1時間後
            .build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            STOP_WORK_NAME, ExistingWorkPolicy.REPLACE, stopServiceWork
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        isOverlayActive = false
        overlay.destroyOverlay()
        // 停止済みなので自動停止の予約を取り消す
        WorkManager.getInstance(this).cancelUniqueWork(STOP_WORK_NAME)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // alpha値の処理
        intent?.getFloatExtra("alpha",-1f)?.let { alpha ->
            if(alpha >= 0f) overlay.syncAlpha(alpha)
        }
        // 通常/暖色の切替
        intent?.getStringExtra("nightUI")?.let { mode ->
            overlay.switchNightUI(mode)
        }
        return START_STICKY
    }


    override fun onBind(intent: Intent?) = null

    companion object {
        var isOverlayActive: Boolean = false
        private const val STOP_WORK_NAME = "stopOverlayService"
    }
}


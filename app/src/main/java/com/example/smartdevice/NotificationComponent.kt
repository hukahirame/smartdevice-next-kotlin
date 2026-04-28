package com.example.smartdevice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class NotificationComponent {
    // 通知チャンネルの作成
    fun createNotification(context: Context): Notification {
        var channelId = ""
        var channelName = ""
        var contentText = ""

        // チャンネル名を設定
        if (context is OverlayService) {
            channelId = "overlay_service_channel"
            channelName = "Overlay Service Channel"
            contentText = "ナイトディスプレイ起動中"
        } else if (context is AudioMonitorService) {
            channelId = "audio_monitor_service_channel"
            channelName = "Audio Monitor Service"
            contentText = "爆音キャンセラー起動中"
        }

        // 通知チャンネルを作成
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        // 通知の作成
        val notificationIntent = Intent(
            context,
            MainActivity::class.java
        )
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle("スマデバ")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 必要に応じてアイコンを変更
            .setContentIntent(pendingIntent)
            .build()
    }
}



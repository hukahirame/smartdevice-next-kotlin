package com.example.smartdevice

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters

class StopServiceWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        // サービス停止処理
        val context = applicationContext
        val stopIntent = Intent(context, OverlayService::class.java)
        context.stopService(stopIntent)
        return Result.success()
    }
}

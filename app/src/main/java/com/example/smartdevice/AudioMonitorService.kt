package com.example.smartdevice

import android.Manifest
import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.sqrt

class AudioMonitorService : Service() {
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var overlay: OverlayComponent? = null
    private var audioManager: AudioManager? = null
    private val systemMaxVolume = 70f //本当は85相当だが、暗黙量がある
    private val audioOutputReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                deviceType = currentDeviceType()
                Log.d(TAG, "Audio output is becoming noisy. Update output device info.")
            }
        }
    }
    private var deviceType = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
    private var captureThread: Thread? = null
    @Volatile private var adjustTicket = 0 // 自動で下げた音量の段階数（停止時に戻す）

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        // オーバーレイ権限がある場合のみ音量表示を出す（無い場合も減音機能は動かす）
        if (Settings.canDrawOverlays(this)) {
            overlay = OverlayComponent(this)
            overlay!!.createOverlay()
            overlay!!.showOverlay("AudioMonitorService")
        }

        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(audioOutputReceiver, filter)
        deviceType = currentDeviceType()

        startForeground(1, NotificationComponent().createNotification(this))
        isMonitoring = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // MediaProjectionの許可は再利用できないため、再起動時（intent == null）は停止する
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        val resultCode = intent.getIntExtra("resultCode", -1)
        val data = IntentCompat.getParcelableExtra(
            intent, "data",
            Intent::class.java
        )
        if (resultCode == Activity.RESULT_OK && data != null) {
            mediaProjection = mediaProjectionManager!!.getMediaProjection(resultCode, data)
            startAudioCapture()
        } else {
            stopSelf()
            Log.e(TAG, "Invalid resultCode or data, stopping service.")
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudioCapture()
        unregisterReceiver(audioOutputReceiver)
        if (overlay != null) {
            overlay!!.hideOverlay("AudioMonitorService")
            overlay!!.destroyOverlay()
        }
        isMonitoring = false
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun startAudioCapture() {
        Log.d(TAG, " startAudioCapture Begin.")
        // 権限のチェック
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "RECORD_AUDIO permission is not granted")
            stopSelf()
            return
        }
        if (mediaProjection == null) {
            stopSelf()
            return
        }

        val config =
            AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

        val bufferSize = AudioRecord.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = AudioRecord.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(44100)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        try {
            audioRecord?.startRecording()
            isMonitoring = true
        } catch (e: IllegalStateException) {
            stopSelf()
            Log.e(TAG, "Failed to start audio recording", e)
            return
        } catch (e: SecurityException) {
            stopSelf()
            Log.e(TAG, "SecurityException occurred", e)
            return
        }

        captureThread = Thread {
            val buffer = ShortArray(bufferSize)
            //android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
            while (isMonitoring) {
                val record = audioRecord ?: break
                val readSize = record.read(buffer, 0, buffer.size)
                val amplitude = calculateRMS(buffer, readSize)

                // システム音量レベルの取得と正規化
                val currentOffset = audioManager!!.getStreamVolumeDb(
                    AudioManager.STREAM_MUSIC,
                    audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC),
                    deviceType
                )
                val decibel =
                    20 * log10(amplitude / 32767.0) + systemMaxVolume + currentOffset
                Log.d(
                    "AudioMonitorService",
                    "x: " + 20 * log10(amplitude / 32767.0)
                )

                overlay?.let { ov ->
                    Handler(Looper.getMainLooper()).post { //テキスト表示
                        ov.decibelTextView.text = ov.decibelText(decibel)
                    }
                }

                if (decibel > thresholdDb) {
                    val downDb =
                        floor((decibel - thresholdDb) / 4).toInt()
                    for (i in 0 until downDb) {
                        audioManager!!.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            -1, AudioManager.FLAG_VIBRATE
                        )
                        adjustTicket++
                        Log.d(
                            TAG,
                            "adjustTicket PLUS: $adjustTicket"
                        )
                    }
                } else if (adjustTicket > 0) {
                    // if(adjustTicket > 5) adjustTicket = 5;
                    audioManager!!.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_VIBRATE
                    )
                    adjustTicket--
                    Log.d(
                        TAG,
                        "adjustTicket MINUS: $adjustTicket"
                    )
                } else if (readSize == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e(
                        TAG,
                        "AudioRecord.read() returned ERROR_INVALID_OPERATION"
                    )
                    isMonitoring = false
                } else if (readSize == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(
                        TAG,
                        "AudioRecord.read() returned ERROR_BAD_VALUE"
                    )
                    isMonitoring = false
                }
            }
        }.also { it.start() }
    }

    private fun currentDeviceType(): Int {
        // 出力用デバイス（sink）をすべて取得
        val devices = audioManager!!.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        // 現在選択中または優先される出力デバイスを選ぶ
        // ※ 複数ある場合には、より適切な選択基準（例えば接続状態やユーザーの選択など）を設ける必要があります。
        var currentDevice = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER // デフォルトは内蔵スピーカー
        for (device in devices) {
            if (device.isSink) {
                // 簡易的に最初の出力デバイスを取得する例
                currentDevice = device.type
                break
            }
        }
        if (currentDevice == AudioDeviceInfo.TYPE_BUILTIN_MIC || currentDevice == AudioDeviceInfo.TYPE_FM_TUNER || currentDevice == AudioDeviceInfo.TYPE_TV_TUNER) {
            // 入力系デバイスなので、代わりにデフォルトの出力デバイスを使う
            currentDevice = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        }

        return currentDevice
    }

    private fun stopAudioCapture() {
        // 読み取りスレッドの終了を待ってからAudioRecordを解放する
        isMonitoring = false
        captureThread?.join(1000)
        captureThread = null
        audioRecord?.let {
            it.stop()
            it.release()
        }
        audioRecord = null
        // 自動で下げた分の音量を元に戻す
        repeat(adjustTicket) {
            audioManager?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE, 0
            )
        }
        adjustTicket = 0
        mediaProjection?.stop()
        mediaProjection = null
    }

    private fun calculateRMS(buffer: ShortArray, readSize: Int): Double {
        if (readSize <= 0) return 0.0

        var sum = 0.0
        for (i in 0 until readSize) {
            sum += buffer[i].toDouble() * buffer[i]
        }
        return sqrt(sum / readSize)
    }

    companion object {
        var isMonitoring: Boolean = false
        var thresholdDb: Int = 50
        private const val TAG = "AudioMonitorService"
    }
}



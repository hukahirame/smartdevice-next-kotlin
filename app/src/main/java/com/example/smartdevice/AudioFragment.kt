package com.example.smartdevice

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.smartdevice.databinding.FragmentAudioBinding

class AudioFragment : Fragment() {
    private var _binding: FragmentAudioBinding? = null
    private val binding get() = _binding!!
    private var audioPermissionLauncher: ActivityResultLauncher<Intent>? = null
    private var mediaProjectionManager: MediaProjectionManager? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAudioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // MediaProjectionManagerの初期化
        mediaProjectionManager =
            requireActivity().getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // 閾値スライダーのリスナー
        binding.slider.addOnChangeListener{ _, value, _ ->
            AudioMonitorService.thresholdDb = value.toInt()
        }

        // 開始ボタンのリスナー
        binding.AMSStartButton.setOnClickListener { startAudioMonitorService() }

        // 停止ボタンのリスナー
        binding.AMSStopButton.setOnClickListener {
            requireActivity().stopService(
                Intent(
                    requireActivity(),
                    AudioMonitorService::class.java
                )
            )
            binding.AMSStartButton.isEnabled = true
        }

        audioPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                // サービスに必要なデータを渡して開始
                val serviceIntent = Intent(
                    requireActivity(),
                    AudioMonitorService::class.java
                )
                serviceIntent.putExtra("resultCode", result.resultCode)
                serviceIntent.putExtra("data", result.data)

                ContextCompat.startForegroundService(requireActivity(), serviceIntent)
                binding.AMSStartButton.isEnabled = false
            } else {
                Log.e(
                    "AudioFragment",
                    "スクリーンキャプチャの権限が許可されませんでした。"
                )
            }
        }

        //UI整合性確保＆ロード処理
        if (AudioMonitorService.isMonitoring) binding.AMSStartButton.isEnabled = false
    }

    // AudioMonitorサービスの開始
    private fun startAudioMonitorService() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.RECORD_AUDIO),
            0
        )
        val captureIntent = mediaProjectionManager!!.createScreenCaptureIntent()
        audioPermissionLauncher!!.launch(captureIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
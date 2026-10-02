package com.example.smartdevice

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.smartdevice.databinding.FragmentAudioBinding

class AudioFragment : Fragment() {
    private var _binding: FragmentAudioBinding? = null
    private val binding get() = _binding!!
    private var audioPermissionLauncher: ActivityResultLauncher<Intent>? = null
    private var micPermissionLauncher: ActivityResultLauncher<String>? = null
    private var overlayPermissionLauncher: ActivityResultLauncher<Intent>? = null
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var overlayPermissionAsked = false

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
        binding.AMSStartButton.setOnClickListener {
            overlayPermissionAsked = false
            startAudioMonitorService()
        }

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

        // マイク権限のランチャー（許可されたら開始処理を続行）
        micPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startAudioMonitorService()
            } else {
                Log.e("AudioFragment", "マイクの権限が許可されませんでした。")
            }
        }

        // オーバーレイ権限のランチャー（拒否されても音量表示なしで続行）
        overlayPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (!Settings.canDrawOverlays(requireContext())) {
                Log.e("AudioFragment", "オーバーレイ権限が許可されませんでした。音量表示なしで起動します。")
            }
            startAudioMonitorService()
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
    }

    override fun onResume() {
        super.onResume()
        //UI整合性確保
        binding.AMSStartButton.isEnabled = !AudioMonitorService.isMonitoring
    }

    // AudioMonitorサービスの開始（マイク権限 → オーバーレイ権限 → キャプチャ許可の順に確認）
    private fun startAudioMonitorService() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            micPermissionLauncher!!.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (!Settings.canDrawOverlays(requireContext()) && !overlayPermissionAsked) {
            overlayPermissionAsked = true
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + requireContext().packageName)
            )
            overlayPermissionLauncher!!.launch(intent)
            return
        }
        val captureIntent = mediaProjectionManager!!.createScreenCaptureIntent()
        audioPermissionLauncher!!.launch(captureIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

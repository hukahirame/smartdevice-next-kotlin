package com.example.smartdevice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.smartdevice.databinding.FragmentOverlayBinding

class OverlayFragment : Fragment() {
    private var overlayPermissionLauncher: ActivityResultLauncher<Intent>? = null
    private var overlayAlpha = 0.6f // 初期アルファ値
    private var _binding: FragmentOverlayBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOverlayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pref = requireActivity().getSharedPreferences("SaveData", Context.MODE_PRIVATE)
        val editor = pref.edit()
        /*
        MobileAds.initialize(requireActivity(),initializationStatus -> {});
        // 広告リクエストを送信して広告を読み込む
        AdRequest adRequest = new AdRequest.Builder().build();
        AdView adView = getView().findViewById(R.id.adView);
        if(adView != null) {
            adView.loadAd(adRequest);
        }
        */
        // オーバーレイ権限のランチャーを初期化
        overlayPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (Settings.canDrawOverlays(requireContext())) {
                // 権限が許可された場合、オーバーレイサービスを開始
                startOverlayService()
            } else {
                Log.e("OverlayFragment", "オーバーレイ権限が許可されませんでした。")
                binding.startButton.isEnabled = false
                binding.slider.isEnabled = false
            }
        }

        /*
// オーバーレイ権限の確認とリクエスト
if (!Settings.canDrawOverlays(requireActivity())) {
    requestOverlayPermission();
}*/

        // スライダーのリスナー設定
        binding.slider.addOnChangeListener { _, value: Float, _ ->
            overlayAlpha = value * 10 / 7f / 100f // v% → v'% → 0~1
            sendAlphaToService(overlayAlpha)
            editor.putInt("overlayValue", value.toInt())
        }
        //トグルボタンのリスナー
        binding.toggleButton.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val mode = if (checkedId == R.id.buttonNormal) "normal" else "orange"
                    sendNightUIModeToService(mode)
                    editor.putString("nightUIStyle", mode).apply()
            }
        }
        //開始ボタンのリスナー
        binding.startButton.setOnClickListener {
            if (!Settings.canDrawOverlays(requireContext())) {
                requestOverlayPermission()
            } else {
                startOverlayService()
            }
        }

        // オーバーレイ停止ボタンのリスナー
        binding.stopButton.setOnClickListener {
            requireContext().stopService(
                Intent(
                    requireContext(),
                    OverlayService::class.java
                )
            )
            binding.startButton.isEnabled = true
        }

        //SaveDataロード処理
        binding.slider.value = pref.getInt("overlayValue", 50).toFloat()
        if (OverlayService.isOverlayActive) binding.startButton.isEnabled = false

        val mode = pref.getString("nightUIStyle", "normal")
        if (mode == "orange") binding.toggleButton.check(R.id.buttonOrange)
        else binding.toggleButton.check(R.id.buttonNormal)
    }

    private fun sendAlphaToService(alpha: Float) {
        if (!OverlayService.isOverlayActive) return // 未起動時はサービスを起動しない
        val intent = Intent(requireContext(), OverlayService::class.java)
        intent.putExtra("alpha", alpha)
        requireContext().startService(intent)
    }

    private fun sendNightUIModeToService(mode: String) {
        if (!OverlayService.isOverlayActive) return // 未起動時はサービスを起動しない
        val intent = Intent(requireContext(), OverlayService::class.java)
        intent.putExtra("nightUI", mode)
        requireContext().startService(intent)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + requireContext().packageName)
        )
        overlayPermissionLauncher!!.launch(intent)
    }

    private fun startOverlayService() { // オーバーレイサービスの開始（ラウンチャーから呼出）
        val intent = Intent(
            requireActivity(),
            OverlayService::class.java
        )
        intent.putExtra("alpha", overlayAlpha)
        ContextCompat.startForegroundService(requireActivity(), intent)
        binding.startButton.isEnabled = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
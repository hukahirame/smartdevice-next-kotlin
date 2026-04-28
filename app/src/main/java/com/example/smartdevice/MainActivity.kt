package com.example.smartdevice

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)

        // FragmentManager 経由で NavHostFragment を取得
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
        if (navHostFragment == null) {
            Log.e("MainActivity", "NavHostFragment が見つかりません。レイアウトを確認してください。")
            return
        }
        val navController = navHostFragment.navController
        setupWithNavController(bottomNavigationView, navController)
    }
}

package com.example.imilipocket.ui.passcode

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.imilipocket.R
import com.example.imilipocket.ui.dashboard.DashboardActivity
import com.example.imilipocket.ui.onboarding.OnboardingActivity

class HomePage : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)
        //ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
        //   val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        // v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
        //insets
        val logoImage: Button = findViewById(R.id.btnhome)
        logoImage.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
        }
    }
}
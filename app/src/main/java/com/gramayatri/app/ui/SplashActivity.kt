package com.gramayatri.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.gramayatri.app.R
import com.gramayatri.app.util.UserPrefs

/**
 * Splash / onboarding screen.
 * First launch: asks for the user's name.
 * Subsequent launches: goes straight to MainActivity.
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var userPrefs: UserPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        userPrefs = UserPrefs(this)

        if (userPrefs.isSetupComplete()) {
            goToMain()
            return
        }

        // Show name-entry UI
        val setupLayout = findViewById<LinearLayout>(R.id.layout_setup)
        setupLayout.visibility = View.VISIBLE

        val etName  = findViewById<EditText>(R.id.et_name)
        val btnSave = findViewById<Button>(R.id.btn_save_name)

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                etName.error = "Please enter your name"
                return@setOnClickListener
            }
            userPrefs.setDisplayName(name)
            userPrefs.markSetupComplete()
            goToMain()
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

package com.app.android.kotlin.facecaptcha

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import com.app.android.kotlin.facecaptcha.challenge.ChallengeActivity


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val captureButton = findViewById<View>(R.id.button_start) as Button

        captureButton.setOnClickListener({ startChallenge(this@MainActivity) })
    }

    private fun startChallenge(context: Context) {
        val intent = Intent(context, ChallengeActivity::class.java)
        context.startActivity(intent)
    }
}

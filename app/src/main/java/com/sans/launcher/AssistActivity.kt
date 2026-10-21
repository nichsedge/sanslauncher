package com.sans.launcher

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class AssistActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private val launchRunnable = Runnable {
        CircleToSearchHelper.launch(this)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val delay = intent.getLongExtra(EXTRA_DELAY_MS, 0L)
        if (delay > 0) {
            handler.postDelayed(launchRunnable, delay)
        } else {
            CircleToSearchHelper.launch(this)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(launchRunnable)
    }

    companion object {
        const val EXTRA_DELAY_MS = "delay_ms"
    }
}

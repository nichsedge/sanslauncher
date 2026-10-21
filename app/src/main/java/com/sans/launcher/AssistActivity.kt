package com.sans.launcher

import android.app.Activity
import android.os.Bundle
import android.util.Log

class AssistActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("SansCircleToSearch", "AssistActivity onCreate triggered")
        CircleToSearchHelper.launch(this)
        finish()
    }
}

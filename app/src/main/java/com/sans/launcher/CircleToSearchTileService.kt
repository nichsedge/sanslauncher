package com.sans.launcher

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService

class CircleToSearchTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, AssistActivity::class.java).apply {
            putExtra(AssistActivity.EXTRA_DELAY_MS, 400L)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            NotificationShadeHelper.collapseNotificationShade(this)
            startActivity(intent)
        }
    }
}

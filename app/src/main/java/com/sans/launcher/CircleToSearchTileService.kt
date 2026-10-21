package com.sans.launcher

import android.service.quicksettings.TileService

class CircleToSearchTileService : TileService() {
    override fun onClick() {
        super.onClick()
        NotificationShadeHelper.collapseNotificationShade(this)
        CircleToSearchHelper.launch(this)
    }
}

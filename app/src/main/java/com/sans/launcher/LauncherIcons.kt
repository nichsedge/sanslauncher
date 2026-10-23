package com.sans.launcher

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource

object LauncherIcons {
    lateinit var AttachMoney: ImageVector
        private set

    lateinit var CalendarToday: ImageVector
        private set

    lateinit var Visibility: ImageVector
        private set

    lateinit var VisibilityOff: ImageVector
        private set

    lateinit var StarBorder: ImageVector
        private set

    lateinit var Work: ImageVector
        private set

    lateinit var Search: ImageVector
        private set

    fun initialize(context: Context) {
        val res = context.resources
        val theme = context.theme
        AttachMoney = ImageVector.vectorResource(theme, res, R.drawable.ic_attach_money)
        CalendarToday = ImageVector.vectorResource(theme, res, R.drawable.ic_calendar_today)
        Visibility = ImageVector.vectorResource(theme, res, R.drawable.ic_visibility)
        VisibilityOff = ImageVector.vectorResource(theme, res, R.drawable.ic_visibility_off)
        StarBorder = ImageVector.vectorResource(theme, res, R.drawable.ic_star_border)
        Work = ImageVector.vectorResource(theme, res, R.drawable.ic_work)
        Search = ImageVector.vectorResource(theme, res, R.drawable.ic_search)
    }
}

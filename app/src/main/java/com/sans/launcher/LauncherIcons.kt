package com.sans.launcher

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource

object LauncherIcons {
    val AttachMoney: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.ic_attach_money)

    val CalendarToday: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.ic_calendar_today)

    val Visibility: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.ic_visibility)

    val VisibilityOff: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.ic_visibility_off)

    val StarBorder: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.ic_star_border)

    val Work: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.ic_work)
}

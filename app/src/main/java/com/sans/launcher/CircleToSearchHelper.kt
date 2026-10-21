package com.sans.launcher

import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.SystemClock
import android.util.Log

object CircleToSearchHelper {
    private const val TAG = "SansCircleToSearch"
    private const val BINDER_DESCRIPTOR = "com.android.internal.app.IVoiceInteractionManagerService"
    private const val TRANSACTION_SHOW_SESSION_FROM_SESSION = 3
    private const val INVOCATION_TIME_MS_KEY = "invocation_time_ms"
    private const val CS_KEY = "omni.entry_point"
    private const val OMNIENT_ENTRYPOINT_LONG_PRESS_NAV_HANDLE = 1

    // VoiceInteractionSession flags:
    // SHOW_WITH_ASSIST (1) | SHOW_WITH_SCREENSHOT (2) | SHOW_SOURCE_ASSIST_GESTURE (4)
    private const val FLAGS = 7

    fun launch(context: Context): Boolean {
        Log.i(TAG, "launch called")
        return try {
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getDeclaredMethod("getService", String::class.java)
            val service = getServiceMethod.invoke(null, "voiceinteraction") as? IBinder
                ?: run {
                    Log.e(TAG, "voiceinteraction service not found")
                    return false
                }

            val sessionArgs = Bundle().apply {
                putLong(INVOCATION_TIME_MS_KEY, SystemClock.elapsedRealtime())
                putInt(CS_KEY, OMNIENT_ENTRYPOINT_LONG_PRESS_NAV_HANDLE)
            }

            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                data.writeInterfaceToken(BINDER_DESCRIPTOR)
                data.writeStrongBinder(null)
                data.writeInt(1)
                sessionArgs.writeToParcel(data, 0)
                data.writeInt(FLAGS)
                data.writeString(context.attributionTag)

                val status = service.transact(TRANSACTION_SHOW_SESSION_FROM_SESSION, data, reply, 0)
                reply.readException()
                val ret = reply.readInt()
                Log.i(TAG, "transact returned status=$status ret=$ret")
                status && ret != 0
            } finally {
                data.recycle()
                reply.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Circle to Search", e)
            false
        }
    }
}

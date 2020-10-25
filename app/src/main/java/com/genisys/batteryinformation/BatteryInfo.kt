package com.genisys.batteryinformation

import android.annotation.SuppressLint
import android.content.Context

object BatteryInfo {
    /**
     * 获取电池容量 mAh
     *
     * 源头文件:frameworks/base/core/res\res/xml/power_profile.xml
     *
     * Java 反射文件：frameworks\base\core\java\com\android\internal\os\PowerProfile.java
     */
    @SuppressLint("PrivateApi")
    fun getBatteryCapacity(context: Context?): String {
        val mPowerProfile: Any
        var batteryCapacity = 0.0
        val POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile"
        try {
            mPowerProfile = Class.forName(POWER_PROFILE_CLASS)
                    .getConstructor(Context::class.java)
                    .newInstance(context)
            batteryCapacity = Class
                    .forName(POWER_PROFILE_CLASS)
                    .getMethod("getBatteryCapacity")
                    .invoke(mPowerProfile) as Double
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return batteryCapacity.toString()
    }
}
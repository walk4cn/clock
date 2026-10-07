package com.example.floatclock

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ClockFormat {

    fun pattern(format24: Boolean, showTenths: Boolean): String {
        val base = if (format24) "HH:mm:ss" else "hh:mm:ss"
        return if (showTenths) "$base.S" else base
    }

    private var cachedPattern: String? = null
    private var cachedFormat: SimpleDateFormat? = null

    /** 仅在主线程调用，缓存 SimpleDateFormat 避免重复创建。 */
    fun format(format24: Boolean, showTenths: Boolean, timeMs: Long): String {
        val p = pattern(format24, showTenths)
        if (p != cachedPattern) {
            cachedFormat = SimpleDateFormat(p, Locale.US)
            cachedPattern = p
        }
        return cachedFormat!!.format(Date(timeMs))
    }
}

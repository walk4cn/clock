package com.example.floatclock

import android.content.Context

object Prefs {
    private const val NAME = "float_clock"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun textSize(ctx: Context): Int = sp(ctx).getInt("text_size", 30)
    fun setTextSize(ctx: Context, v: Int) = sp(ctx).edit().putInt("text_size", v).apply()

    fun textColor(ctx: Context): Int = sp(ctx).getInt("text_color", 0xFFFFFFFF.toInt())
    fun setTextColor(ctx: Context, v: Int) = sp(ctx).edit().putInt("text_color", v).apply()

    fun bgAlpha(ctx: Context): Int = sp(ctx).getInt("bg_alpha", 80)
    fun setBgAlpha(ctx: Context, v: Int) = sp(ctx).edit().putInt("bg_alpha", v).apply()

    fun use24h(ctx: Context): Boolean = sp(ctx).getBoolean("use_24h", true)
    fun setUse24h(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("use_24h", v).apply()

    fun showTenths(ctx: Context): Boolean = sp(ctx).getBoolean("show_tenths", true)
    fun setShowTenths(ctx: Context, v: Boolean) =
        sp(ctx).edit().putBoolean("show_tenths", v).apply()

    fun frameSync(ctx: Context): Boolean = sp(ctx).getBoolean("frame_sync", true)
    fun setFrameSync(ctx: Context, v: Boolean) =
        sp(ctx).edit().putBoolean("frame_sync", v).apply()

    fun posX(ctx: Context): Int = sp(ctx).getInt("pos_x", 40)
    fun posY(ctx: Context): Int = sp(ctx).getInt("pos_y", 100)
    fun setPos(ctx: Context, x: Int, y: Int) =
        sp(ctx).edit().putInt("pos_x", x).putInt("pos_y", y).apply()
}

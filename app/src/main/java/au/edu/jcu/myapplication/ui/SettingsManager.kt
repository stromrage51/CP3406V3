package au.edu.jcu.myapplication.ui

import android.content.Context

object SettingsManager {
    private const val PREFS = "AppSettings"
    private const val K_FONT_SIZE = "fontSize"
    private const val K_BG = "bgColor"
    private const val K_FONT = "fontColor"

    fun fontSize(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(K_FONT_SIZE, 16)

    fun bgColorName(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(K_BG, "White") ?: "White"

    fun fontColorName(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(K_FONT, "Black") ?: "Black"
}
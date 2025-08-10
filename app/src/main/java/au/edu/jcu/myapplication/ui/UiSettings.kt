package au.edu.jcu.myapplication.ui

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment


fun Activity.applyAppSettings(root: View) {
    val size = SettingsManager.fontSize(this)
    val bg = SettingsManager.bgColorName(this)
    val fg = SettingsManager.fontColorName(this)
    root.setBackgroundColor(AppColors.map[bg] ?: Color.WHITE)
    applyToAllText(root, size, fg)
}

fun Fragment.applyAppSettings(root: View) {
    val ctx = requireContext()
    val size = SettingsManager.fontSize(ctx)
    val bg = SettingsManager.bgColorName(ctx)
    val fg = SettingsManager.fontColorName(ctx)
    root.setBackgroundColor(AppColors.map[bg] ?: Color.WHITE)
    applyToAllText(root, size, fg)
}

private fun applyToAllText(view: View, size: Int, colorName: String) {
    val color = AppColors.map[colorName] ?: Color.BLACK
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) applyToAllText(view.getChildAt(i), size, colorName)
    } else if (view is TextView) {
        view.textSize = size.toFloat()
        view.setTextColor(color)
    }
}

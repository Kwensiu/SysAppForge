package com.example.sysappmodule.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable

/**
 * 把 Drawable 渲染为 Bitmap。
 * 不依赖 androidx.core.graphics.drawToBitmap（某些 core-ktx 版本可能未导出）。
 */
fun Drawable.toBitmap(): Bitmap {
    val width = intrinsicWidth.coerceAtLeast(1)
    val height = intrinsicHeight.coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bmp
}

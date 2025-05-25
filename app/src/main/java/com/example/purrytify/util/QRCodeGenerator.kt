package com.example.purrytify.util

import android.graphics.Bitmap
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

object QRCodeGenerator {
    fun generateQRCode(content: String): Bitmap? {
        return try {
            Log.d("QRCodeGenerator", "Starting QR code generation for string content: $content")
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            Log.d("QRCodeGenerator", "QR code generation (in Bitmap) successful")
            bitmap
        } catch (e: WriterException) {
            Log.e("QRCodeGenerator", "Error generating QR code", e)
            null
        }
    }
}
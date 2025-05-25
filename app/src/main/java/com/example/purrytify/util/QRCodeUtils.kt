package com.example.purrytify.util

import android.graphics.Bitmap
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import com.example.purrytify.R
import android.graphics.Canvas
import android.graphics.Typeface

object QRCodeUtils {
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

    fun showQrPreviewDialog(context: Context, qrBitmap: Bitmap, title: String, artist: String) {
        try {
            Log.d("QRCodeUtils", "Preparing to show QR preview dialog")
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_qr_preview, null)
            val qrImageView = dialogView.findViewById<ImageView>(R.id.qr_image)
            val titleTextView = dialogView.findViewById<TextView>(R.id.qr_title)
            val artistTextView = dialogView.findViewById<TextView>(R.id.qr_artist)

            qrImageView.setImageBitmap(qrBitmap)
            titleTextView.text = title
            artistTextView.text = artist

            Log.d("QRCodeUtils", "Setting up AlertDialog for QR preview")
            AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton("Share") { _, _ ->
                    Log.d("QRCodeUtils", "Share button clicked, sharing QR image")
                    this.shareQrImage(context, qrBitmap, title, artist)
                }
                .setNegativeButton("Close") { _, _ ->
                    Log.d("QRCodeUtils", "Close button clicked, dismissing dialog")
                }
                .setIcon(R.drawable.ic_launcher_foreground)
                .show()
            Log.d("QRCodeUtils", "QR preview dialog displayed successfully")
        } catch (e: Exception) {
            Log.e("QRCodeUtils", "Error showing QR preview dialog", e)
        }
    }

    fun shareQrImage(context: Context, qrBitmap: Bitmap, title: String, artist: String) {
        try {
            Log.d("QRCodeUtils", "Starting to share QR image with additional text")

            // Create a new bitmap with extra space for text
            val width = qrBitmap.width
            val height = qrBitmap.height + 200 // Add extra height for text
            val combinedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            // Draw the QR code and text on the new bitmap
            val canvas = Canvas(combinedBitmap)
            canvas.drawColor(Color.WHITE) // Set background color
            canvas.drawBitmap(qrBitmap, 0f, 0f, null)

            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 40f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            val paintArtist = Paint().apply {
                color = Color.GRAY
                textSize = 32f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }

            val paintBold = Paint().apply {
                color = Color.parseColor("#4CAF50")
                textSize = 48f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            // Draw title, artist, and additional text
            canvas.drawText(title, (width / 2).toFloat(), (qrBitmap.height + 50).toFloat(), paintBold)
            canvas.drawText(artist, (width / 2).toFloat(), (qrBitmap.height + 100).toFloat(), paintArtist)
            canvas.drawText("Scan this on your Purrytify", (width / 2).toFloat(), (qrBitmap.height + 175).toFloat(), paint)

            // Save the combined bitmap to share
            val path = MediaStore.Images.Media.insertImage(context.contentResolver, combinedBitmap, "QR Code", null)
            val uri = Uri.parse(path)

            Log.d("QRCodeUtils", "QR image with text saved to path: $path")

            // Create and launch the share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
            }
            Log.d("QRCodeUtils", "Launching share intent")
            context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
            Log.d("QRCodeUtils", "Share intent launched successfully")
        } catch (e: Exception) {
            Log.e("QRCodeUtils", "Error sharing QR image with text", e)
        }
    }
}
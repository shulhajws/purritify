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
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import com.example.purrytify.R

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
                    this.shareQrImage(context, qrBitmap)
                }
                .setNegativeButton("Close") { _, _ ->
                    Log.d("QRCodeUtils", "Close button clicked, dismissing dialog")
                }
                .show()
            Log.d("QRCodeUtils", "QR preview dialog displayed successfully")
        } catch (e: Exception) {
            Log.e("QRCodeUtils", "Error showing QR preview dialog", e)
        }
    }

    fun shareQrImage(context: Context, qrBitmap: Bitmap) {
        try {
            Log.d("QRCodeUtils", "Starting to share QR image")
            val path = MediaStore.Images.Media.insertImage(context.contentResolver, qrBitmap, "QR Code", null)
            val uri = Uri.parse(path)

            // Undeprecated way to do so
//            val contentValues = ContentValues().apply {
//                put(MediaStore.Images.Media.DISPLAY_NAME, "QR Code")
//                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
//                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/QR Codes")
//            }
//
//            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
//            uri?.let {
//                context.contentResolver.openOutputStream(it)?.use { outputStream ->
//                    qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
//                }
//            }

            Log.d("QRCodeUtils", "QR image saved to path: $path")

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
            }
            Log.d("QRCodeUtils", "Launching share intent")
            context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
            Log.d("QRCodeUtils", "Share intent launched successfully")
        } catch (e: Exception) {
            Log.e("QRCodeUtils", "Error sharing QR image", e)
        }
    }
}
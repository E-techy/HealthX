package com.example.healthx.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object QRGenerator {

    private const val TAG = "QRGenerator"

    /**
     * Generates a QR Code Bitmap from any String (URL, JSON, or plain text).
     * Runs on the Default dispatcher to prevent UI thread blocking.
     *
     * @param data The string payload (e.g., JSON string of health records).
     * @param size The width and height of the generated Bitmap in pixels.
     * @param foregroundColor The color of the QR code pixels (Default: Black).
     * @param backgroundColor The background color (Default: White - Best for scanning).
     * @return A Bitmap of the QR code, or null if generation fails.
     */
    suspend fun generateQRCode(
        data: String,
        size: Int = 512,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap? = withContext(Dispatchers.Default) {

        if (data.isBlank()) {
            Log.e(TAG, "Cannot generate QR code: Data is empty.")
            return@withContext null
        }

        try {
            val writer = QRCodeWriter()

            // Hints configure the QR code's behavior
            val hints = mapOf(
                // Error correction 'L' (Low) allows for denser data (good for JSON)
                // If you plan to put a logo in the middle later, change this to 'H' (High)
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
                EncodeHintType.MARGIN to 2 // Sets the white border thickness (default is usually 4)
            )

            // Generate the boolean matrix of the QR code
            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            // Map the boolean matrix to the specified colors
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) foregroundColor else backgroundColor
                }
            }

            // Create and return the final Bitmap
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap

        } catch (e: WriterException) {
            Log.e(TAG, "Failed to generate QR code: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error generating QR code: ${e.message}")
            null
        }
    }
}
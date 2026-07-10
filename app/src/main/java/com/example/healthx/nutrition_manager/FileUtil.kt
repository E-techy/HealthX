package com.example.healthx.nutrition_manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object FileUtil {

    // Generates a blank Uri for the Camera to write into
    fun createTempCameraUri(context: Context): Uri {
        val tempFile = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
        // NOTE: Ensure your AndroidManifest.xml has a FileProvider setup matching this authority!
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
    }

    // Converts a Uri into a compressed File for the API
    fun uriToCompressedFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(tempFile)

            // Compress to JPEG at 80% quality to save bandwidth
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.flush()
            outputStream.close()

            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
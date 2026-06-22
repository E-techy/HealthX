package com.example.healthx.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object FileHelper {
    /**
     * Copies the content of a URI to a temporary file in the app's cache directory.
     * This is required because Retrofit needs a real File object for Multipart uploads.
     */
    fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            // Create a temporary file in the cache directory
            val tempFile = File.createTempFile("profile_upload_", ".jpg", context.cacheDir)
            val outputStream = FileOutputStream(tempFile)

            // Copy the bytes from the URI stream to our new file
            inputStream.copyTo(outputStream)

            inputStream.close()
            outputStream.close()
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
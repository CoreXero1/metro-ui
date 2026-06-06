package com.codeancy.metroui.common.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

actual class IntentUtils(
    private val context: Context
) {
    actual fun onShareMetroRoute(imageBitmap: ImageBitmap) {
        // Sharing must NEVER crash the app — wrap the whole flow. The
        // previous crash was an IllegalArgumentException from
        // FileProvider.getUriForFile() when the <provider> manifest entry
        // was missing for the agra flavor. The manifest is fixed now, but
        // we keep this defensive try/catch so a future regression (e.g.
        // missing path in file_paths.xml, full disk, etc.) shows up as a
        // logged failure and a no-op share rather than a fatal exception.
        try {
            val intent = createShareImageIntent(context, imageBitmap) ?: return
            context.startActivity(intent)
        } catch (t: Throwable) {
            Log.e(TAG, "onShareMetroRoute failed", t)
        }
    }

    private fun createShareImageIntent(context: Context, imageBitmap: ImageBitmap): Intent? {
        val bitmap = imageBitmap.asAndroidBitmap()
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "shared_image.png")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private companion object {
        const val TAG = "IntentUtils"
    }
}
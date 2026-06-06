package com.codeancy.metroui.map

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import com.codeancy.metroui.common.utils.MapDrawableResource

@Composable
actual fun MapImage(
    modifier: Modifier,
    mapDrawableResource: MapDrawableResource,
    contentDescription: String?,
    contentScale: ContentScale
) {
    // painterResource() supports only VectorDrawable & BitmapDrawable. If the
    // resource is anything else (e.g. an empty <selector>, which is what
    // crashed v100020 and got the app suspended), it throws
    // IllegalArgumentException. Compose does NOT allow try/catch around
    // composable invocations, so we validate the resource here — in a
    // non-composable `remember { ... }` block — before calling
    // painterResource.
    val context = LocalContext.current
    val canRender = remember(mapDrawableResource.resId) {
        runCatching {
            val d = ContextCompat.getDrawable(context, mapDrawableResource.resId)
            d is VectorDrawable || d is BitmapDrawable
        }.getOrElse { e ->
            Log.e("MapImage", "Drawable id=${mapDrawableResource.resId} could not be inflated", e)
            false
        }
    }

    if (canRender) {
        Image(
            painter = painterResource(mapDrawableResource.resId),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        // Render an empty box rather than crashing. The actual map.xml is now
        // a valid Vector Drawable, so this branch should never run in the
        // current codebase — it's a safety net against future regressions.
        Log.w("MapImage", "Skipping painterResource — drawable id=${mapDrawableResource.resId} is not VectorDrawable or BitmapDrawable")
        Box(modifier = modifier)
    }
}

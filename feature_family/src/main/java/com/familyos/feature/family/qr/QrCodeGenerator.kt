package com.familyos.feature.family.qr

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Generates a QR [Bitmap] for the given [payload].
 */
object QrCodeGenerator {

    /**
     * Encodes [payload] as a QR code bitmap.
     *
     * @param payload textual payload (invite code or deep link)
     * @param sizePx width/height in pixels
     */
    fun generate(payload: String, sizePx: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
        )
        val matrix: BitMatrix = MultiFormatWriter().encode(
            payload,
            BarcodeFormat.QR_CODE,
            sizePx,
            sizePx,
            hints,
        )
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE
            }
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    /** Builds a FamilyOS invite deep-link payload from a raw invite [code]. */
    fun invitePayload(code: String): String = "familyos://join/$code"
}

/**
 * Displays a QR code for [payload].
 */
@Composable
fun QrCodeImage(
    payload: String,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
) {
    val bitmap = remember(payload) {
        QrCodeGenerator.generate(payload, sizePx = 640)
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Invite QR code",
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
    )
}

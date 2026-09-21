package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Image Crop", appName)
  }

  @Test
  fun `cropBitmapInMemory applies filter and produces non-null bitmap`() = runBlocking {
    val testBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
    val canvasSize = Size(400f, 400f)
    val cropRect = Rect(100f, 100f, 300f, 300f)

    // Test with Grayscale filter
    val grayscaleCropped = cropBitmapInMemory(
      sourceBitmap = testBitmap,
      canvasSize = canvasSize,
      cropRect = cropRect,
      baseScale = 1.0f,
      userScale = 1.0f,
      pan = Offset.Zero,
      rotation = 0f,
      filter = ImageFilterType.GRAYSCALE
    )
    assertNotNull("Cropped bitmap with grayscale filter should not be null", grayscaleCropped)
    assertEquals(200, grayscaleCropped!!.width)
    assertEquals(200, grayscaleCropped.height)

    // Test with Sepia filter
    val sepiaCropped = cropBitmapInMemory(
      sourceBitmap = testBitmap,
      canvasSize = canvasSize,
      cropRect = cropRect,
      baseScale = 1.0f,
      userScale = 1.0f,
      pan = Offset.Zero,
      rotation = 0f,
      filter = ImageFilterType.SEPIA
    )
    assertNotNull("Cropped bitmap with sepia filter should not be null", sepiaCropped)

    // Test with High Contrast filter
    val contrastCropped = cropBitmapInMemory(
      sourceBitmap = testBitmap,
      canvasSize = canvasSize,
      cropRect = cropRect,
      baseScale = 1.0f,
      userScale = 1.0f,
      pan = Offset.Zero,
      rotation = 0f,
      filter = ImageFilterType.HIGH_CONTRAST
    )
    assertNotNull("Cropped bitmap with high contrast filter should not be null", contrastCropped)
  }
}

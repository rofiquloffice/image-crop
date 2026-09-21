package com.example

import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testImageFilterTypeDefinitions() {
    // Original filter has null matrix (identity / no-op)
    assertNull(ImageFilterType.ORIGINAL.matrixValues)
    assertNull(ImageFilterType.ORIGINAL.toComposeColorFilter())
    assertNull(ImageFilterType.ORIGINAL.toAndroidColorFilter())

    // Filter list contains required filters
    val filterTypes = ImageFilterType.entries
    assertTrue(filterTypes.contains(ImageFilterType.GRAYSCALE))
    assertTrue(filterTypes.contains(ImageFilterType.SEPIA))
    assertTrue(filterTypes.contains(ImageFilterType.HIGH_CONTRAST))

    // Every non-original filter has exactly 20 matrix values (4x5 ColorMatrix)
    filterTypes.filter { it != ImageFilterType.ORIGINAL }.forEach { filter ->
      assertNotNull("${filter.name} matrix should not be null", filter.matrixValues)
      assertEquals("${filter.name} matrix must contain 20 float elements", 20, filter.matrixValues!!.size)
      assertNotNull("${filter.name} compose color filter should be non-null", filter.toComposeColorFilter())
      assertNotNull("${filter.name} android color filter should be non-null", filter.toAndroidColorFilter())
    }
  }
}

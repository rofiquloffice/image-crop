package com.example

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        ImageCropApp()
      }
    }
  }
}

enum class CropAspectRatio(val label: String, val ratio: Float?) {
  FREE("Free", null),
  SQUARE("1:1", 1f),
  STANDARD("4:3", 4f / 3f),
  WIDESCREEN("16:9", 16f / 9f),
  PORTRAIT("9:16", 9f / 16f)
}

enum class ImageFilterType(val label: String, val matrixValues: FloatArray?) {
  ORIGINAL("Original", null),
  GRAYSCALE(
    "Grayscale",
    floatArrayOf(
      0.299f, 0.587f, 0.114f, 0f, 0f,
      0.299f, 0.587f, 0.114f, 0f, 0f,
      0.299f, 0.587f, 0.114f, 0f, 0f,
      0f,     0f,     0f,     1f, 0f
    )
  ),
  SEPIA(
    "Sepia",
    floatArrayOf(
      0.393f, 0.769f, 0.189f, 0f, 0f,
      0.349f, 0.686f, 0.168f, 0f, 0f,
      0.272f, 0.534f, 0.131f, 0f, 0f,
      0f,     0f,     0f,     1f, 0f
    )
  ),
  HIGH_CONTRAST(
    "High Contrast",
    floatArrayOf(
      1.7f, 0f,   0f,   0f, -75f,
      0f,   1.7f, 0f,   0f, -75f,
      0f,   0f,   1.7f, 0f, -75f,
      0f,   0f,   0f,   1f, 0f
    )
  ),
  WARM(
    "Warm",
    floatArrayOf(
      1.15f, 0f,    0f,    0f, 10f,
      0f,    1.05f, 0f,    0f, 5f,
      0f,    0f,    0.85f, 0f, -15f,
      0f,    0f,    0f,    1f, 0f
    )
  ),
  COOL(
    "Cool",
    floatArrayOf(
      0.9f,  0f,    0f,    0f, -10f,
      0f,    1.0f,  0f,    0f, 0f,
      0f,    0f,    1.2f,  0f, 15f,
      0f,    0f,    0f,    1f, 0f
    )
  );

  fun toComposeColorFilter(): ColorFilter? {
    return matrixValues?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }
  }

  fun toAndroidColorFilter(): ColorMatrixColorFilter? {
    return matrixValues?.let { ColorMatrixColorFilter(it) }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropApp() {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var isLoadingImage by remember { mutableStateOf(false) }
  var isSaving by remember { mutableStateOf(false) }

  // Transform states
  var userScale by remember { mutableFloatStateOf(1.0f) }
  var panOffset by remember { mutableStateOf(Offset.Zero) }
  var rotationDegrees by remember { mutableFloatStateOf(0f) }
  var selectedAspectRatio by remember { mutableStateOf(CropAspectRatio.SQUARE) }
  var selectedFilter by remember { mutableStateOf(ImageFilterType.ORIGINAL) }
  var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

  // Canvas coordinate memory for accurate export calculation
  var lastCanvasSize by remember { mutableStateOf(Size.Zero) }
  var lastCropRect by remember { mutableStateOf(Rect.Zero) }
  var lastBaseScale by remember { mutableFloatStateOf(1.0f) }

  // Animated feedback & pulse triggers
  var isDraggingFrame by remember { mutableStateOf(false) }
  var adjustmentPulseTrigger by remember { mutableFloatStateOf(0f) }

  fun triggerAdjustmentPulse() {
    adjustmentPulseTrigger = (adjustmentPulseTrigger + 1f) % 1000f
  }

  // Photo Picker launcher
  val pickMedia = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      isLoadingImage = true
      coroutineScope.launch {
        val loaded = loadBitmapWithCoil(context, uri)
        if (loaded != null) {
          currentBitmap = loaded
          // Reset transformations for new image
          userScale = 1.0f
          panOffset = Offset.Zero
          rotationDegrees = 0f
          selectedFilter = ImageFilterType.ORIGINAL
        } else {
          Toast.makeText(context, "Failed to load selected image", Toast.LENGTH_SHORT).show()
        }
        isLoadingImage = false
      }
    }
  }

  fun resetTransforms() {
    userScale = 1.0f
    panOffset = Offset.Zero
    rotationDegrees = 0f
    selectedFilter = ImageFilterType.ORIGINAL
    triggerAdjustmentPulse()
  }

  fun rotateClockwise() {
    rotationDegrees = (rotationDegrees + 90f) % 360f
    triggerAdjustmentPulse()
  }

  fun rotateCounterClockwise() {
    rotationDegrees = (rotationDegrees - 90f + 360f) % 360f
    triggerAdjustmentPulse()
  }

  fun exportCroppedImage() {
    val bitmap = currentBitmap ?: return
    if (lastCropRect.width <= 0f || lastCropRect.height <= 0f) return

    isSaving = true
    coroutineScope.launch {
      val cropped = cropBitmapInMemory(
        sourceBitmap = bitmap,
        canvasSize = lastCanvasSize,
        cropRect = lastCropRect,
        baseScale = lastBaseScale,
        userScale = userScale,
        pan = panOffset,
        rotation = rotationDegrees,
        filter = selectedFilter
      )

      if (cropped != null) {
        val saveResult = saveBitmapToGallery(context, cropped)
        if (saveResult.isSuccess) {
          Toast.makeText(
            context,
            "Cropped image saved to Pictures/ImageCropApp",
            Toast.LENGTH_LONG
          ).show()
        } else {
          Toast.makeText(
            context,
            "Failed to save image: ${saveResult.exceptionOrNull()?.message}",
            Toast.LENGTH_LONG
          ).show()
        }
      } else {
        Toast.makeText(context, "Error processing crop transformation", Toast.LENGTH_SHORT).show()
      }
      isSaving = false
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Crop,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "Image Crop",
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp
            )
          }
        },
        actions = {
          IconButton(
            onClick = { showPrivacyPolicyDialog = true },
            modifier = Modifier.testTag("privacy_policy_button")
          ) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = "Privacy Policy"
            )
          }
          if (currentBitmap != null) {
            IconButton(
              onClick = { resetTransforms() },
              modifier = Modifier.testTag("reset_button")
            ) {
              Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = "Reset adjustments"
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Button(
              onClick = { exportCroppedImage() },
              enabled = !isSaving,
              modifier = Modifier
                .padding(end = 8.dp)
                .testTag("save_button"),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
              )
            ) {
              if (isSaving) {
                CircularProgressIndicator(
                  modifier = Modifier.size(18.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.dp
                )
              } else {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                  )
                  Text("Save", fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.background)
    ) {
      if (currentBitmap == null) {
        EmptyStateView(
          isLoading = isLoadingImage,
          onSelectPhoto = {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
          },
          onUseSample = {
            isLoadingImage = true
            coroutineScope.launch {
              currentBitmap = createSamplePhotoBitmap()
              resetTransforms()
              isLoadingImage = false
            }
          }
        )
      } else {
        Column(
          modifier = Modifier.fillMaxSize()
        ) {
          // Aspect ratio selection bar
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            CropAspectRatio.entries.forEach { ratio ->
              val isSelected = selectedAspectRatio == ratio
              val tag = when (ratio) {
                CropAspectRatio.FREE -> "ratio_free_button"
                CropAspectRatio.SQUARE -> "ratio_1_1_button"
                CropAspectRatio.STANDARD -> "ratio_4_3_button"
                CropAspectRatio.WIDESCREEN -> "ratio_16_9_button"
                CropAspectRatio.PORTRAIT -> "ratio_9_16_button"
              }
              FilterChip(
                selected = isSelected,
                onClick = {
                  if (selectedAspectRatio != ratio) {
                    selectedAspectRatio = ratio
                    triggerAdjustmentPulse()
                  }
                },
                label = { Text(ratio.label, fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag(tag)
              )
            }
          }

          // Interactive Canvas Area
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .background(Color(0xFF121212))
          ) {
            CropCanvas(
              bitmap = currentBitmap!!,
              userScale = userScale,
              pan = panOffset,
              rotation = rotationDegrees,
              aspectRatio = selectedAspectRatio.ratio,
              filter = selectedFilter,
              isDraggingExternal = isDraggingFrame,
              adjustmentPulseTrigger = adjustmentPulseTrigger,
              onTransformGesture = { gesturePan, gestureZoom ->
                panOffset += gesturePan
                userScale = (userScale * gestureZoom).coerceIn(0.5f, 5.0f)
              },
              onGestureStateChanged = { isDragging ->
                isDraggingFrame = isDragging
              },
              onAdjustmentCompleted = {
                triggerAdjustmentPulse()
              },
              onCanvasLayoutCalculated = { canvasSize, cropRect, baseScale ->
                lastCanvasSize = canvasSize
                lastCropRect = cropRect
                lastBaseScale = baseScale
              }
            )
          }

          // Control Toolbar & Sliders
          Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Filter selection row
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Filter",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(end = 2.dp)
                )
                ImageFilterType.entries.forEach { filter ->
                  val isSelected = selectedFilter == filter
                  val tag = "filter_${filter.name.lowercase()}_button"
                  FilterChip(
                    selected = isSelected,
                    onClick = {
                      if (selectedFilter != filter) {
                        selectedFilter = filter
                        triggerAdjustmentPulse()
                      }
                    },
                    label = { Text(filter.label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                      selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.testTag(tag)
                  )
                }
              }

              // Zoom slider row
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                IconButton(
                  onClick = {
                    userScale = (userScale - 0.2f).coerceIn(0.5f, 5.0f)
                    triggerAdjustmentPulse()
                  },
                  modifier = Modifier.size(36.dp).testTag("zoom_out_button")
                ) {
                  Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Zoom Out",
                    modifier = Modifier.size(20.dp)
                  )
                }

                Slider(
                  value = userScale,
                  onValueChange = { userScale = it },
                  onValueChangeFinished = { triggerAdjustmentPulse() },
                  valueRange = 0.5f..5.0f,
                  modifier = Modifier
                    .weight(1f)
                    .testTag("zoom_slider")
                )

                IconButton(
                  onClick = {
                    userScale = (userScale + 0.2f).coerceIn(0.5f, 5.0f)
                    triggerAdjustmentPulse()
                  },
                  modifier = Modifier.size(36.dp).testTag("zoom_in_button")
                ) {
                  Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Zoom In",
                    modifier = Modifier.size(20.dp)
                  )
                }

                Text(
                  text = "${(userScale * 100).roundToInt()}%",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.width(48.dp),
                  textAlign = TextAlign.End,
                  color = MaterialTheme.colorScheme.primary
                )
              }

              // Action buttons row (Rotation + Image Selection)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Rotation controls
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  IconButton(
                    onClick = { rotateCounterClockwise() },
                    modifier = Modifier.testTag("rotate_ccw_button")
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                      contentDescription = "Rotate 90° CCW"
                    )
                  }

                  Text(
                    text = "${rotationDegrees.roundToInt()}°",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                  )

                  IconButton(
                    onClick = { rotateClockwise() },
                    modifier = Modifier.testTag("rotate_cw_button")
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.RotateRight,
                      contentDescription = "Rotate 90° CW"
                    )
                  }
                }

                // Change Photo button
                OutlinedButton(
                  onClick = {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                  },
                  modifier = Modifier.testTag("pick_image_button")
                ) {
                  Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Change Photo", fontSize = 13.sp)
                }
              }
            }
          }
        }
      }

      // Loading overlay
      AnimatedVisibility(
        visible = isLoadingImage || isSaving,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
          contentAlignment = Alignment.Center
        ) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              CircularProgressIndicator()
              Text(
                text = if (isSaving) "Saving high-res crop..." else "Loading photo...",
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
      }
    }
  }

  if (showPrivacyPolicyDialog) {
    AlertDialog(
      onDismissRequest = { showPrivacyPolicyDialog = false },
      icon = {
        Icon(
          imageVector = Icons.Default.Info,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary
        )
      },
      title = {
        Text("Privacy Policy", fontWeight = FontWeight.Bold)
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "Zero Personal Data Collection",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp
          )
          Text(
            text = "Image Crop does not collect, store, transmit, or share any personal data, identifiers, or photo contents. All image editing operations run 100% locally and offline on your device.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "Photo Access & Security",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp
          )
          Text(
            text = "The app uses the Android Photo Picker with zero broad storage permissions. Exported images are saved directly to your device's Pictures/ImageCropApp gallery folder.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "Developer Contact: rofiquloffice0.1@gmail.com\nPackage: com.aistudio.imagecrop.vkyqta",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = { showPrivacyPolicyDialog = false },
          modifier = Modifier.testTag("close_privacy_dialog_button")
        ) {
          Text("Close")
        }
      }
    )
  }
}

@Composable
fun EmptyStateView(
  isLoading: Boolean,
  onSelectPhoto: () -> Unit,
  onUseSample: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .size(100.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.Crop,
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.onPrimaryContainer
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = "Image Crop Studio",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Select a photo to crop, rotate, zoom, and export with precision in high resolution.",
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(
      onClick = onSelectPhoto,
      enabled = !isLoading,
      modifier = Modifier
        .fillMaxWidth(0.85f)
        .height(52.dp)
        .testTag("pick_image_button"),
      shape = RoundedCornerShape(14.dp)
    ) {
      Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text("Select Photo from Gallery", fontWeight = FontWeight.SemiBold)
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(
      onClick = onUseSample,
      enabled = !isLoading,
      modifier = Modifier
        .fillMaxWidth(0.85f)
        .height(52.dp)
        .testTag("sample_image_button"),
      shape = RoundedCornerShape(14.dp)
    ) {
      Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text("Try with Sample Image", fontWeight = FontWeight.SemiBold)
    }
  }
}

@Composable
fun CropCanvas(
  bitmap: Bitmap,
  userScale: Float,
  pan: Offset,
  rotation: Float,
  aspectRatio: Float?,
  filter: ImageFilterType = ImageFilterType.ORIGINAL,
  isDraggingExternal: Boolean = false,
  adjustmentPulseTrigger: Float = 0f,
  onTransformGesture: (pan: Offset, zoom: Float) -> Unit,
  onGestureStateChanged: (isDragging: Boolean) -> Unit = {},
  onAdjustmentCompleted: () -> Unit = {},
  onCanvasLayoutCalculated: (canvasSize: Size, cropRect: Rect, baseScale: Float) -> Unit
) {
  val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
  val composeColorFilter = remember(filter) { filter.toComposeColorFilter() }

  // Track active gesture status
  var isInteracting by remember { mutableStateOf(false) }
  val isDragging = isInteracting || isDraggingExternal

  // Subtle pulse animation on frame when adjustment completes
  val pulseAnimatable = remember { Animatable(0f) }
  LaunchedEffect(adjustmentPulseTrigger) {
    if (adjustmentPulseTrigger > 0f) {
      pulseAnimatable.snapTo(1f)
      pulseAnimatable.animateTo(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
      )
    }
  }

  // Handle drag animation: scale, stroke thickness, and glow
  val handleScale by animateFloatAsState(
    targetValue = if (isDragging) 1.28f else 1.0f,
    animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
    label = "handle_scale_animation"
  )

  val handleStrokeWidth by animateFloatAsState(
    targetValue = if (isDragging) 5.5f else 3.5f,
    animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
    label = "handle_stroke_animation"
  )

  // Subtle breathing glow when dragging
  val infiniteTransition = rememberInfiniteTransition(label = "dragging_glow")
  val draggingPulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.65f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(450, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "handle_glow_pulse"
  )

  val primaryAccent = MaterialTheme.colorScheme.primary
  val handleColor by animateColorAsState(
    targetValue = if (isDragging) {
      primaryAccent.copy(alpha = draggingPulseAlpha)
    } else {
      Color.White
    },
    animationSpec = tween(180),
    label = "handle_color_animation"
  )

  // Frame border highlight during drag or pulse
  val pulseVal = pulseAnimatable.value
  val frameBorderColor = remember(isDragging, pulseVal, handleColor) {
    when {
      isDragging -> primaryAccent.copy(alpha = 0.85f)
      pulseVal > 0f -> Color.White.copy(alpha = 0.9f + 0.1f * pulseVal)
      else -> Color.White.copy(alpha = 0.9f)
    }
  }

  val frameBorderWidthDp = remember(isDragging, pulseVal) {
    if (isDragging) 2.5.dp else (2.dp + (1.5.dp * pulseVal))
  }

  androidx.compose.foundation.Canvas(
    modifier = Modifier
      .fillMaxSize()
      .testTag("crop_canvas")
      .pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            isInteracting = true
            onGestureStateChanged(true)

            var zoom = 1f
            var panAcc = Offset.Zero
            var pastTouchSlop = false
            val touchSlop = viewConfiguration.touchSlop

            do {
              val event = awaitPointerEvent()
              val canceled = event.changes.any { it.isConsumed }
              if (!canceled) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                  zoom *= zoomChange
                  panAcc += panChange
                  val centroidSize = event.calculateCentroidSize(useCurrent = false)
                  val zoomMotion = kotlin.math.abs(1 - zoom) * centroidSize
                  val panMotion = panAcc.getDistance()

                  if (zoomMotion > touchSlop || panMotion > touchSlop) {
                    pastTouchSlop = true
                  }
                }

                if (pastTouchSlop) {
                  if (zoomChange != 1f || panChange != Offset.Zero) {
                    onTransformGesture(panChange, zoomChange)
                  }
                  event.changes.forEach {
                    if (it.positionChanged()) {
                      it.consume()
                    }
                  }
                }
              }
            } while (!canceled && event.changes.any { it.pressed })

            isInteracting = false
            onGestureStateChanged(false)
            if (pastTouchSlop) {
              onAdjustmentCompleted()
            }
        }
      }
  ) {
    val canvasW = size.width
    val canvasH = size.height
    if (canvasW <= 0f || canvasH <= 0f) return@Canvas

    val canvasCenter = Offset(canvasW / 2f, canvasH / 2f)

    // Calculate crop rectangle based on aspect ratio
    val maxCropW = canvasW * 0.85f
    val maxCropH = canvasH * 0.85f
    val cropW: Float
    val cropH: Float

    if (aspectRatio != null && aspectRatio > 0f) {
      if (maxCropW / aspectRatio <= maxCropH) {
        cropW = maxCropW
        cropH = maxCropW / aspectRatio
      } else {
        cropH = maxCropH
        cropW = maxCropH * aspectRatio
      }
    } else {
      // Free: default to 1:1 or fitted to image ratio
      val imgRatio = bitmap.width.toFloat() / max(1, bitmap.height).toFloat()
      if (maxCropW / imgRatio <= maxCropH) {
        cropW = maxCropW
        cropH = maxCropW / imgRatio
      } else {
        cropH = maxCropH
        cropW = maxCropH * imgRatio
      }
    }

    val cropLeft = canvasCenter.x - cropW / 2f
    val cropTop = canvasCenter.y - cropH / 2f
    val cropRect = Rect(cropLeft, cropTop, cropLeft + cropW, cropTop + cropH)

    // Calculate base scale to nicely fit crop box initially
    val imgW = bitmap.width.toFloat()
    val imgH = bitmap.height.toFloat()
    val baseScale = max(cropW / imgW, cropH / imgH)

    // Report layout to caller for export calculation
    onCanvasLayoutCalculated(size, cropRect, baseScale)

    // 1. Draw Image with Matrix transformation
    withTransform({
      translate(pan.x, pan.y)
      rotate(rotation, pivot = canvasCenter)
      scale(userScale, userScale, pivot = canvasCenter)
    }) {
      val displayW = imgW * baseScale
      val displayH = imgH * baseScale
      val dstOffset = canvasCenter - Offset(displayW / 2f, displayH / 2f)
      drawImage(
        image = imageBitmap,
        dstOffset = IntOffset(dstOffset.x.roundToInt(), dstOffset.y.roundToInt()),
        dstSize = IntSize(displayW.roundToInt(), displayH.roundToInt()),
        colorFilter = composeColorFilter
      )
    }

    // 2. Draw Dimmed Mask over out-of-bounds areas
    val scrimColor = Color(0xAA000000)
    // Top
    if (cropRect.top > 0) {
      drawRect(
        color = scrimColor,
        topLeft = Offset.Zero,
        size = Size(canvasW, cropRect.top)
      )
    }
    // Bottom
    if (cropRect.bottom < canvasH) {
      drawRect(
        color = scrimColor,
        topLeft = Offset(0f, cropRect.bottom),
        size = Size(canvasW, canvasH - cropRect.bottom)
      )
    }
    // Left
    if (cropRect.left > 0) {
      drawRect(
        color = scrimColor,
        topLeft = Offset(0f, cropRect.top),
        size = Size(cropRect.left, cropRect.height)
      )
    }
    // Right
    if (cropRect.right < canvasW) {
      drawRect(
        color = scrimColor,
        topLeft = Offset(cropRect.right, cropRect.top),
        size = Size(canvasW - cropRect.right, cropRect.height)
      )
    }

    // 2b. Subtle Pulse Ripple Glow Animation around crop rect when adjustment successfully completed
    if (pulseVal > 0f) {
      val pulseExpand = (16.dp.toPx()) * (1f - pulseVal)
      val pulseAlpha = (0.55f * pulseVal).coerceIn(0f, 1f)
      drawRect(
        color = primaryAccent.copy(alpha = pulseAlpha),
        topLeft = Offset(cropRect.left - pulseExpand, cropRect.top - pulseExpand),
        size = Size(cropRect.width + 2 * pulseExpand, cropRect.height + 2 * pulseExpand),
        style = Stroke(width = (2.5.dp.toPx()) * pulseVal)
      )
    }

    // 3. Draw Crop Frame Border
    drawRect(
      color = frameBorderColor,
      topLeft = cropRect.topLeft,
      size = cropRect.size,
      style = Stroke(width = frameBorderWidthDp.toPx())
    )

    // 4. Draw Rule-of-Thirds Grid
    val oneThirdW = cropRect.width / 3f
    val oneThirdH = cropRect.height / 3f
    val gridAlpha = if (isDragging) 0.5f else 0.35f
    val gridColor = Color.White.copy(alpha = gridAlpha)
    val gridStroke = 1.dp.toPx()

    // Vertical grid lines
    drawLine(
      color = gridColor,
      start = Offset(cropRect.left + oneThirdW, cropRect.top),
      end = Offset(cropRect.left + oneThirdW, cropRect.bottom),
      strokeWidth = gridStroke
    )
    drawLine(
      color = gridColor,
      start = Offset(cropRect.left + 2 * oneThirdW, cropRect.top),
      end = Offset(cropRect.left + 2 * oneThirdW, cropRect.bottom),
      strokeWidth = gridStroke
    )

    // Horizontal grid lines
    drawLine(
      color = gridColor,
      start = Offset(cropRect.left, cropRect.top + oneThirdH),
      end = Offset(cropRect.right, cropRect.top + oneThirdH),
      strokeWidth = gridStroke
    )
    drawLine(
      color = gridColor,
      start = Offset(cropRect.left, cropRect.top + 2 * oneThirdH),
      end = Offset(cropRect.right, cropRect.top + 2 * oneThirdH),
      strokeWidth = gridStroke
    )

    // 5. Draw Animated Accent Corner Handles (L-shaped)
    val baseCornerLen = 22.dp.toPx()
    val cornerLen = baseCornerLen * handleScale
    val cornerStroke = handleStrokeWidth.dp.toPx()

    // Glow underlay when dragging for high visibility & tactile feel
    if (isDragging) {
      val glowLen = cornerLen + 3.dp.toPx()
      val glowStroke = cornerStroke + 3.dp.toPx()
      val glowColor = primaryAccent.copy(alpha = 0.45f)

      drawLine(glowColor, Offset(cropRect.left - 1.dp.toPx(), cropRect.top), Offset(cropRect.left + glowLen, cropRect.top), glowStroke)
      drawLine(glowColor, Offset(cropRect.left, cropRect.top - 1.dp.toPx()), Offset(cropRect.left, cropRect.top + glowLen), glowStroke)

      drawLine(glowColor, Offset(cropRect.right + 1.dp.toPx(), cropRect.top), Offset(cropRect.right - glowLen, cropRect.top), glowStroke)
      drawLine(glowColor, Offset(cropRect.right, cropRect.top - 1.dp.toPx()), Offset(cropRect.right, cropRect.top + glowLen), glowStroke)

      drawLine(glowColor, Offset(cropRect.left - 1.dp.toPx(), cropRect.bottom), Offset(cropRect.left + glowLen, cropRect.bottom), glowStroke)
      drawLine(glowColor, Offset(cropRect.left, cropRect.bottom + 1.dp.toPx()), Offset(cropRect.left, cropRect.bottom - glowLen), glowStroke)

      drawLine(glowColor, Offset(cropRect.right + 1.dp.toPx(), cropRect.bottom), Offset(cropRect.right - glowLen, cropRect.bottom), glowStroke)
      drawLine(glowColor, Offset(cropRect.right, cropRect.bottom + 1.dp.toPx()), Offset(cropRect.right, cropRect.bottom - glowLen), glowStroke)
    }

    // Top-Left
    drawLine(handleColor, Offset(cropRect.left - 1.dp.toPx(), cropRect.top), Offset(cropRect.left + cornerLen, cropRect.top), cornerStroke)
    drawLine(handleColor, Offset(cropRect.left, cropRect.top - 1.dp.toPx()), Offset(cropRect.left, cropRect.top + cornerLen), cornerStroke)

    // Top-Right
    drawLine(handleColor, Offset(cropRect.right + 1.dp.toPx(), cropRect.top), Offset(cropRect.right - cornerLen, cropRect.top), cornerStroke)
    drawLine(handleColor, Offset(cropRect.right, cropRect.top - 1.dp.toPx()), Offset(cropRect.right, cropRect.top + cornerLen), cornerStroke)

    // Bottom-Left
    drawLine(handleColor, Offset(cropRect.left - 1.dp.toPx(), cropRect.bottom), Offset(cropRect.left + cornerLen, cropRect.bottom), cornerStroke)
    drawLine(handleColor, Offset(cropRect.left, cropRect.bottom + 1.dp.toPx()), Offset(cropRect.left, cropRect.bottom - cornerLen), cornerStroke)

    // Bottom-Right
    drawLine(handleColor, Offset(cropRect.right + 1.dp.toPx(), cropRect.bottom), Offset(cropRect.right - cornerLen, cropRect.bottom), cornerStroke)
    drawLine(handleColor, Offset(cropRect.right, cropRect.bottom + 1.dp.toPx()), Offset(cropRect.right, cropRect.bottom - cornerLen), cornerStroke)
  }
}

/**
 * Applies the exact transformation matrix to crop the in-memory Bitmap at high resolution.
 */
suspend fun cropBitmapInMemory(
  sourceBitmap: Bitmap,
  canvasSize: Size,
  cropRect: Rect,
  baseScale: Float,
  userScale: Float,
  pan: Offset,
  rotation: Float,
  filter: ImageFilterType = ImageFilterType.ORIGINAL
): Bitmap? = withContext(Dispatchers.Default) {
  try {
    val canvasCenter = Offset(canvasSize.width / 2f, canvasSize.height / 2f)

    // Preserve high resolution while maintaining safe memory limits
    val maxDim = max(sourceBitmap.width, sourceBitmap.height).toFloat()
    val qualityMultiplier = (maxDim / max(cropRect.width, cropRect.height)).coerceIn(1f, 3.5f)

    val targetW = (cropRect.width * qualityMultiplier).roundToInt().coerceAtLeast(1)
    val targetH = (cropRect.height * qualityMultiplier).roundToInt().coerceAtLeast(1)

    val outputBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
    val exportCanvas = android.graphics.Canvas(outputBitmap)
    exportCanvas.drawColor(android.graphics.Color.WHITE)

    // Construct exact transformation matrix
    val matrix = android.graphics.Matrix().apply {
      // 1. Center source image at (0,0)
      postTranslate(-sourceBitmap.width / 2f, -sourceBitmap.height / 2f)
      // 2. Scale by baseScale * userScale
      val totalScale = baseScale * userScale
      postScale(totalScale, totalScale)
      // 3. Rotate around origin (which is the center of the image)
      postRotate(rotation)
      // 4. Translate by canvas center + pan
      postTranslate(canvasCenter.x + pan.x, canvasCenter.y + pan.y)
      // 5. Shift relative to crop window top-left
      postTranslate(-cropRect.left, -cropRect.top)
      // 6. Scale up by quality multiplier for high-res export
      postScale(qualityMultiplier, qualityMultiplier)
    }

    val paint = Paint().apply {
      isAntiAlias = true
      isFilterBitmap = true
      isDither = true
      filter.toAndroidColorFilter()?.let {
        colorFilter = it
      }
    }

    exportCanvas.drawBitmap(sourceBitmap, matrix, paint)
    outputBitmap
  } catch (e: Exception) {
    Log.e("ImageCropApp", "Error cropping bitmap", e)
    null
  }
}

/**
 * Loads a Bitmap from a Content URI using Coil's ImageLoader with software bitmap fallback.
 */
suspend fun loadBitmapWithCoil(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
  try {
    val imageLoader = ImageLoader(context)
    val request = ImageRequest.Builder(context)
      .data(uri)
      .allowHardware(false) // Software bitmap required for Canvas matrix transformations
      .build()

    val result = imageLoader.execute(request)
    if (result is SuccessResult) {
      (result.drawable as? BitmapDrawable)?.bitmap
    } else {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream)
      }
    }
  } catch (e: Exception) {
    Log.e("ImageCropApp", "Coil load failed, using fallback decoder", e)
    try {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream)
      }
    } catch (inner: Exception) {
      null
    }
  }
}

/**
 * Saves a Bitmap to public Pictures/ImageCropApp gallery using MediaStore Scoped Storage.
 */
suspend fun saveBitmapToGallery(
  context: Context,
  bitmap: Bitmap
): Result<Uri> = withContext(Dispatchers.IO) {
  try {
    val fileName = "IMG_CROP_${System.currentTimeMillis()}.jpg"
    val contentValues = ContentValues().apply {
      put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
      put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ImageCropApp")
        put(MediaStore.Images.Media.IS_PENDING, 1)
      }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
      ?: return@withContext Result.failure(Exception("Failed to insert MediaStore entry"))

    resolver.openOutputStream(uri)?.use { outputStream ->
      if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
        throw Exception("Failed to compress bitmap into JPEG")
      }
    } ?: throw Exception("Failed to open output stream")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      contentValues.clear()
      contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
      resolver.update(uri, contentValues, null, null)
    }

    Result.success(uri)
  } catch (e: Exception) {
    Log.e("ImageCropApp", "Failed to save image to gallery", e)
    Result.failure(e)
  }
}

/**
 * Creates a beautiful sample landscape photo with gradients, hills, and photography watermark
 * so the user can test the app immediately even if the emulator gallery has no photos.
 */
fun createSamplePhotoBitmap(): Bitmap {
  val width = 1280
  val height = 960
  val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  val canvas = android.graphics.Canvas(bitmap)

  // Gradient sky
  val skyPaint = Paint().apply {
    shader = LinearGradient(
      0f, 0f, 0f, height * 0.7f,
      intArrayOf(0xFF0F172A.toInt(), 0xFF1E3A8A.toInt(), 0xFF0284C7.toInt(), 0xFF38BDF8.toInt()),
      floatArrayOf(0f, 0.35f, 0.7f, 1f),
      Shader.TileMode.CLAMP
    )
  }
  canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), skyPaint)

  // Golden glowing sun
  val sunPaint = Paint().apply {
    color = 0xFFFDE047.toInt()
    isAntiAlias = true
  }
  canvas.drawCircle(920f, 260f, 90f, sunPaint)

  // Mountain silhouettes
  val mountainPaint1 = Paint().apply {
    color = 0xFF0F766E.toInt()
    isAntiAlias = true
  }
  val path1 = Path().apply {
    moveTo(0f, height.toFloat())
    lineTo(0f, 540f)
    cubicTo(260f, 420f, 540f, 680f, 840f, 480f)
    lineTo(width.toFloat(), 620f)
    lineTo(width.toFloat(), height.toFloat())
    close()
  }
  canvas.drawPath(path1, mountainPaint1)

  val mountainPaint2 = Paint().apply {
    color = 0xFF042F2E.toInt()
    isAntiAlias = true
  }
  val path2 = Path().apply {
    moveTo(0f, height.toFloat())
    lineTo(0f, 700f)
    cubicTo(360f, 580f, 720f, 760f, width.toFloat(), 660f)
    lineTo(width.toFloat(), height.toFloat())
    close()
  }
  canvas.drawPath(path2, mountainPaint2)

  // Photo typography banner
  val textPaint = Paint().apply {
    color = 0xDDFFFFFF.toInt()
    textSize = 42f
    isAntiAlias = true
    isFakeBoldText = true
    textAlign = Paint.Align.CENTER
  }
  canvas.drawText("Photo Studio • 1280 × 960", width / 2f, height - 60f, textPaint)

  return bitmap
}

/**
 * Retained for backward test compatibility with existing Robolectric screenshot tests.
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

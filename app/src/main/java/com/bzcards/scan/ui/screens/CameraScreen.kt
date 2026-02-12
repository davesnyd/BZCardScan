package com.bzcards.scan.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File

// The card guide rectangle dimensions as fractions of the preview
private const val GUIDE_WIDTH_FRACTION = 0.9f
private const val GUIDE_HEIGHT_DP = 220f

@Composable
fun CameraScreen(
    onTextRecognized: (String) -> Unit,
    onNavigateToSavedCards: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to start camera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val previewWidthPx = with(density) { maxWidth.toPx() }
        val previewHeightPx = with(density) { maxHeight.toPx() }
        val guideHeightPx = with(density) { GUIDE_HEIGHT_DP.dp.toPx() }

        // Calculate the guide rectangle position as fractions of the preview
        val guideWidthPx = previewWidthPx * GUIDE_WIDTH_FRACTION
        val guideLeft = (previewWidthPx - guideWidthPx) / 2f
        val guideTop = (previewHeightPx - guideHeightPx) / 2f

        // Fractions for cropping the captured image
        val cropLeftFraction = guideLeft / previewWidthPx
        val cropTopFraction = guideTop / previewHeightPx
        val cropWidthFraction = guideWidthPx / previewWidthPx
        val cropHeightFraction = guideHeightPx / previewHeightPx

        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Card outline guide
        Box(
            modifier = Modifier
                .fillMaxWidth(GUIDE_WIDTH_FRACTION)
                .height(GUIDE_HEIGHT_DP.dp)
                .align(Alignment.Center)
                .border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
        )

        // Instruction text
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Position business card within the frame",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Saved cards button
        IconButton(
            onClick = onNavigateToSavedCards,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                Icons.Default.List,
                contentDescription = "Saved Cards",
                tint = Color.White
            )
        }

        // Capture button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FloatingActionButton(
                onClick = {
                    captureAndRecognize(
                        context, imageCapture, onTextRecognized,
                        cropLeftFraction, cropTopFraction,
                        cropWidthFraction, cropHeightFraction
                    )
                },
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Scan Card",
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap to scan",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun captureAndRecognize(
    context: Context,
    imageCapture: ImageCapture,
    onTextRecognized: (String) -> Unit,
    cropLeftFraction: Float,
    cropTopFraction: Float,
    cropWidthFraction: Float,
    cropHeightFraction: Float
) {
    val photoFile = File(context.cacheDir, "card_capture_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                try {
                    val croppedBitmap = cropImageToGuide(
                        photoFile,
                        cropLeftFraction, cropTopFraction,
                        cropWidthFraction, cropHeightFraction
                    )
                    val image = InputImage.fromBitmap(croppedBitmap, 0)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())
                    recognizer.process(image)
                        .addOnSuccessListener { result ->
                            onTextRecognized(result.text)
                            photoFile.delete()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Text recognition failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            photoFile.delete()
                        }
                } catch (e: Exception) {
                    // If cropping fails, fall back to full image
                    val image = InputImage.fromFilePath(context, android.net.Uri.fromFile(photoFile))
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())
                    recognizer.process(image)
                        .addOnSuccessListener { result ->
                            onTextRecognized(result.text)
                            photoFile.delete()
                        }
                        .addOnFailureListener { ex ->
                            Toast.makeText(context, "Text recognition failed: ${ex.message}", Toast.LENGTH_SHORT).show()
                            photoFile.delete()
                        }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

private fun cropImageToGuide(
    photoFile: File,
    cropLeftFraction: Float,
    cropTopFraction: Float,
    cropWidthFraction: Float,
    cropHeightFraction: Float
): Bitmap {
    // Read the image and handle EXIF rotation
    val originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
    val exif = ExifInterface(photoFile.absolutePath)
    val orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
    )

    val rotationDegrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }

    val bitmap = if (rotationDegrees != 0f) {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees)
        Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
    } else {
        originalBitmap
    }

    val imgWidth = bitmap.width
    val imgHeight = bitmap.height

    // Apply the crop fractions to the image dimensions
    val cropX = (cropLeftFraction * imgWidth).toInt().coerceIn(0, imgWidth - 1)
    val cropY = (cropTopFraction * imgHeight).toInt().coerceIn(0, imgHeight - 1)
    val cropW = (cropWidthFraction * imgWidth).toInt().coerceIn(1, imgWidth - cropX)
    val cropH = (cropHeightFraction * imgHeight).toInt().coerceIn(1, imgHeight - cropY)

    return Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH)
}

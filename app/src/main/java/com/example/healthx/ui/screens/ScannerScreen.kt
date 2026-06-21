package com.example.healthx.ui.screens

import android.Manifest
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onAddButtonClicked: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State management
    var hasCameraPermission by remember { mutableStateOf(false) }
    var scannedText by remember { mutableStateOf("") }
    var isTextExpanded by remember { mutableStateOf(false) }
    var isFlashEnabled by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }

    // Zoom state (Ratio instead of Linear for pinch-to-zoom)
    var zoomRatio by remember { mutableFloatStateOf(1f) }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val image = InputImage.fromFilePath(context, it)
                val scanner = BarcodeScanning.getClient()
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        barcodes.firstOrNull()?.rawValue?.let { text ->
                            scannedText = text
                            Log.d("ScannerScreen", "Gallery QR Scanned: $text")
                        }
                    }
            } catch (e: Exception) {
                Log.e("ScannerScreen", "Error processing gallery image", e)
            }
        }
    }

    // Request permission immediately upon rendering
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Camera Binding Logic
    LaunchedEffect(lensFacing, hasCameraPermission, previewView) {
        if (hasCameraPermission && previewView != null) {
            val cameraProvider = cameraProviderFuture.get()

            // Unbind to prevent conflicts when switching lenses
            cameraProvider.unbindAll()

            // Setup Preview Use Case
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView!!.surfaceProvider)
            }

            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            // Setup Image Analysis Use Case (for scanning)
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            @androidx.camera.core.ExperimentalGetImage
            imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    val scanner = BarcodeScanning.getClient()
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            barcodes.firstOrNull()?.rawValue?.let { text ->
                                scannedText = text
                            }
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }

            try {
                // Bind BOTH preview and analysis
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

                // Reset states when camera binds
                isFlashEnabled = false
                zoomRatio = camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1f

            } catch (e: Exception) {
                Log.e("ScannerScreen", "Use case binding failed", e)
            }
        }
    }

    // Handle Flashlight explicitly when the state changes
    LaunchedEffect(isFlashEnabled) {
        camera?.cameraControl?.enableTorch(isFlashEnabled)
    }

    // Pinch-to-zoom gesture state
    val transformableState = rememberTransformableState { zoomChange, _, _ ->
        camera?.let { cam ->
            val zoomState = cam.cameraInfo.zoomState.value
            if (zoomState != null) {
                val currentZoomRatio = zoomState.zoomRatio
                val minZoom = zoomState.minZoomRatio
                val maxZoom = zoomState.maxZoomRatio

                // Calculate new ratio and clamp to hardware limits
                val newZoom = (currentZoomRatio * zoomChange).coerceIn(minZoom, maxZoom)
                cam.cameraControl.setZoomRatio(newZoom)
                zoomRatio = newZoom // Keep state updated
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Scanner", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            if (hasCameraPermission) {
                // Live Camera View Implementation
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            // Store the view reference so LaunchedEffect can bind to it
                            previewView = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .transformable(state = transformableState) // Apply pinch gestures
                )
            } else {
                // Friendly prompt if permission is denied
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Camera Permission Required", color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Permission")
                    }
                }
            }

            // Top Panel: Scanned Text Result
            if (scannedText.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xCC1E1E1E))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier
                            .weight(1f)
                            .clickable { isTextExpanded = !isTextExpanded }
                        ) {
                            Text(
                                text = "Result (Tap to expand):",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Text(
                                text = scannedText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                maxLines = if (isTextExpanded) Int.MAX_VALUE else 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = {
                                Log.d("ScannerScreen", "Add button clicked with data: $scannedText")
                                onAddButtonClicked(scannedText)
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Data", tint = Color.White)
                        }
                    }

                    AnimatedVisibility(visible = isTextExpanded) {
                        OutlinedTextField(
                            value = scannedText,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                }
            }

            // Bottom Control Toolbar Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color(0xAA000000))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Toggle Flash
                    FilledIconButton(onClick = {
                        if (camera?.cameraInfo?.hasFlashUnit() == true) {
                            isFlashEnabled = !isFlashEnabled
                        }
                    }) {
                        Icon(
                            if (isFlashEnabled) Icons.Default.FlashlightOff else Icons.Default.FlashlightOn,
                            contentDescription = "Flashlight"
                        )
                    }

                    // Browse Image / Gallery
                    FilledIconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                    }

                    // Flip Camera Lens
                    FilledIconButton(onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    }) {
                        Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Camera")
                    }
                }
            }
        }
    }
}
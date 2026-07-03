package com.example.healthx.ui.screens.scanner

import android.Manifest
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    onCloseClicked: () -> Unit // Callback to navigate back to Home
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: QRScannerViewModel = viewModel()

    val scannedItems by viewModel.scannedItems.collectAsState()
    val currentPendingScan by viewModel.currentPendingScan.collectAsState()
    val currentScanType by viewModel.currentScanType.collectAsState()

    // Camera State
    var hasCameraPermission by remember { mutableStateOf(false) }
    var isFlashEnabled by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraInfo by remember { mutableStateOf<CameraInfo?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val image = InputImage.fromFilePath(context, it)
                val scanner = BarcodeScanning.getClient()
                scanner.process(image).addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull()?.rawValue?.let { text ->
                        viewModel.processScannedText(text)
                    }
                }
            } catch (e: Exception) {
                Log.e("ScannerScreen", "Error processing gallery image", e)
            }
        }
    }

    // Check permissions on mount
    LaunchedEffect(Unit) {
        val isGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        hasCameraPermission = isGranted
        if (!isGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { innerPadding ->
        if (!hasCameraPermission) {
            // Permission Denied UI
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.VideocamOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Camera Permission Required", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("We need camera access to scan QR codes securely.", color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onCloseClicked) {
                    Text("Go Back to Home", color = Color.Gray)
                }
            }
            return@Scaffold
        }

        // Main Layout (Camera Permission Granted)
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            // --- TOP BAR (Close, Flashlight, Gallery) ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCloseClicked) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
                Row {
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
                    }
                    IconButton(onClick = {
                        if (cameraInfo?.hasFlashUnit() == true) {
                            isFlashEnabled = !isFlashEnabled
                            cameraControl?.enableTorch(isFlashEnabled)
                        }
                    }) {
                        Icon(
                            if (isFlashEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = "Flashlight",
                            tint = if (isFlashEnabled) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                }
            }

            // --- CAMERA PREVIEW SECTION ---
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    val scanner = BarcodeScanning.getClient()
                                    scanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            barcodes.firstOrNull()?.rawValue?.let { text ->
                                                viewModel.processScannedText(text)
                                            }
                                        }
                                        .addOnCompleteListener { imageProxy.close() }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            try {
                                cameraProvider.unbindAll()
                                val cam = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                                cameraControl = cam.cameraControl
                                cameraInfo = cam.cameraInfo
                            } catch (e: Exception) {
                                Log.e("Scanner", "Camera binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // OVERLAY: Show pending scan action
                if (currentPendingScan != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xDD121212)),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (currentScanType == ScanDataType.URL) "URL Detected" else "Data Detected",
                                    color = Color.Gray, style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = currentPendingScan!!,
                                    color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(
                                onClick = { viewModel.onImportOrAddClicked() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(if (currentScanType == ScanDataType.URL) "Import" else "Add")
                            }
                        }
                    }
                }
            }

            // --- BOTTOM LIST SECTION ---
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 350.dp).padding(16.dp)
            ) {
                Text("Scanned Items (${scannedItems.size})", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(scannedItems) { index, item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${index + 1}.", color = Color.Gray, modifier = Modifier.width(24.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.displayData,
                                        color = Color.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        fontFamily = if (item.type == ScanDataType.JSON) FontFamily.Monospace else FontFamily.Default
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Status Indicator
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (item.status == ItemStatus.LOADING) {
                                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Downloading...", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                                        } else {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (item.status == ItemStatus.IMPORTED) "Imported" else "Added", color = Color.Green, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }

                                IconButton(onClick = { viewModel.deleteItem(item.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                                }
                            }
                        }
                    }
                }

                // --- SAVE BUTTON ---
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.saveAllItems()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Data Saved Successfully!")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = scannedItems.isNotEmpty()
                ) {
                    Text("SAVE ALL")
                }
            }
        }
    }
}
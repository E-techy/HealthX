package com.example.healthx.docs_manager.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthx.docs_manager.ui.components.DocAccessManagerDialog
import com.example.healthx.docs_manager.ui.components.DocumentCard
import com.example.healthx.docs_manager.ui.components.PublicDocFetcherDialog
import com.example.healthx.docs_manager.ui.components.UploadDocDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocsDashboardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: DocsViewModel = viewModel()
    val docsList by viewModel.docsList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()

    var showUploadDialog by remember { mutableStateOf<Uri?>(null) }
    var manageAccessDocId by remember { mutableStateOf<String?>(null) }
    var showPublicFetcher by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) showUploadDialog = uri
    }

    if (errorMsg != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("Information", color = Color.White) },
            text = { Text(errorMsg!!, color = Color.LightGray) },
            confirmButton = { TextButton(onClick = { viewModel.clearError() }) { Text("OK", color = MaterialTheme.colorScheme.primary) } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Docs Manager", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        floatingActionButton = {
            if (viewModel.currentTab == "MY_DOCS") {
                FloatingActionButton(
                    onClick = { filePicker.launch("*/*") },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Upload Doc", tint = Color.White)
                }
            }
        },
        containerColor = Color.Black
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // TABS
            TabRow(
                selectedTabIndex = if (viewModel.currentTab == "MY_DOCS") 0 else 1,
                containerColor = Color(0xFF121212),
                contentColor = Color.White
            ) {
                Tab(
                    selected = viewModel.currentTab == "MY_DOCS",
                    onClick = { viewModel.currentTab = "MY_DOCS"; viewModel.loadDocs(true) },
                    text = { Text("My Documents") }
                )
                Tab(
                    selected = viewModel.currentTab == "SHARED",
                    onClick = { viewModel.currentTab = "SHARED"; viewModel.loadDocs(true) },
                    text = { Text("Shared With Me") }
                )
            }

            // SEARCH BAR
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.searchQuery = it; viewModel.loadDocs(true) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search documents...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )

            val filterCategories = listOf("ALL", "HEALTH", "DIAGNOSTICS", "PRESCRIPTION", "NUTRITION_MONTHLY_REPORT", "OTHER")

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterCategories) { cat ->
                    val isSelected = if (cat == "ALL") viewModel.selectedCategory == null else viewModel.selectedCategory == cat
                    val displayCat = cat.replace("_", " ")

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.selectedCategory = if (cat == "ALL") null else cat
                            viewModel.loadDocs(reset = true)
                        },
                        label = { Text(displayCat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF1E1E1E),
                            labelColor = Color.LightGray,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color(0xFF2C2C2C),
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            // LIST
            if (isLoading && docsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 100.dp, start = 16.dp, end = 16.dp)
                ) {
                    item {
                        OutlinedButton(
                            onClick = { showPublicFetcher = true },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search Public")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open a Public Link")
                        }
                    }

                    items(docsList) { doc ->
                        DocumentCard(
                            doc = doc,
                            isOwner = viewModel.currentTab == "MY_DOCS",
                            onManageAccess = { manageAccessDocId = doc._id },
                            onDelete = { viewModel.deleteDocument(doc._id) },
                            onView = {
                                val isShared = viewModel.currentTab == "SHARED"
                                viewModel.previewDocument(doc._id, doc.documentName, context, isShared)
                            },
                            onDownload = {
                                val isShared = viewModel.currentTab == "SHARED"
                                viewModel.downloadToDevice(doc._id, doc.documentName, context, isShared)
                            }
                        )
                    }

                    if (viewModel.hasNextPage) {
                        item {
                            TextButton(
                                onClick = { viewModel.loadDocs() },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Load More", color = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
            }
        }
    }

    // DIALOGS
    showUploadDialog?.let { uri ->
        UploadDocDialog(
            fileUri = uri,
            onDismiss = { showUploadDialog = null },
            onUpload = { name, cat ->
                viewModel.uploadDocument(uri, name, cat) {
                    showUploadDialog = null
                }
            }
        )
    }

    manageAccessDocId?.let { docId ->
        DocAccessManagerDialog(
            docId = docId,
            viewModel = viewModel,
            onDismiss = { manageAccessDocId = null; viewModel.loadDocs(true) }
        )
    }

    if (showPublicFetcher) {
        PublicDocFetcherDialog(
            viewModel = viewModel,
            onDismiss = { showPublicFetcher = false; viewModel.isPasswordRequired.value = false }
        )
    }
}
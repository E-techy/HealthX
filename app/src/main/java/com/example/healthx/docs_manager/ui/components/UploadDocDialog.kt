package com.example.healthx.docs_manager.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadDocDialog(
    fileUri: Uri,
    onDismiss: () -> Unit,
    onUpload: (name: String, category: String) -> Unit
) {
    var docName by remember { mutableStateOf("New Document") }
    var docCategory by remember { mutableStateOf("HEALTH") }
    val categories = listOf("HEALTH", "DIAGNOSTICS", "NUTRITION_MONTHLY_REPORT", "PRESCRIPTION", "OTHER")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Document") },
        text = {
            Column {
                OutlinedTextField(
                    value = docName,
                    onValueChange = { docName = it },
                    label = { Text("Document Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Simple Category Dropdown (or radio buttons)
                Text("Category:")
                categories.forEach { cat ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(selected = docCategory == cat, onClick = { docCategory = cat })
                        Text(cat)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onUpload(docName, docCategory) }) { Text("Upload") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
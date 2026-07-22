package com.example.healthx.docs_manager.ui.components

import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UploadDocDialog(
    fileUri: Uri,
    onDismiss: () -> Unit,
    onUpload: (name: String, category: String) -> Unit
) {
    val context = LocalContext.current
    var docName by remember { mutableStateOf("") }
    var docExtension by remember { mutableStateOf("") }
    var docCategory by remember { mutableStateOf("HEALTH") }

    val categories = listOf("HEALTH", "DIAGNOSTICS", "PRESCRIPTION", "NUTRITION_MONTHLY_REPORT", "OTHER")

    // Automatically extract the original file name and extension when the dialog opens
    LaunchedEffect(fileUri) {
        var fullName = "New_Document"
        context.contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fullName = cursor.getString(nameIndex)
                }
            }
        }

        val lastDotIndex = fullName.lastIndexOf('.')
        if (lastDotIndex != -1) {
            docName = fullName.substring(0, lastDotIndex)
            docExtension = fullName.substring(lastDotIndex) // Keep the dot (e.g., ".pdf")
        } else {
            docName = fullName
            docExtension = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { Text("Upload Document", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Rename File (Optional)",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = docName,
                    onValueChange = { docName = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    suffix = {
                        Text(text = docExtension, color = Color.Gray)
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Select Category",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Modern FlowRow for Chips instead of Radio Buttons
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = docCategory == cat
                        val displayCat = cat.replace("_", " ")

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.DarkGray,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { docCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayCat,
                                color = if (isSelected) Color.White else Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpload(docName + docExtension, docCategory) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Upload", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}
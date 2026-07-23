package com.example.healthx.docs_manager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthx.docs_manager.data.DocumentDto
import com.example.healthx.docs_manager.ui.DownloadState

@Composable
fun DocumentCard(
    doc: DocumentDto,
    isOwner: Boolean,
    downloadState: DownloadState?,
    onManageAccess: () -> Unit,
    onDelete: () -> Unit,
    onView: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = doc.documentName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                // Status Badges
                Row {
                    if (doc.isPublic) Icon(Icons.Default.Public, contentDescription = "Public", tint = Color(0xFF64B5F6), modifier = Modifier.size(20.dp).padding(end=4.dp))
                    if (doc.isPasswordProtected) Icon(Icons.Default.Lock, contentDescription = "Password", tint = Color(0xFFFFB74D), modifier = Modifier.size(20.dp).padding(end=4.dp))
                    if (doc.sharedCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Group, contentDescription = "Shared", tint = Color(0xFF81C784), modifier = Modifier.size(20.dp))
                            Text(text = "${doc.sharedCount}", color = Color(0xFF81C784), fontSize = 12.sp, modifier = Modifier.padding(start = 2.dp))
                        }
                    }
                }
            }

            Text(text = doc.documentCategory, color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // DYNAMIC UI: Show Buttons OR Progress Bar depending on state
            if (downloadState is DownloadState.Downloading) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Downloading... ${downloadState.progress}%", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onCancelDownload, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color(0xFFE53935))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = downloadState.progress / 100f,
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color(0xFF2C2C2C)
                    )
                }
            } else if (downloadState is DownloadState.Success) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = Color(0xFF81C784), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Saved to Downloads", color = Color(0xFF81C784), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                // NORMAL ACTION BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onView,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = "View", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onDownload,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Save", modifier = Modifier.size(16.dp))
                        }
                    }

                    if (isOwner) {
                        Row {
                            IconButton(onClick = onManageAccess) { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Access", tint = Color(0xFF64B5F6)) }
                            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE53935)) }
                        }
                    }
                }

                // Show inline error if download failed specifically for this doc
                if (downloadState is DownloadState.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(downloadState.message, color = Color(0xFFE53935), fontSize = 12.sp)
                }
            }
        }
    }
}
package com.example.healthx.docs_manager.ui.components

import androidx.compose.foundation.background
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

@Composable
fun DocumentCard(
    doc: DocumentDto,
    isOwner: Boolean,
    onManageAccess: () -> Unit,
    onDelete: () -> Unit,
    onDownload: () -> Unit // NEW CALLBACK
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

            Spacer(modifier = Modifier.height(12.dp))

            // ACTION BUTTONS ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // DOWNLOAD BUTTON (Visible for both owners and shared users)
                Button(
                    onClick = onDownload,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download", fontSize = 12.sp)
                }

                if (isOwner) {
                    Row {
                        TextButton(onClick = onManageAccess) { Text("Manage Access", color = Color(0xFF64B5F6)) }
                        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE53935)) }
                    }
                }
            }
        }
    }
}
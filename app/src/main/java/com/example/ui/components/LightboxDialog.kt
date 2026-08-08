package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.util.DriveImageUtils

@Composable
fun LightboxDialog(
    imageUrl: String?,
    title: String = "Pratinjau Foto",
    onDismiss: () -> Unit
) {
    if (imageUrl == null) return
    val uriHandler = LocalUriHandler.current
    val primaryDirectUrl = DriveImageUtils.convertToDirectUrl(imageUrl)
    val fallbackUrl = DriveImageUtils.getFallbackUrl(imageUrl)
    val driveWebUrl = DriveImageUtils.getDriveWebUrl(imageUrl) ?: imageUrl

    var currentDisplayUrl by remember(imageUrl) { mutableStateOf(primaryDirectUrl) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .padding(16.dp)
        ) {
            // Header Bar inside Lightbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .testTag("close_lightbox_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color.White
                    )
                }
            }

            // Image Display
            SubcomposeAsyncImage(
                model = currentDisplayUrl,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.72f)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit,
                loading = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                },
                error = {
                    if (currentDisplayUrl == primaryDirectUrl && fallbackUrl != null) {
                        LaunchedEffect(Unit) {
                            currentDisplayUrl = fallbackUrl
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Pratinjau foto langsung tidak tersedia.\nGunakan tombol di bawah untuk membuka di Drive.", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            )

            // Google Drive Link Box at Bottom
            Surface(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 8.dp, end = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔗 Link Google Drive / Source:",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = driveWebUrl,
                        color = Color(0xFF81D4FA),
                        fontSize = 11.sp,
                        maxLines = 2,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clickable {
                                try {
                                    uriHandler.openUri(driveWebUrl)
                                } catch (_: Exception) {}
                            }
                    )
                    Button(
                        onClick = {
                            try {
                                uriHandler.openUri(driveWebUrl)
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buka Link Foto di Google Drive", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

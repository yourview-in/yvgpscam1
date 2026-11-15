package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ShutterGold
import com.example.ui.theme.TelemetryEmerald
import com.example.util.AppUpdateInfoState

@Composable
fun AppUpdateDialog(
    updateState: AppUpdateInfoState,
    onUpdateNow: () -> Unit,
    onRestartToInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!updateState.isImmediateRequired && !updateState.isDownloaded) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateState.isImmediateRequired && !updateState.isDownloaded,
            dismissOnClickOutside = !updateState.isImmediateRequired && !updateState.isDownloaded
        ),
        modifier = Modifier.testTag("update_popup_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_brand_yv_logo),
                    contentDescription = "YVGPSCAM Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .border(1.5.dp, ShutterGold, RoundedCornerShape(11.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = if (updateState.isDownloaded) TelemetryEmerald.copy(alpha = 0.20f) else ShutterGold.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (updateState.isDownloaded) "READY TO INSTALL" else "NEW UPDATE AVAILABLE",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (updateState.isDownloaded) TelemetryEmerald else ShutterGold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (updateState.isDownloaded) {
                            "Restart YVGPSCAM"
                        } else {
                            "Update YVGPSCAM"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(
                                text = "Current",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "v${updateState.currentVersionName}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Surface(
                        color = TelemetryEmerald.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, TelemetryEmerald.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(
                                text = "New Version",
                                fontSize = 10.sp,
                                color = TelemetryEmerald
                            )
                            Text(
                                text = updateState.availableVersionLabel,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TelemetryEmerald
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (updateState.isDownloaded) {
                            "The latest version of YVGPSCAM has finished downloading. Tap Restart & Install to finish updating."
                        } else {
                            updateState.releaseNotes
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (updateState.isDownloading) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val pct = updateState.downloadProgressPercent
                        if (pct != null) {
                            LinearProgressIndicator(
                                progress = { (pct / 100f).coerceIn(0f, 1f) },
                                color = ShutterGold,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Downloading update... $pct%",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = ShutterGold
                            )
                        } else {
                            LinearProgressIndicator(
                                color = ShutterGold,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Downloading update...",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = ShutterGold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (updateState.isDownloaded) {
                Button(
                    onClick = onRestartToInstall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TelemetryEmerald,
                        contentColor = ObsidianBg
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("update_popup_restart_button")
                ) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Restart & Install", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    enabled = !updateState.isDownloading,
                    onClick = onUpdateNow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ShutterGold,
                        contentColor = Color(0xFF141619)
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("update_popup_update_button")
                ) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Update Now", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!updateState.isImmediateRequired && !updateState.isDownloaded) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("update_popup_later_button")
                ) {
                    Text("Later")
                }
            }
        }
    )
}

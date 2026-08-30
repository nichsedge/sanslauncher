package com.sans.launcher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ScratchpadScreen(
    text: String,
    onTextChange: (String) -> Unit
) {
    val context = LocalContext.current
    var localText by remember(text) { mutableStateOf(text) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BRAIN DUMP",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "instant capture • auto-saved",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Add Checkbox Bullet
                IconButton(
                    onClick = {
                        val updated = if (localText.isBlank()) "- [ ] " else "$localText\n- [ ] "
                        localText = updated
                        onTextChange(updated)
                    }
                ) {
                    Icon(
                        Icons.Default.CheckBox,
                        contentDescription = "Add Checkbox",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                // Copy All
                IconButton(
                    onClick = {
                        if (localText.isNotBlank()) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = ClipData.newPlainText("Brain Dump", localText)
                            clipboard?.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy notes",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                // Clear
                IconButton(
                    onClick = {
                        localText = ""
                        onTextChange("")
                    }
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear all",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(16.dp))

        // Editor
        BasicTextField(
            value = localText,
            onValueChange = {
                localText = it
                onTextChange(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onBackground
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                if (localText.isEmpty()) {
                    Text(
                        text = "Dump sudden thoughts, tasks, or ideas here before you forget...\n\nTap checkbox icon above to add a todo item.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                    )
                }
                innerTextField()
            }
        )
    }
}

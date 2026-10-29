package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BulkActionsBar(
    selectedCount: Int,
    isVisible: Boolean,
    onClearSelection: () -> Unit,
    onBulkComplete: () -> Unit,
    onBulkDelete: () -> Unit,
    onChangeCriticality: () -> Unit,
    onChangeDeadline: () -> Unit,
    onBulkExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onClearSelection,
                        modifier = Modifier.testTag("bulk_cancel_button")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel multi-select")
                    }
                    Text(
                        text = "$selectedCount selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBulkComplete,
                        modifier = Modifier.testTag("bulk_complete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Complete selected tasks",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onChangeCriticality,
                        modifier = Modifier.testTag("bulk_criticality_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Flag,
                            contentDescription = "Change criticality"
                        )
                    }

                    IconButton(
                        onClick = onChangeDeadline,
                        modifier = Modifier.testTag("bulk_deadline_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = "Change deadline"
                        )
                    }

                    IconButton(
                        onClick = onBulkExport,
                        modifier = Modifier.testTag("bulk_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Export selected"
                        )
                    }

                    IconButton(
                        onClick = onBulkDelete,
                        modifier = Modifier.testTag("bulk_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete selected tasks",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

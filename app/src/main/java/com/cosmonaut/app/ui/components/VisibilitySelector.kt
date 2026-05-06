@file:Suppress("MatchingDeclarationName")

package com.cosmonaut.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.ui.theme.CosmoTheme

private const val BORDER_ALPHA = 0.5f

data class VisibilityOption(val value: String, val label: String, val description: String, val icon: ImageVector,)

private val visibilityOptions = listOf(
    VisibilityOption("private", "Private", "Only you can see this", Icons.Outlined.Lock),
    VisibilityOption("unlisted", "Unlisted", "Anyone with the link", Icons.Outlined.Link),
    VisibilityOption("public", "Public", "Visible to everyone", Icons.Outlined.Public),
)

@Composable
fun VisibilitySelector(
    selectedVisibility: String,
    onVisibilityChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Visibility",
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = visibilityOptions.find { it.value == selectedVisibility } ?: visibilityOptions.first()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = CosmoTheme.colors.foreground,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmoTheme.colors.outline)
                    .border(
                        width = 1.dp,
                        color = CosmoTheme.colors.outline,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable { expanded = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = selected.icon,
                    contentDescription = null,
                    tint = CosmoTheme.colors.foreground,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selected.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmoTheme.colors.foreground,
                    )
                    Text(
                        text = selected.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmoTheme.colors.mutedForeground,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = CosmoTheme.colors.mutedForeground,
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = CosmoTheme.colors.card,
            ) {
                visibilityOptions.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = null,
                                    tint = CosmoTheme.colors.foreground,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = CosmoTheme.colors.foreground,
                                    )
                                    Text(
                                        text = option.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CosmoTheme.colors.mutedForeground,
                                    )
                                }
                            }
                        },
                        onClick = {
                            onVisibilityChange(option.value)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

package com.example.btrapp.habits.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The calm, warm palette of the to-do list, independent of the wallpaper-based app theme. */
@Immutable
internal data class TodoPalette(
    val background: Color,
    val card: Color,
    val segmentTrack: Color,
    val ink: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val chipNeutral: Color,
    val divider: Color,
    val boxOutline: Color,
)

private val LightPalette = TodoPalette(
    background = Color(0xFFF7F5F0),
    card = Color.White,
    segmentTrack = Color(0xFFEDEBE6),
    ink = Color(0xFF26252B),
    muted = Color(0xFF7C7A82),
    accent = Color(0xFF5856AE),
    onAccent = Color.White,
    accentSoft = Color(0xFFE9E8F6),
    chipNeutral = Color(0xFFEDEBE6),
    divider = Color(0xFFEDEBE6),
    boxOutline = Color(0xFFD9D7DE),
)

private val DarkPalette = TodoPalette(
    background = Color(0xFF16151A),
    card = Color(0xFF222128),
    segmentTrack = Color(0xFF2A2930),
    ink = Color(0xFFEDEBF2),
    muted = Color(0xFF9C9AA4),
    accent = Color(0xFFA9A7F0),
    onAccent = Color(0xFF1E1D44),
    accentSoft = Color(0xFF2E2D4D),
    chipNeutral = Color(0xFF34333B),
    divider = Color(0xFF2E2D35),
    boxOutline = Color(0xFF4A4952),
)

@Composable
internal fun todoPalette(): TodoPalette = if (isSystemInDarkTheme()) DarkPalette else LightPalette

internal val CardShape = RoundedCornerShape(20.dp)

/** A rounded-square checkbox: outlined when open, filled with a tick when done. */
@Composable
internal fun TodoCheckBox(checked: Boolean, enabled: Boolean, palette: TodoPalette) {
    val shape = RoundedCornerShape(7.dp)
    Box(
        Modifier
            .size(24.dp)
            .clip(shape)
            .background(if (checked) palette.accent else palette.card)
            .then(if (checked) Modifier else Modifier.border(1.5.dp, palette.boxOutline, shape)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = palette.onAccent.copy(alpha = if (enabled) 1f else 0.6f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** A small tag such as "Habit" or "To-do". */
@Composable
internal fun TodoChip(label: String, highlighted: Boolean, palette: TodoPalette) {
    Text(
        label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = if (highlighted) palette.accent else palette.muted,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(if (highlighted) palette.accentSoft else palette.chipNeutral)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/**
 * One row in a to-do card. Open rows show the title over a tag and a detail line; done rows
 * collapse to a struck-through title.
 */
@Composable
internal fun TodoRow(
    title: String,
    done: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    palette: TodoPalette,
    tag: String,
    tagHighlighted: Boolean,
    detail: String?,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Checkbox) { onToggle(!done) }
            .padding(start = 18.dp, end = 8.dp, top = if (done) 14.dp else 16.dp, bottom = if (done) 14.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TodoCheckBox(checked = done, enabled = enabled, palette = palette)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                title,
                fontSize = 16.sp,
                color = if (done) palette.muted else palette.ink,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!done) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TodoChip(tag, tagHighlighted, palette)
                    detail?.let { Text(it, fontSize = 13.sp, color = palette.muted) }
                }
            }
        }
        trailing()
    }
}

/** A white rounded card whose rows are separated by hairlines. */
@Composable
internal fun <T> TodoCard(palette: TodoPalette, items: List<T>, row: @Composable (T) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(palette.card),
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(color = palette.divider)
            row(item)
        }
    }
}

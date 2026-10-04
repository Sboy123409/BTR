package com.example.btrapp.avatar

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

class AvatarEditorViewModel(private val store: AvatarStore) : ViewModel() {
    /** Edits stay in this draft until [save]; backing out discards them. */
    var draft by mutableStateOf(store.config.value)

    fun save() = store.save(draft)
}

private enum class Category(val label: String) {
    Face("Face"), Hair("Hair"), Eyes("Eyes"), Outfit("Outfit"), Extras("Extras"), Background("Background"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AvatarEditorScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AvatarEditorViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AvatarEditorViewModel(AvatarStore.get(this[APPLICATION_KEY] as Application)) }
        }
    ),
) {
    val draft = viewModel.draft
    var category by rememberSaveable { mutableStateOf(Category.Face) }
    var previewMood by rememberSaveable { mutableStateOf(Mood.Happy) }
    fun update(change: (AvatarConfig) -> AvatarConfig) {
        viewModel.draft = change(viewModel.draft)
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.padding(start = 4.dp, top = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDone) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Your avatar", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Button(onClick = {
                viewModel.save()
                onDone()
            }) {
                Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp))
                Text("Save", Modifier.padding(start = 6.dp))
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Avatar(draft, previewMood, Modifier.size(200.dp).padding(top = 8.dp))
            Row(
                Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Mood.entries.forEach { mood ->
                    FilterChip(
                        selected = previewMood == mood,
                        onClick = { previewMood = mood },
                        label = { Text(mood.name) },
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { name -> update { it.copy(name = name.take(24)) } },
                    label = { Text("Your name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = { update { it.shuffled() } }) {
                    Icon(Icons.Default.Refresh, contentDescription = null, Modifier.size(18.dp))
                    Text("Shuffle", Modifier.padding(start = 6.dp))
                }
            }

            SecondaryScrollableTabRow(selectedTabIndex = category.ordinal, edgePadding = 16.dp) {
                Category.entries.forEach { c ->
                    Tab(selected = category == c, onClick = { category = c }, text = { Text(c.label) })
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (category) {
                    Category.Face -> {
                        OptionTiles(FaceShape.entries, draft.face, { it.label }, { draft.copy(face = it) }) { v ->
                            update { it.copy(face = v) }
                        }
                        Label("Skin tone")
                        Swatches(AvatarPalette.skin, draft.skin) { i -> update { it.copy(skin = i) } }
                    }
                    Category.Hair -> {
                        OptionTiles(HairStyle.entries, draft.hair, { it.label }, { draft.copy(hair = it) }) { v ->
                            update { it.copy(hair = v) }
                        }
                        Label("Colour")
                        Swatches(AvatarPalette.hair, draft.hairColor) { i -> update { it.copy(hairColor = i) } }
                    }
                    Category.Eyes -> {
                        OptionTiles(EyeStyle.entries, draft.eyes, { it.label }, { draft.copy(eyes = it) }) { v ->
                            update { it.copy(eyes = v) }
                        }
                        Label("Brows")
                        OptionTiles(BrowStyle.entries, draft.brows, { it.label }, { draft.copy(brows = it) }) { v ->
                            update { it.copy(brows = v) }
                        }
                    }
                    Category.Outfit -> {
                        OptionTiles(OutfitStyle.entries, draft.outfit, { it.label }, { draft.copy(outfit = it) }) { v ->
                            update { it.copy(outfit = v) }
                        }
                        Label("Colour")
                        Swatches(AvatarPalette.outfit, draft.outfitColor) { i -> update { it.copy(outfitColor = i) } }
                    }
                    Category.Extras ->
                        OptionTiles(Accessory.entries, draft.accessory, { it.label }, { draft.copy(accessory = it) }) { v ->
                            update { it.copy(accessory = v) }
                        }
                    Category.Background ->
                        Swatches(AvatarPalette.background, draft.background) { i -> update { it.copy(background = i) } }
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall)
}

/** One tile per option, each showing the avatar wearing it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> OptionTiles(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    preview: (T) -> AvatarConfig,
    onSelect: (T) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                onClick = { onSelect(option) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            ) {
                Column(
                    Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Avatar(preview(option), Mood.Happy, Modifier.size(64.dp), animated = false)
                    Text(label(option), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Swatches(colors: List<Color>, selected: Int, onSelect: (Int) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        colors.forEachIndexed { index, color ->
            val isSelected = index == selected
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = CircleShape,
                    )
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                    )
                }
            }
        }
    }
}

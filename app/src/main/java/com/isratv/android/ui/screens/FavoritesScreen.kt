package com.isratv.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.isratv.android.model.Channel
import com.isratv.android.viewmodel.ChannelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onChannelClick: (Channel) -> Unit,
    viewModel: ChannelViewModel = hiltViewModel()
) {
    val favoriteEntities by viewModel.favorites.collectAsState(initial = emptyList())
    
    val favoriteChannels = favoriteEntities.map { 
        Channel(
            id = it.id,
            name = it.name,
            logoUrl = it.logoUrl,
            url = it.streamUrl
        )
    }

    Scaffold(
        topBar = {
            // 1. Define the font (same as in the main screen).
            val oswaldFont = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(
                    resId = com.isratv.android.R.font.oswald_bold,
                    weight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            )

            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Favorites",
                        style = MaterialTheme.typography.headlineMedium,
                        // 2. Apply the same styling:
                        fontSize = 32.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = oswaldFont
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary // Optional: match the arrow color to the title
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (favoriteChannels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No favorite channels yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(favoriteChannels) { channel ->
                    ChannelGridItem(
                        channel = channel,
                        isFavorite = true,
                        onFavoriteClick = { viewModel.toggleFavorite(channel) },
                        onClick = { onChannelClick(channel) }
                    )
                }
            }
        }
    }
}

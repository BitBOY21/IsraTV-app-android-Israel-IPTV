package com.isratv.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.isratv.android.ui.screens.ChannelListScreen
import com.isratv.android.ui.screens.FavoritesScreen
import com.isratv.android.ui.screens.PlayerScreen
import com.isratv.android.viewmodel.ChannelViewModel

@Composable
fun TvStreamsApp(
    viewModel: ChannelViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val channels by viewModel.channels.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    // Hide the bottom bar when on the player screen to provide a full-screen experience.
    val showBottomBar = currentRoute != "player/{id}"

    // Use a Box to overlay the floating bottom bar on top of the content.
    Box(modifier = Modifier.fillMaxSize()) {

        // 1. Content Layer - The main navigation host.
        Scaffold(
            // No bottomBar here; it's handled as an overlay below.
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "channel_list",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("channel_list") {
                    ChannelListScreen(
                        channels = channels,
                        isLoading = isLoading,
                        onChannelClick = { channel ->
                            navController.navigate("player/${channel.id}")
                        },
                        onRefresh = { viewModel.loadChannels() }
                    )
                }

                composable("favorites") {
                    FavoritesScreen(
                        onBack = { navController.navigate("channel_list") },
                        onChannelClick = { channel ->
                            navController.navigate("player/${channel.id}")
                        }
                    )
                }

                composable(
                    route = "player/{id}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: return@composable
                    val channel = viewModel.getChannelById(id) ?: return@composable

                    PlayerScreen(
                        channelName = channel.name,
                        url = channel.url,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        // 2. Floating Bottom Bar Layer - Overlays the content at the bottom.
        if (showBottomBar) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp), // Bottom margin for the floating effect.
                contentAlignment = Alignment.Center
            ) {
                NavigationBar(
                    modifier = Modifier
                        .width(220.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(50),
                            spotColor = Color.Black.copy(alpha = 0.2f)
                        )
                        .clip(RoundedCornerShape(50))
                        .height(70.dp),
                    containerColor = Color.White,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0.dp)
                ) {
                    // Channels Tab
                    val isHomeSelected = currentRoute == "channel_list"
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (isHomeSelected) Icons.Filled.Tv else Icons.Outlined.Tv,
                                contentDescription = "Channels"
                            )
                        },
                        label = {
                            Text(
                                "Channels",
                                fontWeight = if (isHomeSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = isHomeSelected,
                        onClick = {
                            navController.navigate("channel_list") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1D1B20),
                            selectedTextColor = Color(0xFF1D1B20),
                            indicatorColor = Color(0xFFE8DEF8),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )

                    // Favorites Tab
                    val isFavSelected = currentRoute == "favorites"
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (isFavSelected) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorites"
                            )
                        },
                        label = {
                            Text(
                                "Favorites",
                                fontWeight = if (isFavSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = isFavSelected,
                        onClick = {
                            navController.navigate("favorites") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1D1B20),
                            selectedTextColor = Color(0xFF1D1B20),
                            indicatorColor = Color(0xFFE8DEF8),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    }
}

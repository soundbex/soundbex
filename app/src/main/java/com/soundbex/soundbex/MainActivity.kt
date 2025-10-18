package com.soundbex.soundbex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.soundbex.soundbex.ui.theme.SoundbexTheme
import com.soundbex.soundbex.PlayerManager

class MainActivity : ComponentActivity() {
    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoundbexTheme {
                SoundbexApp()
            }
        }
    }
}

data class CurrentSong(
    val videoId: String,
    val title: String,
    val artist: String,
    val duration: String,
    val imageUrl: String?
)

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun SoundbexApp() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Home", "Search", "Library")

    var currentSong by remember {
        mutableStateOf(
            CurrentSong(
                videoId = "default",
                title = "Şarkı Çalınmıyor",
                artist = "Lütfen bir şarkı seçin",
                duration = "0:00",
                imageUrl = "https://placehold.co/80x80/cccccc/ffffff?text=Soundbex"
            )
        )
    }

    var isPlaying by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val playerManager = remember { PlayerManager(context) }

    val currentPlayingId = playerManager.currentVideoId
    val isPlayingState = playerManager.isPlayingState

    // Player durumunu takip et
    LaunchedEffect(currentPlayingId, isPlayingState) {
        isPlaying = isPlayingState && currentPlayingId == currentSong.videoId
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "SoundBex",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Column {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(if (currentSong.videoId != "default") 76.dp else 0.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    if (currentSong.videoId != "default") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OptimizedAsyncImage(
                                imageUrl = currentSong.imageUrl,
                                contentDescription = "Now playing",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentSong.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = currentSong.artist,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (isPlaying) {
                                        playerManager.pause()
                                    } else {
                                        if (currentPlayingId == currentSong.videoId) {
                                            playerManager.play()
                                        } else {
                                            // Yeni şarkıyı çal
                                            playerManager.playSong(
                                                currentSong.videoId,
                                                currentSong.title,
                                                currentSong.artist
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    tabs.forEachIndexed { index, title ->
                        NavigationBarItem(
                            icon = {
                                when (index) {
                                    0 -> Icon(Icons.Default.Home, contentDescription = title)
                                    1 -> Icon(Icons.Default.Search, contentDescription = title)
                                    2 -> Icon(Icons.Default.LibraryBooks, contentDescription = title)
                                }
                            },
                            label = { Text(title) },
                            selected = selectedTab == index,
                            onClick = { selectedTab = index }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> HomeScreen()
                1 -> SearchScreen(
                    snackbarHostState = snackbarHostState,
                    playerManager = playerManager,
                    onSongClick = { song ->
                        currentSong = CurrentSong(
                            videoId = song.videoId,
                            title = song.title,
                            artist = song.artist,
                            duration = song.duration,
                            imageUrl = song.imageUrl
                        )
                        // Şarkıyı çalmaya başla
                        playerManager.playSong(song.videoId, song.title, song.artist)
                    }
                )
                2 -> LibraryScreen()
            }
        }
    }
}

@Composable
fun OptimizedAsyncImage(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(false)
            .size(200)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

@Composable
fun HomeScreen() {
    val featuredPlaylists by remember {
        mutableStateOf(
            listOf(
                Playlist("playlist1", "Today's Top Hits", "Most played songs now", "https://picsum.photos/200/200?1"),
                Playlist("playlist2", "Chill Vibes", "Relax and unwind", "https://picsum.photos/200/200?2"),
                Playlist("playlist3", "Workout Mix", "Energy for workout", "https://picsum.photos/200/200?3"),
                Playlist("playlist4", "Discover Weekly", "New recommendations", "https://picsum.photos/200/200?4")
            )
        )
    }

    val quickActions by remember {
        mutableStateOf(
            listOf(
                QuickAction("action1", "Liked Songs", Icons.Default.Favorite, Color(0xFFFF6B6B)),
                QuickAction("action2", "Recently Played", Icons.Default.History, Color(0xFF4ECDC4)),
                QuickAction("action3", "Downloaded", Icons.Default.Download, Color(0xFF45B7D1)),
                QuickAction("action4", "Made For You", Icons.Default.Person, Color(0xFF96CEB4))
            )
        )
    }

    val moods by remember { mutableStateOf(listOf("Happy", "Relaxed", "Energetic", "Focused")) }

    val recentlyPlayed by remember {
        mutableStateOf(
            listOf(
                Song("song1", "Blinding Lights", "The Weeknd", "https://picsum.photos/100/100?1"),
                Song("song2", "Save Your Tears", "The Weeknd", "https://picsum.photos/100/100?2"),
                Song("song3", "Levitating", "Dua Lipa", "https://picsum.photos/100/100?3"),
                Song("song4", "Stay", "The Kid LAROI, Justin Bieber", "https://picsum.photos/100/100?4")
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "SoundBex",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Simple and fast music & podcasting platform",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                items(quickActions, key = { it.id }) { action ->
                    QuickActionCard(action)
                }
            }
        }

        item {
            Text(
                text = "Mood & Genre",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                items(moods, key = { it }) { mood ->
                    FilterChip(
                        selected = false,
                        onClick = { },
                        label = { Text(mood) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Featured Playlists",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { }) {
                    Text("See all")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(featuredPlaylists, key = { it.id }) { playlist ->
            FeaturedPlaylistCard(playlist = playlist)
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recently Played",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { }) {
                    Text("See all")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(recentlyPlayed, key = { it.id }) { song ->
            SongItem(song = song)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun QuickActionCard(action: QuickAction) {
    Card(
        onClick = { },
        modifier = Modifier
            .width(120.dp)
            .height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = action.color.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.title,
                tint = action.color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = action.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FeaturedPlaylistCard(playlist: Playlist) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box {
            OptimizedAsyncImage(
                imageUrl = playlist.imageUrl,
                contentDescription = playlist.title,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playlist.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SongItem(song: Song) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OptimizedAsyncImage(
                imageUrl = song.imageUrl,
                contentDescription = song.title,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(6.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@UnstableApi
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(),
    snackbarHostState: SnackbarHostState,
    playerManager: PlayerManager,
    onSongClick: (SearchResultSong) -> Unit
) {
    val context = LocalContext.current

    var searchText by remember { mutableStateOf("") }
    val results by viewModel.results.collectAsState()

    val currentPlayingId = playerManager.currentVideoId
    val isPlayingState = playerManager.isPlayingState

    LaunchedEffect(viewModel.errorFlow) {
        viewModel.errorFlow.collect { errorMessage ->
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Short
            )
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        TextField(
            value = searchText,
            onValueChange = {
                searchText = it
                if (it.length > 2) viewModel.searchSongs(it)
            },
            placeholder = { Text("Şarkı, sanatçı veya albüm ara...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Arama") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(28.dp)),
            singleLine = true,
            maxLines = 1,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        Spacer(Modifier.height(16.dp))

        if (results.isEmpty() && searchText.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "'${searchText}' için sonuç bulunamadı.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (results.isEmpty() && searchText.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aramaya başlamak için yazın.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(results, key = { it.videoId }) { song ->
                    val isPlayingThisSong = song.videoId == currentPlayingId && isPlayingState

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSongClick(song)
                                playerManager.playSong(song.videoId, song.title, song.artist)
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        AsyncImage(
                            model = song.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                song.title,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (isPlayingThisSong) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlayingThisSong) "Durdur" else "Oynat",
                            tint = if (isPlayingThisSong) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}

@Composable
fun LibraryScreen() {
    val libraryItems by remember {
        mutableStateOf(
            listOf(
                LibraryItem("lib1", "Liked Songs", "0 songs", Icons.Default.Favorite),
                LibraryItem("lib2", "My Playlists", "0 playlists", Icons.Default.PlaylistPlay),
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "Your Library",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(libraryItems, key = { it.id }) { item ->
            LibraryItemRow(item = item)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun LibraryItemRow(item: LibraryItem) {
    Card(
        onClick = { },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class Playlist(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String
)

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val imageUrl: String
)

data class QuickAction(
    val id: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

data class LibraryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@UnstableApi
@Preview(showBackground = true)
@Composable
fun SoundbexAppPreview() {
    SoundbexTheme {
        SoundbexApp()
    }
}
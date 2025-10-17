package com.soundbex.soundbex.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.soundbex.soundbex.data.PodcastItem

@Composable
fun PodcastList() {
    val podcastItems = listOf(
        PodcastItem("Tech Talk Daily", "Alex Johnson", "https://picsum.photos/200/200?random=5"),
        PodcastItem("True Crime Stories", "Sarah Miller", "https://picsum.photos/200/200?random=6"),
        PodcastItem("Business Insights", "Mike Chen", "https://picsum.photos/200/200?random=7"),
        PodcastItem("Health & Wellness", "Dr. Lisa Park", "https://picsum.photos/200/200?random=8")
    )

    LazyRow(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        items(podcastItems) { podcast ->
            PodcastCard(podcast = podcast)
        }
    }
}

@Composable
fun PodcastCard(podcast: PodcastItem) {
    Card(
        modifier = Modifier
            .size(150.dp)
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            AsyncImage(
                model = podcast.coverUrl,
                contentDescription = "Cover for ${podcast.title}",
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Text(
                text = podcast.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = podcast.host,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
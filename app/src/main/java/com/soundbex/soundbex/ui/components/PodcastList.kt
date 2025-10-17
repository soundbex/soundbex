package com.soundbex.soundbex.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
            .width(160.dp)
            .height(200.dp)
            .padding(8.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            AsyncImage(
                model = podcast.coverUrl,
                contentDescription = "Cover for ${podcast.title}",
                modifier = Modifier
                    .size(120.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = podcast.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = podcast.host,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
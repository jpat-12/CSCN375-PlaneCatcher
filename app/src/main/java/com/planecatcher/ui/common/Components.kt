package com.planecatcher.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.planecatcher.core.model.Tier
import com.planecatcher.ui.theme.color

@Composable
fun TierBadge(tier: Tier, modifier: Modifier = Modifier) {
    Text(
        text = tier.displayName.uppercase(),
        color = Color.Black,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.ExtraBold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(tier.color)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Plane photo with a silhouette placeholder when there is no photo or it fails to load. */
@Composable
fun PlanePhoto(url: String?, tier: Tier?, modifier: Modifier = Modifier) {
    val border = tier?.color ?: MaterialTheme.colorScheme.outline
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, border, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url == null) {
            Silhouette()
        } else {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = "Aircraft photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = { Silhouette() },
                loading = { Silhouette() },
            )
        }
    }
}

@Composable
private fun Silhouette() {
    Icon(
        Icons.Filled.Flight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxSize(0.45f),
    )
}

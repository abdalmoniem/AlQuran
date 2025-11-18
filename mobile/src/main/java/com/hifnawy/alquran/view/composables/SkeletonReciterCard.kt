package com.hifnawy.alquran.view.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun SkeletonReciterCard(modifier: Modifier, brush: Brush) {
    Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                    modifier = Modifier
                        .size(50.dp)
                        .padding(10.dp)
                        .clip(CircleShape)
                        .background(brush)
            )

            Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 10.dp)
            ) {

                // Placeholder for the Reciter Name
                Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(brush)
                )

                // Placeholder for the moshaf count text
                // Spacer(modifier = Modifier.height(10.dp))
                Spacer(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(50.dp)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(brush)
                )
            }
        }
    }
}

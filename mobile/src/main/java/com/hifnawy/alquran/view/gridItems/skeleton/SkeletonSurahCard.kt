package com.hifnawy.alquran.view.gridItems.skeleton

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun SkeletonSurahCard(modifier: Modifier = Modifier, brush: Brush) {
    Card(
            modifier = modifier.aspectRatio(1f),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(20.dp)
    ) {
        Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // placeholder for surah name
            Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(brush)
            )

            // placeholder for surah type
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

package com.hifnawy.alquran.view.gridItems

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter

@Composable
fun MoshafCard(
        modifier: Modifier = Modifier,
        reciter: Reciter,
        moshaf: Moshaf,
        onMoshafClick: (Reciter, Moshaf) -> Unit = { _, _ -> }
) {
    ElevatedCard(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            // elevation = CardDefaults.elevatedCardElevation(20.dp),
            onClick = { onMoshafClick(reciter, moshaf) },
    ) {
        Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                    painter = painterResource(id = R.drawable.book_24px),
                    contentDescription = "Moshaf Icon"
            )

            Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 10.dp)
                        .basicMarquee(),
                    text = "${moshaf.name} - ${pluralStringResource(R.plurals.surah_count, moshaf.surahsCount, moshaf.surahsCount)}",
            )
        }
    }
}
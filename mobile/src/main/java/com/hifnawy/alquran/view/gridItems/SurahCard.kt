package com.hifnawy.alquran.view.gridItems

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.utils.TextUtil.highlightMatchingText
import com.hifnawy.alquran.shared.R as Rs

@Composable
fun SurahCard(
        modifier: Modifier = Modifier,
        surah: Surah,
        searchQuery: String = "",
        onClick: (surah: Surah) -> Unit
) {
    Card(
            modifier = modifier.aspectRatio(1f),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(20.dp),
            onClick = { onClick(surah) },
    ) {
        Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                    modifier = Modifier.basicMarquee(),
                    text = highlightMatchingText(
                            fullText = surah.name,
                            query = searchQuery,
                            highlightColor = MaterialTheme.colorScheme.primary,
                            defaultColor = MaterialTheme.colorScheme.onSurface
                    ),
                    fontSize = 45.sp,
                    fontFamily = FontFamily(Font(Rs.font.decotype_thuluth_2)),
            )


            Text(
                    text = when (surah.makkia) {
                        1    -> stringResource(R.string.surah_makkia)
                        else -> stringResource(R.string.surah_madaneyya)
                    },
                    fontSize = 25.sp,
                    fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
            )
        }
    }
}

package com.hifnawy.alquran.view.gridItems

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.utils.TextUtil.highlightMatchingText
import com.hifnawy.alquran.shared.R as Rs

@Composable
fun ReciterCard(
        modifier: Modifier = Modifier,
        reciter: Reciter,
        searchQuery: String = "",
        isExpanded: Boolean,
        onToggleExpand: () -> Unit = {},
        onMoshafClick: (Reciter, Moshaf) -> Unit = { _, _ -> }
) {
    Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(20.dp),
            onClick = onToggleExpand,
    ) {
        Column(
                modifier = Modifier
                    .fillMaxWidth()
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                if (reciter.moshaf.isNotEmpty()) {
                    val iconDrawableId = when {
                        isExpanded -> R.drawable.arrow_up_24px
                        else       -> R.drawable.arrow_down_24px
                    }

                    Icon(
                            modifier = Modifier
                                .size(50.dp)
                                .padding(10.dp),
                            painter = painterResource(id = iconDrawableId),
                            contentDescription = "Show Moshafs"
                    )
                }

                Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 10.dp)
                ) {
                    Text(
                            modifier = Modifier.basicMarquee(),
                            text = highlightMatchingText(
                                    fullText = reciter.name,
                                    query = searchQuery,
                                    highlightColor = MaterialTheme.colorScheme.primary,
                                    defaultColor = MaterialTheme.colorScheme.onSurface
                            ),
                            fontSize = 40.sp,
                            fontFamily = FontFamily(Font(Rs.font.decotype_thuluth_2)),

                            )

                    Text(
                            modifier = Modifier.basicMarquee(),
                            text = pluralStringResource(R.plurals.moshaf_count, reciter.moshaf.size, reciter.moshaf.size),
                            fontSize = 30.sp,
                            fontFamily = FontFamily(Font(Rs.font.diwany_1))
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, bottom = 10.dp, end = 10.dp)
                ) {
                    reciter.moshaf.forEach { moshaf ->
                        MoshafCard(reciter = reciter, moshaf = moshaf, onMoshafClick = onMoshafClick)
                    }
                }
            }
        }
    }
}

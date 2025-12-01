package com.hifnawy.alquran.view.screens

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.hifnawy.alquran.BuildConfig
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.R as Rs

@Composable
fun SettingsScreen() {
    Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceDim)
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(10.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
    ) {
        Text(
                modifier = Modifier
                    .basicMarquee()
                    .padding(10.dp),
                text = stringResource(R.string.navbar_settings),
                maxLines = 1,
                fontSize = 60.sp,
                fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                color = MaterialTheme.colorScheme.onSurface
        )
        Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
        ) {
            AppearanceSection()
            AboutSection()
        }
    }
}

@Composable
private fun AppearanceSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
                modifier = Modifier
                    .basicMarquee()
                    .padding(10.dp),
                text = stringResource(R.string.settings_appearance_section),
                maxLines = 1,
                fontSize = 25.sp,
                fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        ) {
            Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeSettings()
                DynamicColorsSettings()
            }
        }
    }
}

@Composable
private fun ThemeSettings() {
    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(20.dp)
    ) {
        val options = listOf(
                R.drawable.theme_light_mode_outlined_24px to R.drawable.theme_light_mode_filled_24px,
                R.drawable.theme_auto_outlined_24px to R.drawable.theme_auto_filled_24px,
                R.drawable.theme_dark_mode_outlined_24px to R.drawable.theme_dark_mode_filled_24px,
        )

        var selectedIndex by remember { mutableIntStateOf(1) }

        Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                    modifier = Modifier
                        .size(50.dp)
                        .padding(horizontal = 10.dp),
                    painter = painterResource(id = R.drawable.theme_24px),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null
            )

            Column {
                Text(
                        text = stringResource(R.string.settings_theme_label),
                        fontSize = 15.sp,
                        fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                        color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                        text = stringResource(R.string.settings_theme_description),
                        fontSize = 10.sp,
                        fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                        color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            SingleChoiceSegmentedButtonRow {
                options.forEachIndexed { index, icon ->
                    SegmentedButton(
                            selected = selectedIndex == index,
                            onClick = { selectedIndex = index },
                            shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = options.size
                            )
                    ) {
                        val icon = when (selectedIndex) {
                            index -> icon.second
                            else  -> icon.first
                        }
                        Icon(
                                painter = painterResource(icon),
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun DynamicColorsSettings() {
    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(20.dp)
    ) {
        var checked by remember { mutableStateOf(true) }

        Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                    modifier = Modifier
                        .size(50.dp)
                        .padding(horizontal = 10.dp),
                    painter = painterResource(id = R.drawable.dynamic_colors_24px),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null
            )

            Column {
                Text(
                        text = stringResource(R.string.settings_dynamic_colors_label),
                        fontSize = 15.sp,
                        fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                        color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                        text = stringResource(R.string.settings_dynamic_colors_description),
                        fontSize = 10.sp,
                        fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                        color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Switch(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    thumbContent = {
                        Icon(
                                painter = when {
                                    checked -> painterResource(id = R.drawable.check_24px)
                                    else    -> painterResource(id = R.drawable.close_24px)
                                },
                                contentDescription = null
                        )
                    }
            )
        }
    }
}

@Composable
private fun AboutSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
                modifier = Modifier
                    .basicMarquee()
                    .padding(10.dp),
                text = stringResource(R.string.settings_about_section),
                maxLines = 1,
                fontSize = 25.sp,
                fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        ) {
            Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NotificationsSettings()
                TranslationCard()
                PrivacyPolicyCard()
                ContactCard()
                AppDetailsCard()
            }
        }
    }
}

@Composable
private fun NotificationsSettings() {
}

@Composable
private fun TranslationCard() {
}

@Composable
private fun PrivacyPolicyCard() {
}

@Composable
private fun ContactCard() {
}

@Composable
private fun AppDetailsCard() {
    val activity = LocalActivity.current

    Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                        modifier = Modifier.size(250.dp),
                        painter = painterResource(id = R.mipmap.ic_launcher_monochrome),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                )

                Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 30.sp,
                        fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                        color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                        text = stringResource(R.string.settings_about_version_code, BuildConfig.VERSION_CODE),
                        fontSize = 25.sp,
                        fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                        color = MaterialTheme.colorScheme.onSurface
                )

                val versionName = BuildConfig.VERSION_NAME.split(".")
                    .map { stringResource(R.string.settings_about_version_name_part, it.toInt()) }
                    .joinToString(separator = ".") { it }
                Text(
                        text = stringResource(R.string.settings_about_version_name, versionName),
                        fontSize = 25.sp,
                        fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                        color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                        text = stringResource(R.string.settings_about_developer_name),
                        fontSize = 20.sp,
                        fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                        color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val githubIntent = Intent().apply {
                    action = Intent.ACTION_VIEW
                    data = activity?.getString(R.string.settings_about_github_url)?.toUri()
                }

                IconButton(
                        modifier = Modifier.size(80.dp),
                        onClick = { activity?.startActivity(githubIntent) }
                ) {
                    Icon(
                            painter = painterResource(R.drawable.github_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                        modifier = Modifier.size(80.dp),
                        onClick = { },
                        enabled = false
                ) {
                    Icon(
                            painter = painterResource(R.drawable.crowdin_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                        modifier = Modifier.size(80.dp),
                        onClick = { },
                        enabled = false
                ) {
                    Icon(
                            painter = painterResource(R.drawable.fdroid_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                        modifier = Modifier.size(80.dp),
                        onClick = { },
                        enabled = false
                ) {
                    Icon(
                            painter = painterResource(R.drawable.izzyondroid_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

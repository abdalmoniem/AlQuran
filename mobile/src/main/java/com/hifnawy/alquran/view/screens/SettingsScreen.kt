package com.hifnawy.alquran.view.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.Html
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.hifnawy.alquran.BuildConfig
import com.hifnawy.alquran.R
import com.hifnawy.alquran.datastore.SettingsDataStore
import com.hifnawy.alquran.shared.QuranApplication
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.utils.HTML.MutableHtml
import com.hifnawy.alquran.utils.HTML.htmlBody
import kotlinx.coroutines.launch
import timber.log.Timber
import com.hifnawy.alquran.shared.R as Rs

/**
 * A Composable function that displays the main settings screen of the application.
 *
 * This screen provides a centralized location for users to configure various aspects of the app.
 * It is structured vertically using a [Column] layout, which contains distinct sections for
 * different categories of settings, such as "Appearance" and "About".
 *
 * Key Features:
 * - **Main Title**: A prominent title `Settings` is displayed at the top using a custom font.
 * - **Scrolling Content**: The main body of the settings is scrollable, ensuring all options
 *   are accessible regardless of screen size.
 * - **SafeArea Handling**: The layout respects the device's physical features by applying padding
 *   for the status bar and display cutouts ([Modifier.statusBarsPadding] and [Modifier.displayCutoutPadding]).
 * - **Modular Sections**: The screen is composed of modular sub-composables like [AppearanceSection]
 *   and [AboutSection], promoting code reusability and clarity.
 * - **Consistent Styling**: It uses [MaterialTheme] colors and typography to maintain a consistent
 *   look and feel with the rest of the application.
 *
 * The overall structure consists of a main [Column] for the screen's background and padding,
 * a [Text] for the title, and another nested [Column] that holds the scrollable settings sections.
 * This separation ensures the title remains fixed at the top while the settings content can be
 * scrolled independently.
 *
 * @see AppearanceSection
 * @see AboutSection
 */
@Composable
fun SettingsScreen() {
    Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceDim)
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(horizontal = 10.dp),
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

/**
 * A Composable function that groups and displays settings related to the application's appearance.
 *
 * This function acts as a container for various visual settings, organized within a
 * [SettingsSectionCard]. It provides a clear and structured way to present appearance-related
 * options to the user.
 *
 * The section is titled `Appearance` and includes the following sub-settings:
 * - [LanguageSettings]: Allows the user to change the application's language.
 * - [ThemeSettings]: Provides options to switch between light, dark, and system default themes.
 * - [DynamicColorsSettings]: Toggles the use of Material You dynamic colors based on the device's wallpaper.
 *
 * @see SettingsSectionCard
 * @see LanguageSettings
 * @see ThemeSettings
 * @see DynamicColorsSettings
 */
@Composable
private fun AppearanceSection() {
    SettingsSectionCard(title = stringResource(R.string.settings_appearance_section)) {
        LanguageSettings()
        ThemeSettings()
        DynamicColorsSettings()
    }
}

/**
 * A Composable that displays the language settings item.
 *
 * This setting allows the user to change the application's language. It displays the
 * current language and country code. Tapping this item opens the system's application-specific
 * locale settings screen on Android 13 (Tiramisu) and above, or the general application
 * details screen on older versions, where the user can manage the language.
 *
 * The current locale is observed from [QuranApplication.currentLocaleInfo] and is updated
 * via a [LaunchedEffect]. When the item is clicked, haptic feedback is triggered,
 * and an [Intent] is fired to navigate the user to the appropriate system settings screen.
 * The current locale is also persisted using [SettingsDataStore].
 *
 * @see SettingsItemCard
 * @see QuranApplication.currentLocaleInfo
 * @see Settings.ACTION_APP_LOCALE_SETTINGS
 * @see Settings.ACTION_APPLICATION_DETAILS_SETTINGS
 */
@Composable
private fun LanguageSettings() {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val haptic = LocalHapticFeedback.current
    val settingsDataStore = remember { SettingsDataStore }

    val intent = Intent().apply {
        action = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> Settings.ACTION_APP_LOCALE_SETTINGS
            else                                                  -> Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        }
        data = "package:${activity?.packageName}".toUri()
    }

    var currentLocale by rememberSaveable { mutableStateOf(QuranApplication.currentLocaleInfo) }

    val onClick = {
        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        activity?.startActivity(intent) ?: Unit
    }

    LaunchedEffect(QuranApplication.currentLocaleInfo) {
        currentLocale = QuranApplication.currentLocaleInfo
        settingsDataStore.setLocale(context, currentLocale)
        Timber.debug("Current locale: ${currentLocale.language}")
    }

    SettingsItemCard(
            onClick = onClick,
            icon = painterResource(id = R.drawable.language_24px),
            title = stringResource(R.string.settings_language_label),
            description = stringResource(R.string.settings_language_description)
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Button(
                onClick = onClick,
                modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            val localeName = currentLocale.language
            val localeCountry = currentLocale.country
            val language = when {
                localeCountry.isBlank() -> localeName
                else                    -> "$localeName ($localeCountry)"
            }

            Text(
                    modifier = Modifier.basicMarquee(),
                    text = language,
                    fontSize = 25.sp,
                    fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                    color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/**
 * A Composable that displays the theme selection setting.
 *
 * This setting allows the user to choose between Light, Dark, and System Default themes.
 * The UI consists of a [SingleChoiceSegmentedButtonRow] with icons representing each theme option:
 * - Light Mode
 * - Auto (System Default)
 * - Dark Mode
 *
 * The current theme selection is retrieved from [SettingsDataStore] when the Composable is first
 * displayed using a [LaunchedEffect]. When a user selects a new theme, haptic feedback is triggered,
 * the UI state is updated, and the new preference is saved asynchronously to the [SettingsDataStore].
 * The entire item is also clickable, cycling through the available themes.
 *
 * @see SettingsItemCard
 * @see SingleChoiceSegmentedButtonRow
 * @see SettingsDataStore.Theme
 * @see SettingsDataStore.getTheme
 * @see SettingsDataStore.setTheme
 */
@Composable
private fun ThemeSettings() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val settingsDataStore = remember { SettingsDataStore }

    val options = listOf(
            R.drawable.theme_light_mode_outlined_24px to R.drawable.theme_light_mode_filled_24px,
            R.drawable.theme_auto_outlined_24px to R.drawable.theme_auto_filled_24px,
            R.drawable.theme_dark_mode_outlined_24px to R.drawable.theme_dark_mode_filled_24px,
    )

    var currentTheme by rememberSaveable { mutableStateOf(SettingsDataStore.Theme.SYSTEM) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(currentTheme.code) }

    val onClick = { index: Int ->
        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        selectedIndex = index

        coroutineScope.launch {
            currentTheme = when (selectedIndex) {
                0 -> SettingsDataStore.Theme.LIGHT
                1 -> SettingsDataStore.Theme.SYSTEM
                else -> SettingsDataStore.Theme.DARK
            }

            settingsDataStore.setTheme(context, currentTheme)
        }

        Unit
    }

    LaunchedEffect(Unit) {
        currentTheme = settingsDataStore.getTheme(context)
        selectedIndex = currentTheme.code
    }

    SettingsItemCard(
            onClick = { onClick((selectedIndex + 1) % options.size) },
            icon = painterResource(id = R.drawable.theme_24px),
            title = stringResource(R.string.settings_theme_label),
            description = stringResource(R.string.settings_theme_description)
    ) {
        Spacer(modifier = Modifier.weight(1f))

        SingleChoiceSegmentedButtonRow {
            options.forEachIndexed { index, (unSelectedIcon, selectedIcon) ->
                SegmentedButton(
                        selected = selectedIndex == index,
                        onClick = { onClick(index) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                ) {
                    val icon = when (selectedIndex) {
                        index -> selectedIcon
                        else  -> unSelectedIcon
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

/**
 * A Composable that provides a setting to enable or disable Material You dynamic colors.
 *
 * This setting is presented as an item within the `Appearance` section. It consists of a
 * title, a description, and a [Switch] component to toggle the feature on or off. This
 * feature is only available on Android 12 (API 31) and higher.
 *
 * State Management:
 * - The `checked` state of the [Switch] is managed by a [rememberSaveable] state variable.
 * - The initial state is loaded from [SettingsDataStore] using a [LaunchedEffect].
 * - When the user toggles the switch, the new value is persisted to [SettingsDataStore]
 *   asynchronously via a coroutine.
 *
 * User Interaction:
 * - Tapping the item or the switch toggles the setting.
 * - Haptic feedback ([HapticFeedbackType.ToggleOn] or [HapticFeedbackType.ToggleOff])
 *   is provided upon interaction to enhance the user experience.
 * - The switch's thumb displays a check icon when enabled and a close icon when disabled,
 *   providing clear visual feedback.
 *
 * @see SettingsItemCard
 * @see Switch
 * @see SettingsDataStore.getDynamicColors
 * @see SettingsDataStore.setDynamicColors
 */
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun DynamicColorsSettings() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val settingsDataStore = remember { SettingsDataStore }

    var checked by rememberSaveable { mutableStateOf(true) }

    val onClick = {
        val hapticFeedbackType = when {
            checked -> HapticFeedbackType.ToggleOff
            else    -> HapticFeedbackType.ToggleOn
        }
        haptic.performHapticFeedback(hapticFeedbackType)
        checked = !checked

        coroutineScope.launch { settingsDataStore.setDynamicColors(context, checked) }
        Unit
    }

    LaunchedEffect(Unit) {
        checked = settingsDataStore.getDynamicColors(context)
    }

    SettingsItemCard(
            onClick = onClick,
            icon = painterResource(id = R.drawable.dynamic_colors_24px),
            title = stringResource(R.string.settings_dynamic_colors_label),
            description = stringResource(R.string.settings_dynamic_colors_description),
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Switch(
                checked = checked,
                onCheckedChange = { onClick() },
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

/**
 * A Composable function that groups and displays settings and information related to the application.
 *
 * This function serves as a container for various `About` items, organized within a
 * [SettingsSectionCard]. It provides a structured section for users to access notifications settings,
 * contribute to translation, view the privacy policy, contact the developer, and see app details.
 *
 * The section is titled `About` and includes the following sub-composables:
 * - [NotificationsSettings]: Navigates the user to the system's notification settings for the app.
 * - [TranslationCard]: Provides a link to the project's translation platform (Crowdin).
 * - [PrivacyPolicyCard]: Displays the app's privacy policy in a modal bottom sheet.
 * - [ContactCard]: Opens an email client with a pre-filled template for contacting the developer.
 * - [AppDetailsCard]: Shows application details like version, developer name, and links to external resources like GitHub.
 *
 * @see SettingsSectionCard
 * @see NotificationsSettings
 * @see TranslationCard
 * @see PrivacyPolicyCard
 * @see ContactCard
 * @see AppDetailsCard
 */
@Composable
private fun AboutSection() {
    SettingsSectionCard(title = stringResource(R.string.settings_about_section)) {
        NotificationsSettings()
        TranslationCard()
        PrivacyPolicyCard()
        ContactCard()
        AppDetailsCard()
    }
}

/**
 * A Composable that displays the notification settings item.
 *
 * This setting provides a direct shortcut for the user to manage the application's
 * notification settings in the Android system. When tapped, it triggers haptic feedback
 * and launches an [Intent] with [Settings.ACTION_APP_NOTIFICATION_SETTINGS], which
 * opens the app-specific notification channel settings screen.
 *
 * This allows the user to configure notification permissions, sounds, and other
 * preferences without leaving the app context entirely.
 *
 * @see SettingsItemCard
 * @see Settings.ACTION_APP_NOTIFICATION_SETTINGS
 */
@Composable
private fun NotificationsSettings() {
    val activity = LocalActivity.current
    val haptic = LocalHapticFeedback.current
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(Settings.EXTRA_APP_PACKAGE, activity?.packageName)
    }

    SettingsItemCard(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                activity?.startActivity(intent)
            },
            icon = painterResource(id = R.drawable.notifications_24px),
            title = stringResource(R.string.settings_notifications_label),
            description = stringResource(R.string.settings_notifications_description),
    )
}

/**
 * A Composable that displays a settings item for contributing to the app's translation.
 *
 * This card is part of the `About` section. It presents an option for users to help
 * translate the application into different languages. When the user taps on this item,
 * it performs the following actions:
 * 1.  Triggers haptic feedback for a tactile response.
 * 2.  Creates and launches an [Intent] with [Intent.ACTION_VIEW] to open a URL.
 * 3.  The URL, retrieved from string resources, directs the user to the project's
 *     translation platform (e.g., Crowdin).
 *
 * The card is built using the [SettingsItemCard] composable, providing a consistent look and
 * feel with other settings items. It includes an icon, a title, and a description.
 *
 * @see SettingsItemCard
 * @see Intent.ACTION_VIEW
 */
@Composable
private fun TranslationCard() {
    val activity = LocalActivity.current
    val haptic = LocalHapticFeedback.current
    val intent = Intent().apply {
        action = Intent.ACTION_VIEW
        data = stringResource(R.string.settings_translation_crowdin_url).toUri()
    }

    SettingsItemCard(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                activity?.startActivity(intent)
            },
            icon = painterResource(id = R.drawable.translate_24px),
            title = stringResource(R.string.settings_translation_label),
            description = stringResource(R.string.settings_translation_description),
    )
}

/**
 * A Composable that displays the Privacy Policy settings item.
 *
 * This function creates a card that, when clicked, reveals the application's privacy policy.
 * The policy itself is displayed within a [ModalBottomSheet], providing a non-intrusive and
 * easily dismissible overlay.
 *
 * Key Features:
 * - **Clickable Item**: Encapsulated within a [SettingsItemCard], this Composable displays a
 *   title ("Privacy Policy") and a brief description. Tapping it triggers haptic feedback and
 *   opens the bottom sheet.
 * - **Modal Bottom Sheet**: The visibility of the sheet is controlled by a `rememberSaveable`
 *   state variable (`isBottomSheetVisible`), ensuring the state is preserved across configuration
 *   changes.
 * - **HTML Content Rendering**: The privacy policy content is loaded from a string resource
 *   which contains HTML. It uses a custom utility ([MutableHtml]) to parse and style the HTML
 *   (e.g., coloring headers) before converting it into an [AnnotatedString] for display.
 * - **Scrollable Content**: The content within the bottom sheet is placed in a scrollable [Column],
 *   ensuring the full text is accessible regardless of screen height.
 * - **Styling**: The sheet and its content are styled using [MaterialTheme] colors and typography
 *   to maintain a consistent look with the rest of the app.
 *
 * @see SettingsItemCard
 * @see ModalBottomSheet
 * @see MutableHtml
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
private fun PrivacyPolicyCard() {
    val haptic = LocalHapticFeedback.current
    var isBottomSheetVisible by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    SettingsItemCard(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                isBottomSheetVisible = true
            },
            icon = painterResource(id = R.drawable.privacy_policy_24px),
            title = stringResource(R.string.settings_privacy_policy_label),
            description = stringResource(R.string.settings_privacy_policy_description),
    )

    if (!isBottomSheetVisible) return

    ModalBottomSheet(
            modifier = Modifier.fillMaxWidth(),
            sheetState = sheetState,
            onDismissRequest = { isBottomSheetVisible = false }
    ) {
        val privacyPolicyHtml = stringResource(R.string.settings_privacy_policy_content).trimIndent()
        val privacyPolicyBody = privacyPolicyHtml.htmlBody?.run {
            MutableHtml(this).run {
                setHtmlTagColor("h1", MaterialTheme.colorScheme.primary)
                setHtmlTagColor("h2", MaterialTheme.colorScheme.secondary)
                setHtmlTagColor("h3", MaterialTheme.colorScheme.tertiary)

                Html.fromHtml(content, Html.FROM_HTML_OPTION_USE_CSS_COLORS).annotatedString
            }
        } ?: AnnotatedString("")

        Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright)
        ) {
            Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
            ) {
                Text(
                        text = stringResource(R.string.settings_privacy_policy_label),
                        fontSize = MaterialTheme.typography.displayLargeEmphasized.fontSize,
                        fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                        color = MaterialTheme.colorScheme.primary
                )

                Text(
                        text = privacyPolicyBody,
                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                        fontFamily = FontFamily(Font(Rs.font.aref_ruqaa))
                )
            }
        }
    }
}

/**
 * A Composable that provides a way for the user to contact the developer via email.
 *
 * This function creates a settings item card that, when tapped, launches an email client.
 * An [Intent] with the action [Intent.ACTION_SENDTO] and `mailto:` data is prepared.
 *
 * The email intent is pre-populated with:
 * - The developer's email address in the "To" field.
 * - A pre-formatted body containing useful diagnostic information, such as:
 *   - App version code and name.
 *   - Android OS version and SDK level.
 *   - Device manufacturer and model.
 *
 * This pre-filled information helps the developer diagnose any issues the user might be reporting.
 * Haptic feedback is triggered on click to improve user experience.
 *
 * @see SettingsItemCard
 * @see Intent.ACTION_SENDTO
 */
@Composable
private fun ContactCard() {
    val activity = LocalActivity.current
    val haptic = LocalHapticFeedback.current
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        val versionName = BuildConfig.VERSION_NAME.split(".")
            .map { stringResource(R.string.locale_int, it.toInt()) }
            .joinToString(separator = ".") { it }

        data = "mailto:".toUri()
        putExtra(Intent.EXTRA_EMAIL, arrayOf(stringResource(R.string.settings_contact_developer_email)))
        putExtra(
                Intent.EXTRA_TEXT,
                """


                            -------------
                            ${stringResource(R.string.settings_about_version_code, BuildConfig.VERSION_CODE)}
                            ${stringResource(R.string.settings_about_version_name, versionName)}
                            ${stringResource(R.string.settings_contact_android_version, Build.VERSION.RELEASE)}
                            ${stringResource(R.string.settings_contact_android_sdk, Build.VERSION.SDK_INT)}
                            ${stringResource(R.string.settings_contact_device_info, "${Build.MANUFACTURER}: ${Build.MODEL}")}
                        """.trimIndent()
        )
    }

    SettingsItemCard(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                activity?.startActivity(intent)
            },
            icon = painterResource(id = R.drawable.mail_24px),
            title = stringResource(R.string.settings_contact_label),
            description = stringResource(R.string.settings_contact_description),
    )
}

/**
 * A Composable that displays detailed information about the application.
 *
 * This card, part of the `About` section, serves as a visual summary of the app.
 * It is structured into two main vertical columns:
 *
 * - **Start Column (App Information)**:
 *      - Displays the monochrome app icon.
 *      - Shows the application name (`app_name`).
 *      - Lists the version code and a localized version name.
 *      - Credits the developer.
 *
 * - **End Column (External Links)**:
 *      - Contains several Icon Buttons linking to external resources.
 *      - The GitHub button is functional and opens the project's repository URL using an [Intent].
 *      - Other buttons (Crowdin, F-Droid, IzzyOnDroid) are present as placeholders but are currently disabled.
 *
 * All text elements use a custom font and are styled with colors from the [MaterialTheme].
 * User interactions, like clicking the GitHub icon, trigger haptic feedback.
 *
 * @see SettingsItemCard
 * @see BuildConfig
 * @see Intent.ACTION_VIEW
 */
@Composable
private fun AppDetailsCard() {
    val activity = LocalActivity.current
    val haptic = LocalHapticFeedback.current
    val versionName = BuildConfig.VERSION_NAME.split(".")
        .map { stringResource(R.string.locale_int, it.toInt()) }
        .joinToString(separator = ".") { it }
    val githubIntent = Intent().apply {
        action = Intent.ACTION_VIEW
        data = activity?.getString(R.string.settings_about_github_url)?.toUri()
    }

    val crowdinIntent = Intent().apply {
        action = Intent.ACTION_VIEW
        data = stringResource(R.string.settings_translation_crowdin_url).toUri()
    }

    SettingsItemCard(cardContainerColor = MaterialTheme.colorScheme.primaryContainer) {
        Column(
                modifier = Modifier
                    .weight(2f)
                    .height(500.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                    modifier = Modifier.size(350.dp),
                    painter = painterResource(id = R.drawable.app_icon_monochrome),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
            )

            Text(
                    modifier = Modifier.basicMarquee(),
                    text = stringResource(R.string.app_name),
                    fontSize = 30.sp,
                    fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                    modifier = Modifier.basicMarquee(),
                    text = stringResource(R.string.settings_about_version_code, BuildConfig.VERSION_CODE),
                    fontSize = 25.sp,
                    fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                    modifier = Modifier.basicMarquee(),
                    text = stringResource(R.string.settings_about_version_name, versionName),
                    fontSize = 25.sp,
                    fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                    modifier = Modifier.basicMarquee(),
                    text = stringResource(R.string.settings_about_developer_name),
                    fontSize = 20.sp,
                    fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Column(
                modifier = Modifier
                    .weight(1f)
                    .height(500.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                    modifier = Modifier.size(80.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        activity?.startActivity(githubIntent)
                    }
            ) {
                Icon(
                        painter = painterResource(R.drawable.github_icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                    modifier = Modifier.size(80.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        activity?.startActivity(crowdinIntent)
                    }
            ) {
                Icon(
                        painter = painterResource(R.drawable.crowdin_icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                )
            }

            // IconButton(
            //         modifier = Modifier.size(80.dp),
            //         onClick = {
            //             haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            //         }
            // ) {
            //     Icon(
            //             painter = painterResource(R.drawable.fdroid_icon),
            //             contentDescription = null,
            //             tint = MaterialTheme.colorScheme.primary
            //     )
            // }
            //
            // IconButton(
            //         modifier = Modifier.size(80.dp),
            //         onClick = {
            //             haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            //         }
            // ) {
            //     Icon(
            //             painter = painterResource(R.drawable.izzyondroid_icon),
            //             contentDescription = null,
            //             tint = MaterialTheme.colorScheme.primary
            //     )
            // }
        }
    }
}

/**
 * A reusable Composable that creates a styled card for grouping related settings.
 *
 * This function displays a prominent title above an [OutlinedCard]. The card itself
 * contains content provided via a composable lambda. This structure helps organize the
 * settings screen into visually distinct and logical sections.
 *
 * The layout consists of a vertical [Column] that holds:
 * - A [Text] composable for the section [title].
 * - An [OutlinedCard] that wraps the [content]. The card has a rounded shape,
 *   elevation, a border, and specific background colors drawn from the [MaterialTheme].
 *
 * The [content] lambda is rendered inside a [Column] within the card, with vertical
 * spacing applied between its children.
 *
 * @param title [String] The text to be displayed as the header for this settings section.
 * @param content [@Composable ColumnScope.() -> Unit][content] A composable lambda that defines the
 *   settings items to be displayed inside the card.
 */
@Composable
private fun SettingsSectionCard(
        title: String,
        content: @Composable ColumnScope.() -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
                modifier = Modifier
                    .basicMarquee()
                    .padding(10.dp),
                text = title,
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
                    verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * A flexible and reusable Composable for displaying a single setting item within a card.
 *
 * This function creates a styled [Card] that can be configured with an optional icon,
 * title, and description. It also supports custom content and an [onClick] handler, making it
 * a versatile building block for settings screens.
 *
 * The layout is a [Row] that arranges its children horizontally:
 * - An optional [Icon] on the far left.
 * - A [Column] containing an optional [title] and [description].
 * - A flexible content slot provided by the [content] lambda, which is typically used
 *   for interactive elements like switches, buttons, or segmented controls.
 *
 * If an [onClick] lambda is provided, the entire card becomes clickable. The card's
 * background color can be customized via the [cardContainerColor] parameter.
 *
 * @param onClick [(() -> Unit)?][onClick] An optional lambda to be executed when the card is clicked. If `null`,
 *   the card will not be clickable.
 * @param icon [Painter?][Painter] An optional [Painter] for the icon to be displayed at the start of the card.
 * @param title [String?][String] An optional [String] for the main title of the setting item.
 * @param description [String?][String] An optional [String] for a brief description displayed below the title.
 * @param cardContainerColor [Color] The background [Color] of the card. Defaults to
 *   [MaterialTheme.colorScheme.surfaceBright][ColorScheme.surfaceBright].
 * @param content [@Composable RowScope.() -> Unit][content] A composable lambda that defines custom content to be placed at the end
 *   of the row. This slot is scoped to a [RowScope], allowing for flexible layout arrangements.
 */
@Composable
private fun SettingsItemCard(
        onClick: (() -> Unit)? = null,
        icon: Painter? = null,
        title: String? = null,
        description: String? = null,
        cardContainerColor: Color = MaterialTheme.colorScheme.surfaceBright,
        content: @Composable RowScope.() -> Unit = {}
) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardContainerColor),
            elevation = CardDefaults.cardElevation(20.dp)
    ) {
        val boxModifier = Modifier
            .fillMaxSize()
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier)

        Box(modifier = boxModifier) {
            Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
            ) {
                icon?.let {
                    Icon(
                            modifier = Modifier
                                .size(50.dp)
                                .padding(horizontal = 10.dp),
                            painter = icon,
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = null
                    )
                }

                Column {
                    title?.let {
                        Text(
                                modifier = Modifier.basicMarquee(),
                                text = it,
                                fontSize = 25.sp,
                                fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                                color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    description?.let {
                        Text(
                                modifier = Modifier.basicMarquee(),
                                text = it,
                                fontSize = 20.sp,
                                fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                                color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                content()
            }
        }
    }
}

/**
 * A Jetpack Compose Preview function for the [SettingsScreen].
 *
 * This Composable is annotated with [@Preview], allowing developers to visualize the
 * [SettingsScreen] within Android Studio's design tools without needing to run the
 * full application on a device or emulator.
 *
 * The preview is configured with specific parameters to simulate a realistic environment:
 * - `device = Devices.PIXEL_9_PRO_XL`: Renders the preview on a screen size equivalent to a Pixel 9 Pro XL.
 * - `locale = "ar"`: Displays the UI using the Arabic language locale to test right-to-left (RTL)
 *   layouts and localized strings.
 *
 * This setup is crucial for verifying the layout, typography, and overall appearance of the
 * settings screen in a common target configuration.
 *
 * @see SettingsScreen
 * @see Preview
 */
@Composable
@Preview(device = Devices.PIXEL_9_PRO_XL, locale = "ar")
fun SettingsScreenPreview() {
    SettingsScreen()
}

package com.hifnawy.alquran.view.player.widgets

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.domain.QuranMediaService
import com.hifnawy.alquran.shared.domain.ServiceStatus
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.shared.utils.DrawableResUtil.surahDrawableId
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.utils.sampleReciters
import com.hifnawy.alquran.utils.sampleSurahs
import com.hifnawy.alquran.view.activities.MainActivity
import com.hifnawy.alquran.view.player.widgets.PlayerWidget.Companion.appWidgetId
import com.hifnawy.alquran.view.player.widgets.PlayerWidget.Companion.updateGlanceWidgets
import com.hifnawy.alquran.view.player.widgets.PlayerWidgetBitmaps.defaultSurahBitmap
import com.hifnawy.alquran.view.player.widgets.PlayerWidgetBitmaps.defaultSurahBlurredBitmap
import com.hifnawy.alquran.view.player.widgets.PlayerWidgetBitmaps.surahImages
import com.hifnawy.alquran.view.theme.WidgetTheme
import timber.log.Timber
import com.hifnawy.alquran.shared.R as Rs

/**
 * A [GlanceAppWidget] that provides a player control interface on the user's home screen.
 *
 * This widget displays the currently playing Surah and Reciter, along with media controls
 * for play/pause, skip to next, and skip to previous. It interacts with the [QuranMediaService]
 * to control media playback and reflects the service's current status.
 *
 * The widget's state, defined by [PlayerWidgetState], is updated via the [updateGlanceWidgets]
 * static method, which is called by the service when media information changes. Clicking on the
 * widget opens the [MainActivity].
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see GlanceAppWidget
 * @see QuranMediaService
 * @see PlayerWidgetState
 */
class PlayerWidget : GlanceAppWidget() {

    /**
     * Companion object for the [PlayerWidget] class.
     *
     * This object holds utility functions and properties that are related to the [PlayerWidget]
     * but are not specific to an instance of it. It's primarily used for managing and
     * updating widget states across all instances of the [PlayerWidget].
     *
     * Key functionalities include:
     * - A private extension property to safely extract the [appWidgetId] from a [GlanceId].
     * - The [updateGlanceWidgets] function, which receives a [ServiceStatus] update from the
     *   [QuranMediaService] and propagates the new state to all active [PlayerWidget]
     *   instances. This ensures the UI of all widgets remains synchronized with the
     *   media player's state.
     *
     * Usage of this companion object centralizes the logic for widget updates, making it
     * easier to manage and debug interactions between the background media service and the
     * home screen widgets.
     */
    companion object {

        /**
         * A private extension property on [GlanceId] to extract the underlying `appWidgetId`.
         *
         * This is a workaround using reflection to access the private `appWidgetId` field
         * within the [GlanceId] class. This is necessary because the `appWidgetId` is not
         * publicly exposed, but it's useful for logging and debugging purposes to identify
         * which specific widget instance is being updated or composed.
         *
         * **Warning:** This implementation relies on the internal structure of the `GlanceId`
         * class and may break in future versions of the Glance library if the field name
         * or class structure changes. Use with caution.
         *
         * @return [Int] The integer ID of the AppWidget associated with this [GlanceId].
         */
        private val GlanceId.appWidgetId get() = this::class.java.getDeclaredField("appWidgetId").apply { isAccessible = true }.getInt(this)

        /**
         * Updates the state of all active [PlayerWidget] instances.
         *
         * This function is called by the [QuranMediaService] whenever there is a change
         * in the media playback status (e.g., new surah, play/pause state change). It receives
         * the new [ServiceStatus], converts it into a [PlayerWidgetState], and then updates
         * each active widget instance.
         *
         * The function performs the following steps:
         * 1. Retrieves all `GlanceId`s for the `PlayerWidget` class.
         * 2. Ignores the update if there are no active widgets or if the status is not `ServiceStatus.MediaInfo`.
         * 3. Creates a new `PlayerWidgetState` from the provided `status`.
         * 4. Iterates through each `glanceId` and updates its state in the datastore, but only if the new state is different from the old one.
         * 5. If any widget's state was changed, it triggers a UI update for all widgets by calling [GlanceAppWidget.updateAll].
         *
         * @param context [Context] The application [Context], used to get the [GlanceAppWidgetManager].
         * @param status [ServiceStatus] The latest [ServiceStatus] from the media service. Only [ServiceStatus.MediaInfo] will trigger a state update.
         */
        suspend fun updateGlanceWidgets(context: Context, status: ServiceStatus) {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(PlayerWidget::class.java)
            val appWidgetIds = glanceIds.map { it.appWidgetId }.toTypedArray().contentToString()
            var anyStateChanged = false

            if (glanceIds.isEmpty()) return
            if (status !is ServiceStatus.MediaInfo) return

            val newState = PlayerWidgetState(reciter = status.reciter, moshaf = status.moshaf, surah = status.surah, status = status)

            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PlayerWidgetStateDefinition, glanceId) { oldState ->
                    val currentStateChanged = oldState != newState
                    anyStateChanged = anyStateChanged || currentStateChanged

                    when {
                        currentStateChanged -> newState
                        else                -> oldState
                    }
                }
            }

            if (!anyStateChanged) return /* Timber.debug("No state changes! Skipping UI update!") */
            PlayerWidget().updateAll(context)

            Timber.debug("Updated $appWidgetIds glance widgets' states to ${newState.status?.javaClass?.simpleName}")
        }
    }

    /**
     * Defines the state management for this widget.
     *
     * This property delegates state handling to [PlayerWidgetStateDefinition], which specifies
     * how the widget's state ([PlayerWidgetState]) is stored and retrieved. By overriding this,
     * we tell the Glance framework to use our custom state definition, enabling the widget to
     * persist and access its data, such as the current surah, reciter, and playback status.
     *
     * @return [PlayerWidgetStateDefinition] The state definition for this widget.
     *
     * @see PlayerWidgetStateDefinition
     * @see PlayerWidgetState
     */
    override val stateDefinition = PlayerWidgetStateDefinition

    /**
     * Defines the UI content for a single instance of the [PlayerWidget].
     *
     * This method is the entry point for Glance to render the widget's UI. It is called by the
     * framework whenever the widget needs to be drawn or updated.
     *
     * Inside this function, it performs the following steps:
     * 1. Calls [provideContent] to start the composition process.
     * 2. Retrieves the current [PlayerWidgetState] using [currentState]. This state contains
     *    all the necessary data like the current `reciter`, `surah`, and playback `status`.
     * 3. Prepares the necessary bitmaps for the surah image and its blurred background, falling back
     *    to default images if the current surah is not set.
     * 4. Logs the composition event and the current status for debugging purposes.
     * 5. Wraps the main UI in [WidgetTheme] to apply consistent styling.
     * 6. Delegates the actual UI construction to the [Content] composable function, passing in the
     *    retrieved state and bitmaps.
     *
     * @param context [Context] The [Context] in which the widget is being hosted.
     * @param id [GlanceId] The unique [GlanceId] for this specific widget instance.
     */
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state = currentState<PlayerWidgetState>()
            val reciter = state.reciter
            val surah = state.surah
            val status = state.status

            val surahBitmap = surah?.let { surahImages[it.id].bitmap } ?: defaultSurahBitmap
            val surahBlurredBitmap = surah?.let { surahImages[it.id].blurredBitmap } ?: defaultSurahBlurredBitmap

            Timber.debug("#${id.appWidgetId} Composing glance widget...")
            Timber.debug("#${id.appWidgetId} Status: $status")

            WidgetTheme {
                Content(
                        reciter = reciter,
                        surah = surah,
                        surahBitmap = surahBitmap,
                        surahBlurredBitmap = surahBlurredBitmap,
                        status = status
                )
            }
        }
    }

    /**
     * The main composable function that defines the layout and content of the player widget.
     *
     * This function builds the UI by arranging several components:
     * - A background image derived from a blurred version of the surah's artwork.
     * - An overlay containing the surah and reciter information.
     * - Media control buttons (play/pause, next, previous).
     * - The primary, non-blurred surah artwork displayed on the side.
     *
     * Clicking anywhere on the widget (except the control buttons) opens the main application.
     *
     * @param reciter [Reciter?][Reciter] The currently selected [Reciter], or null if none is selected.
     * @param surah [Surah?][Surah] The currently playing [Surah], or null if none is selected.
     * @param surahBitmap [Bitmap] The primary [Bitmap] artwork for the current surah.
     * @param surahBlurredBitmap [Bitmap] A blurred version of the surah artwork for the background.
     * @param status [ServiceStatus?][ServiceStatus] The current [ServiceStatus] of the media player, which determines the play/pause icon.
     */
    @Composable
    private fun Content(
            reciter: Reciter? = null,
            surah: Surah? = null,
            surahBitmap: Bitmap,
            surahBlurredBitmap: Bitmap,
            status: ServiceStatus? = null
    ) {
        val context = LocalContext.current
        val contentForegroundColor = Color.White
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        SurahCard(modifier = GlanceModifier.clickable(onClick = actionStartActivity(activityIntent))) {
            Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .background(Color.Black)
            ) {
                Image(
                        provider = ImageProvider(surahBlurredBitmap),
                        contentDescription = "Surah Image",
                        modifier = GlanceModifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                )

                Column(
                        modifier = GlanceModifier
                            .fillMaxHeight()
                            .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    SurahInfo(contentForegroundColor, surah, context, reciter)

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    PlayerControls(contentForegroundColor, status)

                    Spacer(modifier = GlanceModifier.defaultWeight())
                }
            }

            Image(
                    provider = ImageProvider(surahBitmap),
                    contentDescription = "Surah Image",
                    modifier = GlanceModifier
                        .fillMaxHeight()
                        .width(100.dp),
                    contentScale = ContentScale.FillBounds
            )
        }
    }

    /**
     * A composable function that creates the main card-like container for the widget's UI.
     *
     * This function wraps its content in a [Column] with rounded corners to give it a card
     * appearance. It then arranges the provided `content` within a [Row], aligning it to the
     * end (right side). This structure is designed to hold the two main sections of the widget:
     * the info/controls part and the surah artwork part, side-by-side.
     *
     * @param modifier [GlanceModifier?][GlanceModifier] A [GlanceModifier] to be applied to the root [Column] of the card.
     *        Defaults to an empty modifier.
     * @param content [@Composable RowScope.() -> Unit][content] A composable lambda that defines the content to be placed inside the card.
     *        This content is placed within a [RowScope], allowing for horizontal arrangement.
     */
    @Composable
    private fun SurahCard(
            modifier: GlanceModifier = GlanceModifier,
            content: @Composable RowScope.() -> Unit
    ) {
        Column(
                modifier = modifier
                    .fillMaxSize()
                    .cornerRadius(20.dp)
        ) {
            Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.End,
                    content = content
            )
        }
    }

    /**
     * A composable that displays the surah and reciter information.
     *
     * This function arranges an icon and two lines of text horizontally. It shows the
     * name of the current surah and the name of the reciter. If the surah or reciter
     * is not available, it displays placeholder text from string resources.
     *
     * @param contentForegroundColor [Color] The color to be used for the icon and text.
     * @param surah [Surah?][Surah] The currently playing [Surah], or null if none.
     * @param context [Context] The application [Context], used to retrieve placeholder strings.
     * @param reciter [Reciter?][Reciter] The currently selected [Reciter], or null if none.
     */
    @Composable
    private fun SurahInfo(
            contentForegroundColor: Color,
            surah: Surah?,
            context: Context,
            reciter: Reciter?
    ) = Row {
        Image(
                ImageProvider(Rs.drawable.quran_icon_monochrome_white_64),
                contentDescription = "Surah Image",
                modifier = GlanceModifier
                    .size(45.dp)
                    .padding(10.dp),
                colorFilter = ColorFilter.tint(ColorProvider(contentForegroundColor, contentForegroundColor))
        )

        Column(modifier = GlanceModifier.padding(horizontal = 10.dp)) {
            Text(
                    modifier = GlanceModifier.fillMaxWidth(),
                    text = surah?.name ?: context.getString(R.string.surah_name),
                    maxLines = 1,
                    style = TextStyle(
                            fontSize = 20.sp,
                            fontStyle = FontStyle.Normal,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            color = ColorProvider(contentForegroundColor, contentForegroundColor)
                    )
            )

            Text(
                    modifier = GlanceModifier.fillMaxWidth(),
                    text = reciter?.name ?: context.getString(R.string.reciter_name),
                    maxLines = 1,
                    style = TextStyle(
                            fontSize = 10.sp,
                            fontStyle = FontStyle.Normal,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.End,
                            color = ColorProvider(contentForegroundColor, contentForegroundColor)
                    ),
            )
        }
    }

    /**
     * A composable that displays the media control buttons (previous, play/pause, next).
     *
     * This function arranges three [ImageButton]s in a horizontal [Row], centered within
     * their container. The central button's icon changes between play and pause based on the
     * provided [status]. Each button is configured with an `onClick` action that runs a
     * corresponding [ActionCallback]: [SkipToPreviousAction], [ToggleMediaAction], [SkipToNextAction]
     * to send a command to the [QuranMediaService].
     *
     * @param status [ServiceStatus?][ServiceStatus] The current media playback status. This is used to determine
     *        whether to display a play or pause icon. If the status is [ServiceStatus.Playing],
     *        the pause icon is shown; otherwise, the play icon is shown.
     */
    @Composable
    private fun PlayerControls(
            contentForegroundColor: Color,
            status: ServiceStatus?
    ) = Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
    ) {
        val buttonSize = 30.dp
        val buttonSpacing = 25.dp

        ImageButton(
                modifier = GlanceModifier.size(buttonSize),
                imageProvider = ImageProvider(Rs.drawable.skip_previous_24px),
                contentDescription = "Skip To Previous",
                contentForegroundColor = contentForegroundColor,
                onClick = actionRunCallback<SkipToPreviousAction>()
        )

        Spacer(modifier = GlanceModifier.width(buttonSpacing))

        ImageButton(
                modifier = GlanceModifier.size(buttonSize),
                imageProvider = ImageProvider(
                        when (status) {
                            is ServiceStatus.Playing -> Rs.drawable.pause_circle_24px
                            else                     -> Rs.drawable.play_circle_24px
                        }
                ),
                contentDescription = "Toggle Media",
                contentForegroundColor = contentForegroundColor,
                onClick = actionRunCallback<ToggleMediaAction>()
        )

        Spacer(modifier = GlanceModifier.width(buttonSpacing))

        ImageButton(
                modifier = GlanceModifier.size(buttonSize),
                imageProvider = ImageProvider(Rs.drawable.skip_next_24px),
                contentDescription = "Skip To Previous",
                contentForegroundColor = contentForegroundColor,
                onClick = actionRunCallback<SkipToNextAction>()
        )
    }

    /**
     * A composable that displays an image that acts as a button.
     *
     * This function wraps a Glance [Image] inside a [Button] composable, making the image
     * clickable. It applies a tint to the image, which is useful for monochrome icons.
     * The `onClick` action is handled by the parent [Button].
     *
     * @param modifier [GlanceModifier] The modifier to be applied to both the wrapping [Button] and the [Image].
     * @param imageProvider [ImageProvider] The source for the image to be displayed.
     * @param contentDescription [String?][String] A description of the image for accessibility. Defaults to `null`.
     * @param contentForegroundColor [Color] The color to tint the image with. Defaults to [Color.White].
     * @param onClick [Action] The action to be performed when the button is clicked. Defaults to a no-op action.
     *
     * @see Button
     * @see Image
     */
    @Composable
    private fun ImageButton(
            modifier: GlanceModifier,
            imageProvider: ImageProvider,
            contentDescription: String? = null,
            contentForegroundColor: Color = Color.White,
            onClick: Action = actionRunCallback<ActionCallback>()
    ) = Button(
            modifier = modifier,
            onClick = onClick
    ) {
        Image(
                provider = imageProvider,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(ColorProvider(contentForegroundColor, contentForegroundColor))
        )
    }

    /**
     * A basic, transparent, clickable composable that acts as a button.
     *
     * This function wraps its `content` in a [Box] with a transparent background.
     * It applies a [GlanceModifier.clickable] to handle user interactions via the
     * provided [onClick] action. The content is centered within the box.
     *
     * This composable is a building block for creating custom buttons, like [ImageButton],
     * where the visual appearance is defined entirely by the `content` lambda.
     *
     * @param modifier [GlanceModifier] to be applied to the button's root [Box]. Defaults to an empty modifier.
     * @param onClick [Action] to be executed when the button is clicked. Defaults to a no-op action.
     * @param content [@Composable () -> Unit][content] The composable content to be displayed inside the button.
     */
    @Composable
    private fun Button(
            modifier: GlanceModifier = GlanceModifier,
            onClick: Action = actionRunCallback<ActionCallback>(),
            content: @Composable () -> Unit
    ) = Box(
            modifier = modifier
                .background(Color.Transparent)
                .cornerRadius(50.dp)
                .clickable(onClick),
            contentAlignment = Alignment.Center,
            content = content
    )

    /**
     * A preview of the content of the app widget.
     *
     * This function is called when the app widget is displayed and is responsible for providing the content of the app widget.
     *
     * @see [Content]
     */
    @Composable
    @Suppress("unused")
    @Preview(widthDp = 300, heightDp = 90)
    @OptIn(ExperimentalGlancePreviewApi::class)
    private fun ContentPreview() {
        val context = LocalContext.current
        val reciter = sampleReciters.random()
        val surah = sampleSurahs.random()

        val surahBitmap = (AppCompatResources.getDrawable(context, surah.surahDrawableId) as BitmapDrawable).bitmap
        val surahBlurredBitmap = (AppCompatResources.getDrawable(context, Rs.drawable.surah_name_blurred) as BitmapDrawable).bitmap

        val status = ServiceStatus.Playing(
                reciter = reciter,
                moshaf = reciter.moshafList.first(),
                surah = surah,
                durationMs = 0L,
                currentPositionMs = 0L,
                bufferedPositionMs = 0L,
        )

        Box(modifier = GlanceModifier.fillMaxSize().background(Color.Black)) {
            Content(
                    reciter = reciter,
                    surah = surah,
                    surahBitmap = surahBitmap,
                    surahBlurredBitmap = surahBlurredBitmap,
                    status = status
            )
        }
    }
}

/**
 * An [ActionCallback] that handles the "skip to previous" action from the player widget.
 *
 * When triggered, this callback creates an [Intent] to start the [QuranMediaService] and
 * sets the action to [QuranMediaService.Actions.ACTION_SKIP_TO_PREVIOUS]. This instructs
 * the media service to move to the previous track in the playlist.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see ActionCallback
 * @see QuranMediaService
 */
class SkipToPreviousAction : ActionCallback {

    /**
     * Handles the action of skipping to the next track.
     *
     * This function is invoked when the "skip to next" button in the widget is clicked.
     * It creates an [Intent] directed at the [QuranMediaService], setting the action
     * to [QuranMediaService.Actions.ACTION_SKIP_TO_NEXT]. It then starts the service
     * with this intent, instructing it to advance to the next surah in the playlist.
     *
     * @param context [Context] The [Context] from which the action is triggered.
     * @param glanceId [GlanceId] The [GlanceId] of the widget instance that triggered the action.
     * @param parameters [ActionParameters] A map of [ActionParameters] associated with the action, if any.
     */
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Timber.debug("SkipToPreviousAction")

        Intent(context, QuranMediaService::class.java).run {
            action = QuranMediaService.Actions.ACTION_SKIP_TO_PREVIOUS.name
            context.startService(this)
        }
    }
}

/**
 * An [ActionCallback] that handles the "toggle play/pause" action from the player widget.
 *
 * When triggered, this callback creates an [Intent] to start the [QuranMediaService] and
 * sets the action to [QuranMediaService.Actions.ACTION_TOGGLE_PLAY_PAUSE]. This instructs
 * the media service to either play or pause the current media, depending on its current state.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see ActionCallback
 * @see QuranMediaService
 */
class ToggleMediaAction : ActionCallback {

    /**
     * Handles the action of skipping to the next track.
     *
     * This function is invoked when the "skip to next" button in the widget is clicked.
     * It creates an [Intent] directed at the [QuranMediaService], setting the action
     * to [QuranMediaService.Actions.ACTION_SKIP_TO_NEXT]. It then starts the service
     * with this intent, instructing it to advance to the next surah in the playlist.
     *
     * @param context [Context] The [Context] from which the action is triggered.
     * @param glanceId [GlanceId] The [GlanceId] of the widget instance that triggered the action.
     * @param parameters [ActionParameters] A map of [ActionParameters] associated with the action, if any.
     */
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Timber.debug("ToggleMediaAction")

        Intent(context, QuranMediaService::class.java).apply {
            action = QuranMediaService.Actions.ACTION_TOGGLE_PLAY_PAUSE.name
            context.startService(this)
        }
    }
}

/**
 * An [ActionCallback] that handles the "skip to next" action from the player widget.
 *
 * When triggered, this callback creates an [Intent] to start the [QuranMediaService] and
 * sets the action to [QuranMediaService.Actions.ACTION_SKIP_TO_NEXT]. This instructs
 * the media service to move to the next track in the playlist.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see ActionCallback
 * @see QuranMediaService
 */
class SkipToNextAction : ActionCallback {

    /**
     * Handles the action of toggling the media playback state (play/pause).
     *
     * This function is invoked when the play/pause button in the widget is clicked.
     * It creates an [Intent] directed at the [QuranMediaService], setting the action
     * to [QuranMediaService.Actions.ACTION_TOGGLE_PLAY_PAUSE]. It then starts the service
     * with this intent, instructing it to either play or pause the current media.
     *
     * @param context [Context] The [Context] from which the action is triggered.
     * @param glanceId [GlanceId] The [GlanceId] of the widget instance that triggered the action.
     * @param parameters [ActionParameters] A map of [ActionParameters] associated with the action, if any.
     */
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Timber.debug("SkipToNextAction")

        Intent(context, QuranMediaService::class.java).apply {
            action = QuranMediaService.Actions.ACTION_SKIP_TO_NEXT.name
            context.startService(this)
        }
    }
}

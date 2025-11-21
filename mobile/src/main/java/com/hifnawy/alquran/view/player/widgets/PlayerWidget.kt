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
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
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
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.google.gson.GsonBuilder
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.QuranApplication
import com.hifnawy.alquran.shared.domain.QuranMediaService
import com.hifnawy.alquran.shared.domain.ServiceStatus
import com.hifnawy.alquran.shared.domain.ServiceStatusObserver
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.shared.utils.DrawableResUtil.defaultSurahDrawableId
import com.hifnawy.alquran.shared.utils.DrawableResUtil.surahDrawableId
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.shared.utils.SerializableExt.Companion.toJsonString
import com.hifnawy.alquran.utils.RuntimeTypeAdapterFactoryEx.registerSealedSubtypes
import com.hifnawy.alquran.utils.RuntimeTypeAdapterFactoryEx.registeredSubtypes
import com.hifnawy.alquran.utils.RuntimeTypeAdapterFactoryEx.registeredTypeFieldName
import com.hifnawy.alquran.view.activities.MainActivity
import com.hifnawy.alquran.view.theme.WidgetTheme
import com.hoko.blur.HokoBlur
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import com.hifnawy.alquran.shared.R as Rs

class PlayerWidget : GlanceAppWidget(), ServiceStatusObserver {

    private companion object PlayerWidgetState {

        val RECITER = stringPreferencesKey("reciter")
        val SURAH = stringPreferencesKey("surah")
        val STATUS = stringPreferencesKey("status")
    }

    private val serviceStatusAdapter = ServiceStatus::class.registerSealedSubtypes

    private val gson = GsonBuilder().registerTypeAdapterFactory(serviceStatusAdapter).create()

    private lateinit var quranApplication: QuranApplication

    private var currentReciter: Reciter? = null
    private var currentSurah: Surah? = null

    override fun onServiceStatusUpdated(status: ServiceStatus) {
        Timber.debug("status: $status")

        when (status) {
            is ServiceStatus.Paused,
            is ServiceStatus.Playing -> status.run {
                currentReciter = reciter
                currentSurah = surah
            }

            else                     -> Unit
        }

        val reciter = currentReciter ?: return
        val surah = currentSurah ?: return
        updateGlanceWidgets(reciter = reciter, surah = surah, status = status)
    }

    /**
     * Provides the content of the widget.
     *
     * This function is called when the widget is displayed and is responsible for providing the content of the widget.
     *
     * @param context [Context] The context of the widget.
     * @param id [GlanceId] The id of the widget.
     *
     * @see [provideContent]
     * @see [Content]
     */
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Timber.debug("${serviceStatusAdapter.registeredTypeFieldName}:")
        serviceStatusAdapter.registeredSubtypes.lines().forEach { line -> Timber.debug(line) }

        quranApplication = context.applicationContext as QuranApplication
        if (this !in quranApplication.quranServiceObservers) quranApplication.quranServiceObservers.add(this)

        provideContent {
            WidgetTheme {
                val prefs = currentState<Preferences>()
                val reciter = prefs[RECITER]?.let { gson.fromJson(it, Reciter::class.java) }
                val surah = prefs[SURAH]?.let { gson.fromJson(it, Surah::class.java) }
                val status = prefs[STATUS]?.let { gson.fromJson(it, ServiceStatus::class.java) }

                val surahDrawableId = surah?.surahDrawableId ?: defaultSurahDrawableId
                val surahDrawable = AppCompatResources.getDrawable(context, surahDrawableId)
                val surahBitmap = (surahDrawable as BitmapDrawable).bitmap
                val surahBlurredBitmap = HokoBlur.with(context)
                    .scheme(HokoBlur.SCHEME_NATIVE) // different implementation, RenderScript、OpenGL、Native(default) and Java
                    .mode(HokoBlur.MODE_GAUSSIAN) // blur algorithms，Gaussian、Stack(default) and Box
                    .radius(5) // blur radius，max=25，default=5
                    .sampleFactor(2.0f)
                    .processor()
                    .blur(surahBitmap)

                Content(reciter, surah, surahBitmap, surahBlurredBitmap, status)
            }
        }
    }

    @Composable
    private fun Content(reciter: Reciter? = null, surah: Surah? = null, surahBitmap: Bitmap, surahBlurredBitmap: Bitmap, status: ServiceStatus? = null) {
        val context = LocalContext.current
        val contentForegroundColor = Color.White

        Timber.debug("composing...")

        SurahCard(
                modifier = GlanceModifier
                    .clickable(
                            onClick = actionStartActivity(
                                    Intent(context, MainActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                    }
                            )
                    )
        ) {
            Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .background(Color.Black)
            ) {
                Image(
                        ImageProvider(surahBlurredBitmap),
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
                }
            }

            Image(
                    ImageProvider(surahBitmap),
                    contentDescription = "Surah Image",
                    modifier = GlanceModifier
                        .fillMaxHeight()
                        .width(100.dp),
                    contentScale = ContentScale.FillBounds
            )
        }
    }

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
                    .size(32.dp)
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

    @Composable
    private fun Button(
            modifier: GlanceModifier = GlanceModifier,
            onClick: Action = actionRunCallback<ActionCallback>(),
            content: @Composable () -> Unit
    ) = Box(
            modifier = modifier
                .background(Color.Transparent)
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
        val surahDrawable = AppCompatResources.getDrawable(context, Rs.drawable.surah_name)
        val surahBlurredDrawable = AppCompatResources.getDrawable(context, Rs.drawable.surah_name_blurred)

        val surahBitmap = (surahDrawable as BitmapDrawable).bitmap
        val surahBlurredBitmap = (surahBlurredDrawable as BitmapDrawable).bitmap

        Box(modifier = GlanceModifier.fillMaxSize().background(Color.Black)) {
            Content(surahBitmap = surahBitmap, surahBlurredBitmap = surahBlurredBitmap)
        }
    }

    private fun updateGlanceWidgets(reciter: Reciter, surah: Surah, status: ServiceStatus) {
        CoroutineScope(Dispatchers.Default).launch {
            val manager = GlanceAppWidgetManager(quranApplication)
            val glanceIds = manager.getGlanceIds(PlayerWidget::class.java)

            glanceIds.forEach { glanceId ->
                updateAppWidgetState(quranApplication, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        val reciterJson = reciter.toJsonString(gson)
                        val surahJson = surah.toJsonString(gson)
                        val statusJson = status.toJsonString(gson, ServiceStatus::class.java)

                        set(RECITER, reciterJson)
                        set(SURAH, surahJson)
                        set(STATUS, statusJson)
                    }
                }

                update(quranApplication, glanceId)
            }
        }
    }
}

internal class SkipToPreviousAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Timber.debug("SkipToPreviousAction")

        Intent(context, QuranMediaService::class.java).run {
            action = QuranMediaService.Actions.ACTION_SKIP_TO_PREVIOUS.name

            context.startService(this)
        }
    }
}

internal class ToggleMediaAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Timber.debug("ToggleMediaAction")

        Intent(context, QuranMediaService::class.java).apply {
            action = QuranMediaService.Actions.ACTION_TOGGLE_PLAY_PAUSE.name

            context.startService(this)
        }
    }
}

internal class SkipToNextAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Timber.debug("SkipToNextAction")

        Intent(context, QuranMediaService::class.java).apply {
            action = QuranMediaService.Actions.ACTION_SKIP_TO_NEXT.name

            context.startService(this)
        }
    }
}

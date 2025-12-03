package com.hifnawy.alquran.utils

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import org.jsoup.Jsoup

/**
 * Provides a comprehensive and centralized suite of utilities for handling
 * HTML content within an Android application, specifically tailored for integration
 * with Jetpack Compose. This object is designed to be the go-to resource for
 * any tasks involving the parsing, manipulation, and display of HTML strings.
 *
 * It serves as a bridge between the classic Android text styling system, which
 * heavily relies on [Spanned] objects from HTML, and the modern, declarative
 * UI paradigm of Jetpack Compose, which uses [AnnotatedString].
 *
 * Key functionalities encapsulated within this object include:
 *
 * - **HTML Body Extraction**: A convenient extension property is provided to
 *   effortlessly extract the content from within the `<body>` tags of a full
 *   HTML document. This is particularly useful for stripping away boilerplate
 *   HTML structure (`<head>`, `<meta>`, etc.) to isolate the primary displayable
 *   content. It simplifies the process of consuming HTML from sources where only
 *   the core message is relevant.
 *
 * - **Mutable HTML Content**: A nested class, [MutableHtml], offers a powerful
 *   and fluent interface for performing in-place modifications to an HTML string.
 *   This allows for dynamic styling adjustments before the content is rendered.
 *   For example, you can programmatically change the color or style of specific
 *   HTML tags to match your application's theme or to highlight certain elements,
 *   all through a clean and chainable API.
 *
 * - **Spanned to AnnotatedString Conversion**: At the heart of its integration
 *   with Compose is a sophisticated conversion utility. It transforms a standard
 *   Android [Spanned] object—rich with various style spans like color, font weight,
 *   underlines, and hyperlinks—into a fully equivalent Jetpack Compose
 *   [AnnotatedString]. This ensures that all the rich formatting defined in the
 *   original HTML is faithfully preserved and rendered correctly in a Compose [Text]
 *   composable.
 *
 * This utility object abstracts away the complexities of HTML parsing and style
 * application, making it easier to work with HTML content in a Compose environment.
 *
 * @author AbdAlMoniem AlHifnawy
 */
object HTML {

    /**
     * Extracts the content of the `<body>` element from an HTML string.
     *
     * This property is designed to quickly and efficiently isolate the content found within
     * the `<body>...</body>` tags of a given HTML document string. It is particularly useful
     * for scenarios where only the primary visible content of an HTML page is needed,
     * discarding the `<head>`, metadata, and other structural elements.
     *
     * The property robustly handles `<body>` tags that may contain various attributes, such as
     * `class`, `id`, or `style`, ensuring that only the inner content is returned. It is also
     * capable of processing multi-line HTML documents, correctly identifying the start and end
     * of the body section across line breaks.
     *
     * After extraction, any leading or trailing whitespace (including newlines) from the
     * resulting content is automatically trimmed to provide a clean string.
     *
     * ### Use Case Example:
     * If you have a full HTML document string like:
     * ```html
     * <html>
     *   <head><title>My Page</title></head>
     *   <body class="main">
     *     <h1>Welcome</h1>
     *     <p>This is the main content.</p>
     *   </body>
     * </html>
     * ```
     * Getting this property with the above string would return:
     * ```html
     * <h1>Welcome</h1>
     * <p>This is the main content.</p>
     * ```
     *
     * ### Return Value Behavior:
     * - On success, it returns the trimmed inner content of the `<body>` tag as a `String`.
     * - If the input string does not contain a `<body>...</body>` block, the property
     *   will safely return `null`. This prevents [NullPointerException]s and allows for
     *   easy checking of whether the extraction was successful.
     *
     * @receiver [String] The HTML string from which to extract the body content.
     *
     * @return [String] The trimmed inner content of the `<body>` tag, or `null` if no `<body>` tag is found.
     */
    val String.htmlBody get() = Regex("<body.*?>(.*?)</body>", RegexOption.DOT_MATCHES_ALL).find(this)?.groups?.first()?.value?.trim()

    /**
     * A mutable wrapper for an HTML string, providing utilities for content manipulation and
     * conversion to a Jetpack Compose [AnnotatedString].
     *
     * This class encapsulates an HTML string and offers methods to modify it, such as
     * changing the color of specific tags. It also includes an extension property to
     * convert a standard Android [Spanned] object (often derived from HTML) into a
     * fully styled [AnnotatedString] for use in Compose UIs.
     *
     * Example Usage:
     * ```kotlin
     * val mutableHtml = HTML.MutableHtml("<h1>Title</h1><p>Some text.</p>")
     *     .setHtmlTagColor("h1", Color.Red)
     *
     * // To display in a Composable
     * val spanned = Html.fromHtml(mutableHtml.content, Html.FROM_HTML_OPTION_USE_CSS_COLORS)
     * with(mutableHtml) {
     *     Text(text = spanned.annotatedString)
     * }
     * ```
     *
     * @property content [String] The raw HTML string. This property is mutable and can be updated directly
     *   or via methods like [setHtmlTagColor].
     *
     * @author AbdAlMoniem AlHifnawy
     */
    class MutableHtml(var content: String) {

        /**
         * Converts a [Spanned] object into a Jetpack Compose [AnnotatedString].
         *
         * This composable extension property processes a [Spanned] object, which is typically
         * generated from HTML, and translates its various style spans (like [ForegroundColorSpan],
         * [StyleSpan], [URLSpan], etc.) into the corresponding [SpanStyle] and annotations
         * for an [AnnotatedString]. This allows for rendering styled text from HTML within
         * Jetpack Compose UI.
         *
         * The conversion handles:
         * - **Color**: [ForegroundColorSpan] is converted to `SpanStyle(color = ...)`.
         * - **Size**: [RelativeSizeSpan] is converted to `SpanStyle(fontSize = ...)`.
         * - **Style**: [StyleSpan] (bold, italic, bold-italic) is converted to [SpanStyle]
         *   with appropriate [fontWeight][SpanStyle.fontWeight] and [fontStyle][SpanStyle.fontStyle].
         * - **Decoration**: [UnderlineSpan] and [StrikethroughSpan] are converted to [SpanStyle]
         *   with the corresponding [textDecoration][SpanStyle.textDecoration].
         * - **Links**: [URLSpan] is converted to a [URL][URLSpan.url] string annotation and styled with
         *   the primary theme color and an underline.
         *
         * @return [AnnotatedString] The resulting [AnnotatedString] with all styles and
         *   annotations applied.
         */
        val Spanned.annotatedString: AnnotatedString
            @Composable get() {
                val builder = AnnotatedString.Builder(this.toString())
                val spans = getSpans(0, length, Any::class.java)

                spans.forEach { span ->
                    val start = getSpanStart(span)
                    val end = getSpanEnd(span)

                    with(builder) {
                        when (span) {
                            is ForegroundColorSpan -> addStyle(style = SpanStyle(color = Color(span.foregroundColor)), start = start, end = end)
                            is RelativeSizeSpan    -> addStyle(style = SpanStyle(fontSize = TextUnit(span.sizeChange, TextUnitType.Em)), start = start, end = end)

                            is StyleSpan           -> when (span.style) {
                                Typeface.BOLD        -> addStyle(style = SpanStyle(fontWeight = FontWeight.Bold), start = start, end = end)
                                Typeface.ITALIC      -> addStyle(style = SpanStyle(fontStyle = FontStyle.Italic), start = start, end = end)
                                Typeface.BOLD_ITALIC -> addStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), start = start, end = end)
                            }

                            is UnderlineSpan       -> addStyle(style = SpanStyle(textDecoration = TextDecoration.Underline), start = start, end = end)
                            is StrikethroughSpan   -> addStyle(style = SpanStyle(textDecoration = TextDecoration.LineThrough), start = start, end = end)

                            is URLSpan             -> {
                                addStringAnnotation(tag = "URL", annotation = span.url, start = start, end = end)
                                addStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline), start = start, end = end)
                            }
                        }
                    }
                }

                return builder.toAnnotatedString()
            }

        /**
         * Modifies the HTML content by wrapping the text of all elements with a specific tag
         * in a `<font>` tag to set their color and make them bold.
         *
         * This method uses [Jsoup] to parse the current HTML `content`. It finds all
         * elements matching the provided [tagName], gets their text content, and then replaces
         * their inner HTML with a new structure: `<font color='...'><b>...</b></font>`.
         * The color is derived from the given Jetpack Compose [Color] parameter.
         *
         * After modification, the internal `content` of the [MutableHtml] instance is updated
         * with the new HTML.
         *
         * @param tagName [String] The name of the HTML tag to find and modify (e.g., `h1`, `p`).
         * @param color [Color] The Jetpack Compose [Color] to apply to the text of the found tags.
         *
         * @return [MutableHtml] The current instance of [MutableHtml] to allow for method chaining.
         */
        fun setHtmlTagColor(tagName: String, color: Color) = Jsoup.parse(content).run {
            val tagColor = "#${color.toArgb().toHexString().substring(2)}"
            select(tagName).forEach { tag -> tag.html("<font color='${tagColor}'><b>${tag.text()}</b></font>") }
            content = html()

            this@MutableHtml
        }
    }
}

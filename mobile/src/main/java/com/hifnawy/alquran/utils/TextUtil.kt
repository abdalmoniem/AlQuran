package com.hifnawy.alquran.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * Util class for all things related to [String Text][String].
 *
 * @author AbdElMoniem ElHifnawy
 */
object TextUtil {

    /**
     * Highlights matching text in a string.
     *
     * @param fullText [String] The full text to be processed.
     * @param query [String] The query string to search for.
     * @param highlightColor [Color] The color to use for highlighting matching text.
     * @param defaultColor [Color] The color to use for default text.
     *
     * @return [AnnotatedString] An AnnotatedString with highlighted matching text.
     */
    fun highlightMatchingText(fullText: String, query: String, highlightColor: Color, defaultColor: Color): AnnotatedString {
        if (query.isBlank()) return AnnotatedString(fullText)

        val queryChars = query.trim().lowercase().toSet()
        val highlightStyle = SpanStyle(color = highlightColor)
        val defaultStyle = SpanStyle(color = defaultColor)

        return buildAnnotatedString {
            fullText.forEach { char ->
                val isMatch = queryChars.contains(char.lowercaseChar())

                when {
                    isMatch -> withStyle(style = highlightStyle) { append(char) }
                    else    -> withStyle(style = defaultStyle) { append(char) }
                }
            }
        }
    }
}

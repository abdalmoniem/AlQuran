package com.hifnawy.alquran.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hifnawy.alquran.R

/**
 * A global [FocusManager] instance used to control focus within the composables of this file.
 * It's primarily used to clear focus from the search field, for example, when the back button is pressed
 * or a search action is performed. It is initialized in the composable where `LocalFocusManager.current`
 * is available and needs to be used across different parts of the UI, such as in a [BackHandler].
 *
 * @return [FocusManager] The global [FocusManager] instance.
 *
 * @see FocusManager
 */
var focusManager: FocusManager? = null

/**
 * A composable that displays a search bar. It can either show a skeleton loader or a fully functional
 * text field for search queries.
 *
 * This component conditionally renders a [Spacer] with a shimmering background (skeleton loader) when
 * [isSkeleton] is `true` and a [brush] is provided. Otherwise, it renders a [SearchTextField] where users
 * can input their search queries.
 *
 * @param isSkeleton [Boolean] A boolean flag to determine whether to show the skeleton loader view.
 * If `true`, a [Spacer] with the specified [brush] is shown. If `false`,
 * the [SearchTextField] is shown.
 * @param brush [Brush?][Brush] The [Brush] to be used for the background of the skeleton loader. It is ignored if
 * [isSkeleton] is `false`.
 * @param query [String] The current text to display in the search text field.
 * @param placeholder [String] The placeholder text to display when the search text field is empty.
 * @param label [String] The label text for the search text field.
 * @param onQueryChange [(query: String) -> Unit][onQueryChange] A callback that is invoked when the user
 * types in the search text field.The new query string is passed as an argument.
 * @param onClearQuery [() -> Unit][onClearQuery] A callback that is invoked when the user clicks the clear
 *  icon in the search
 * text field.
 */
@Composable
fun SearchBar(
        isSkeleton: Boolean,
        brush: Brush?,
        query: String,
        placeholder: String = "",
        label: String = "",
        onQueryChange: (query: String) -> Unit = {},
        onClearQuery: () -> Unit = {}
) {
    when {
        isSkeleton -> {
            if (brush == null) return
            Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(brush)
            )
        }

        else       -> SearchTextField(
                query = query,
                placeholder = placeholder,
                label = label,
                onQueryChange = onQueryChange,
                onClearQuery = onClearQuery
        )
    }
}

/**
 * A private composable that renders a styled text field for search input.
 *
 * This text field is designed for search functionality, featuring a search icon, a clear button
 * that appears when there is text, and keyboard actions configured for search. It also manages
 * its focus state, hiding the keyboard and clearing focus when a search is performed or when the
 * back button is pressed while the field is focused.
 *
 * @param query [String] The current text to display in the search field.
 * @param placeholder [String] The placeholder text to display when the search field is empty.
 * @param label [String] The label for the search field.
 * @param onQueryChange [(query: String) -> Unit][onQueryChange] A callback invoked with the new string when the user types in the field.
 * @param onClearQuery [() -> Unit][onClearQuery] A callback invoked when the user clicks the clear icon.
 */
@Composable
private fun SearchTextField(
        query: String,
        placeholder: String = "",
        label: String = "",
        onQueryChange: (query: String) -> Unit = {},
        onClearQuery: () -> Unit = {}
) {
    var isFocused by rememberSaveable { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler(enabled = query.isNotEmpty() && isFocused) {
        focusManager?.clearFocus()
    }

    TextField(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    if (!isFocused) keyboardController?.hide()
                },
            value = query,
            onValueChange = onQueryChange,
            shape = RoundedCornerShape(20.dp),
            colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
            singleLine = true,
            placeholder = { Text(placeholder) },
            label = { Text(label) },
            leadingIcon = { Icon(painter = painterResource(id = R.drawable.search_24px), contentDescription = "Search Icon") },
            trailingIcon = {
                if (query.isEmpty()) return@TextField
                Icon(
                        modifier = Modifier.clickable(onClick = onClearQuery),
                        painter = painterResource(id = R.drawable.close_24px),
                        contentDescription = "Clear Search Icon"
                )
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager?.clearFocus()
                keyboardController?.hide()
            })
    )
}

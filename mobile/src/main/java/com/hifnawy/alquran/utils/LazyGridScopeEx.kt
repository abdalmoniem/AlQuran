package com.hifnawy.alquran.utils

import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import com.hifnawy.alquran.utils.LazyGridScopeEx.gridItems

/**
 * An extension object for [LazyGridScope] providing utility functions.
 *
 * This object groups extension functions that simplify common patterns when
 * working with lazy grids, particularly for handling loading states and
 * displaying actual data. The primary goal is to reduce boilerplate code
 * in the UI layer when dealing with lists that have a skeleton or
 * loading representation.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see gridItems
 */
object LazyGridScopeEx {

    /**
     * A composable that displays a grid of items, intelligently handling a skeleton/loading state.
     *
     * This function simplifies the common UI pattern of showing a placeholder (skeleton) view
     * while data is loading, and then switching to the actual data once it's available.
     * It avoids the need for explicit `if/else` blocks in the composable tree for handling
     * the loading state.
     *
     * When `isSkeleton` is `true`, it displays a grid of mock items. The number of mock items
     * can be controlled by `mockCount`. The `content` lambda is called with a `null` item,
     * allowing you to render a placeholder composable.
     *
     * When `isSkeleton` is `false`, it displays the actual `items` from the list. The `content`
     * lambda is called with the real item from the list, allowing you to render the data-driven
     * composable. The `key` for each item is derived from its `hashCode()` to improve performance
     * and state preservation during recompositions.
     *
     * @receiver [T] The type of the items in the list.
     *
     * @param isSkeleton [Boolean] A boolean flag to indicate if the skeleton view should be shown.
     * `true` for skeleton, `false` for actual data.
     * @param mockCount [Int] The number of placeholder items to display when `isSkeleton` is `true`.
     * Defaults to 300.
     * @param items [List< T >][List] The list of items to display when `isSkeleton` is `false`.
     * @param content [@Composable LazyGridItemScope.(Int, T?) -> Unit][content] The composable lambda to be invoked for each item in the grid.
     * It receives the index and the item. The item will be of type [T?][T],
     * being `null` in skeleton mode and non-null otherwise.
     */
    fun <T> LazyGridScope.gridItems(isSkeleton: Boolean, mockCount: Int = 300, items: List<T>, content: @Composable LazyGridItemScope.(Int, T?) -> Unit) = when {
        isSkeleton -> itemsIndexed(items = (1..mockCount).toList(), key = { _, item -> item }) { index, _ -> content(index, null) }
        else       -> itemsIndexed(items = items, key = { _, item -> item.hashCode() }) { index, item -> content(index, item) }
    }
}

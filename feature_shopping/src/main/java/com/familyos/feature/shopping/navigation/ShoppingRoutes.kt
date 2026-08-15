package com.familyos.feature.shopping.navigation

/** Type-safe route constants for the shopping feature graph. */
object ShoppingRoutes {
    const val GRAPH = "shopping_graph"
    const val LIST = "shopping/list"
    const val HISTORY = "shopping/history"
    const val ARCHIVE = "shopping/archive"
    const val ADD = "shopping/add"
    const val EDIT = "shopping/edit/{itemId}"

    /** Builds the edit route for an existing item. */
    fun edit(itemId: String): String = "shopping/edit/$itemId"
}

package com.familyos.feature.calendar.navigation

/** Route constants for the calendar feature graph. */
object CalendarRoutes {
    const val GRAPH = "calendar_graph"
    const val HOME = "calendar/home"
    const val ADD = "calendar/add"
    const val EDIT = "calendar/edit/{eventId}"

    fun edit(eventId: String): String = "calendar/edit/$eventId"
}

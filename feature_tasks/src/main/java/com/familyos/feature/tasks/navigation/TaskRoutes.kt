package com.familyos.feature.tasks.navigation

/** Route constants for the tasks feature graph. */
object TaskRoutes {
    const val GRAPH = "tasks_graph"
    const val LIST = "tasks/list"
    const val DETAIL = "tasks/detail/{taskId}"
    const val ADD = "tasks/add"
    const val EDIT = "tasks/edit/{taskId}"

    fun detail(taskId: String): String = "tasks/detail/$taskId"
    fun edit(taskId: String): String = "tasks/edit/$taskId"
}

package com.familyos.feature.budget.navigation

/** Route constants for the budget feature graph. */
object BudgetRoutes {
    const val GRAPH = "budget_graph"
    const val HOME = "budget/home"
    const val ADD = "budget/add"
    const val EDIT = "budget/edit/{transactionId}"
    const val REPORT = "budget/report"

    fun edit(transactionId: String): String = "budget/edit/$transactionId"
}

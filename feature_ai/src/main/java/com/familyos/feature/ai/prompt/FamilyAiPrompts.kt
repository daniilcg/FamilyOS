package com.familyos.feature.ai.prompt

/**
 * Production prompt engineering for Family AI structured features.
 */
object FamilyAiPrompts {

    val SYSTEM_CORE = """
You are Family AI inside FamilyOS, a family OS for shared life management.
Always respond with a single valid JSON object. Never wrap JSON in markdown fences.
Prefer actionable, concise family-friendly suggestions.
""".trimIndent()

    val SHOPPING_FROM_RECIPE = """
Task: Generate a shopping list from a recipe description.
Example user input: "Borscht for 6"
Return JSON:
{
  "action": "create_shopping_list",
  "title": string,
  "servings": number,
  "items": [
    { "title": string, "quantity": number, "unit": string|null, "category": "PRODUCTS"|"HOME"|"PHARMACY"|"AUTO"|"PETS"|"KIDS"|"CLOTHING"|"ELECTRONICS"|"OTHER" }
  ],
  "notes": string
}
Include realistic quantities for the requested servings. Deduplicate ingredients.
""".trimIndent()

    val TASKS_FROM_GOAL = """
Task: Break a family goal into concrete tasks.
Example user input: "Prepare birthday party for our child"
Return JSON:
{
  "action": "create_task_set",
  "goal": string,
  "tasks": [
    { "title": string, "description": string, "priority": "LOW"|"MEDIUM"|"HIGH"|"URGENT", "dueInDays": number|null }
  ]
}
Create 5-12 actionable tasks covering prep, shopping, invites, and cleanup.
""".trimIndent()

    val BUDGET_ALLOCATION = """
Task: Propose a household budget allocation plan.
Example user input: "Family of 4, monthly budget 1200 EUR"
Return JSON:
{
  "action": "budget_plan",
  "currency": string,
  "total": number,
  "allocations": [
    { "category": "FOOD"|"UTILITIES"|"CAR"|"EDUCATION"|"HEALTH"|"ENTERTAINMENT"|"TRAVEL"|"OTHER", "amount": number, "percent": number, "tips": string }
  ],
  "summary": string
}
Percentages must sum to ~100. Keep amounts practical.
""".trimIndent()

    val TRIP_CHECKLIST = """
Task: Build a family trip checklist.
Example user input: "Weekend trip to Chernivtsi for 5 days"
Return JSON:
{
  "action": "trip_checklist",
  "destination": string,
  "days": number,
  "packing": [ { "title": string, "quantity": number, "unit": string|null } ],
  "tasks": [ { "title": string, "description": string, "priority": "LOW"|"MEDIUM"|"HIGH"|"URGENT", "dueInDays": number|null } ],
  "tips": [string]
}
Cover documents, clothes, health, transport, and home-leaving tasks.
""".trimIndent()

    /**
     * Chooses the best specialized system add-on for a free-form user prompt.
     */
    fun detectFeaturePrompt(userText: String): String {
        val t = userText.lowercase()
        return when {
            listOf("recipe", "borscht", "soup", "ingredients", "cook", "meal").any { it in t } ->
                SHOPPING_FROM_RECIPE
            listOf("budget", "allocate", "money", "eur", "usd", "spend").any { it in t } ->
                BUDGET_ALLOCATION
            listOf("trip", "travel", "pack", "vacation", "weekend", "flight").any { it in t } ->
                TRIP_CHECKLIST
            listOf("birthday", "prepare", "goal", "plan", "tasks", "party").any { it in t } ->
                TASKS_FROM_GOAL
            else -> """
Return JSON:
{
  "action": "chat",
  "reply": string,
  "suggestedActions": ["create_shopping_list"|"create_task_set"|"budget_plan"|"trip_checklist"]
}
""".trimIndent()
        }
    }
}

package com.familyos.feature.ai.parser

import com.familyos.core.domain.model.BudgetCategory
import com.familyos.core.domain.model.ShoppingCategory
import com.familyos.core.domain.model.TaskPriority
import com.familyos.core.domain.usecase.ai.ApplyAiShoppingListUseCase
import com.familyos.core.domain.usecase.ai.ApplyAiTaskSetUseCase
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Structured domain actions parsed from Family AI JSON responses.
 */
sealed class AiDomainAction {
    data class ShoppingList(
        val title: String,
        val items: List<ApplyAiShoppingListUseCase.AiShoppingLine>,
        val notes: String = "",
    ) : AiDomainAction()

    data class TaskSet(
        val goal: String,
        val tasks: List<ApplyAiTaskSetUseCase.AiTaskLine>,
    ) : AiDomainAction()

    data class BudgetPlan(
        val currency: String,
        val total: Double,
        val allocations: List<Allocation>,
        val summary: String,
    ) : AiDomainAction() {
        data class Allocation(
            val category: BudgetCategory,
            val amount: Double,
            val percent: Double,
            val tips: String,
        )
    }

    data class TripChecklist(
        val destination: String,
        val packing: List<ApplyAiShoppingListUseCase.AiShoppingLine>,
        val tasks: List<ApplyAiTaskSetUseCase.AiTaskLine>,
        val tips: List<String>,
    ) : AiDomainAction()

    data class ChatReply(val reply: String) : AiDomainAction()

    data class Unknown(val raw: String) : AiDomainAction()

    val canApplyToFamily: Boolean
        get() = when (this) {
            is ShoppingList -> items.isNotEmpty()
            is TaskSet -> tasks.isNotEmpty()
            is TripChecklist -> packing.isNotEmpty() || tasks.isNotEmpty()
            is BudgetPlan, is ChatReply, is Unknown -> false
        }
}

/**
 * Parses provider JSON into [AiDomainAction] values.
 */
@Singleton
class AiResponseParser @Inject constructor(
    private val json: Json,
) {
    fun parse(content: String): AiDomainAction {
        val cleaned = extractJsonObject(content)
        return runCatching {
            val root = json.parseToJsonElement(cleaned).jsonObject
            val items = shoppingLines(root)
            when (root["action"]?.jsonPrimitive?.contentOrNull?.lowercase()) {
                "create_shopping_list", "shopping_list", "shopping" -> AiDomainAction.ShoppingList(
                    title = root.string("title") ?: root.string("dish") ?: "Shopping list",
                    items = items,
                    notes = root.string("notes").orEmpty(),
                )
                "create_task_set", "task_set", "tasks" -> AiDomainAction.TaskSet(
                    goal = root.string("goal") ?: "Goal",
                    tasks = root.array("tasks").mapNotNull { it.asTaskLine() },
                )
                "budget_plan" -> AiDomainAction.BudgetPlan(
                    currency = root.string("currency") ?: "EUR",
                    total = root["total"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    allocations = root.array("allocations").mapNotNull { el ->
                        val o = el.jsonObject
                        AiDomainAction.BudgetPlan.Allocation(
                            category = o.string("category").toBudgetCategory(),
                            amount = o["amount"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                            percent = o["percent"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                            tips = o.string("tips").orEmpty(),
                        )
                    },
                    summary = root.string("summary").orEmpty(),
                )
                "trip_checklist" -> AiDomainAction.TripChecklist(
                    destination = root.string("destination") ?: "Trip",
                    packing = root.array("packing").ifEmpty { root.array("items") }.mapNotNull { it.asShoppingLine() },
                    tasks = root.array("tasks").mapNotNull { it.asTaskLine() },
                    tips = root.array("tips").mapNotNull { it.jsonPrimitive.contentOrNull },
                )
                "chat" -> if (items.isNotEmpty()) {
                    AiDomainAction.ShoppingList(
                        title = root.string("title") ?: root.string("reply")?.take(80) ?: "Shopping list",
                        items = items,
                        notes = root.string("notes") ?: root.string("reply").orEmpty(),
                    )
                } else {
                    AiDomainAction.ChatReply(root.string("reply") ?: cleaned)
                }
                else -> if (items.isNotEmpty()) {
                    AiDomainAction.ShoppingList(
                        title = root.string("title") ?: "Shopping list",
                        items = items,
                        notes = root.string("notes").orEmpty(),
                    )
                } else {
                    AiDomainAction.Unknown(cleaned)
                }
            }
        }.getOrElse { AiDomainAction.Unknown(cleaned) }
    }

    private fun extractJsonObject(content: String): String {
        val trimmed = content.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start >= 0 && end > start) trimmed.substring(start, end + 1) else trimmed
    }

    private fun shoppingLines(root: JsonObject) =
        root.array("items").ifEmpty { root.array("ingredients") }.mapNotNull { it.asShoppingLine() }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.array(key: String): JsonArray =
        this[key]?.jsonArray ?: JsonArray(emptyList())

    private fun kotlinx.serialization.json.JsonElement.asShoppingLine(): ApplyAiShoppingListUseCase.AiShoppingLine? {
        val o = runCatching { jsonObject }.getOrNull() ?: return null
        val title = o["title"]?.jsonPrimitive?.contentOrNull?.trim()
            ?: o["name"]?.jsonPrimitive?.contentOrNull?.trim()
            ?: o["ingredient"]?.jsonPrimitive?.contentOrNull?.trim()
            ?: ""
        if (title.isEmpty()) return null
        return ApplyAiShoppingListUseCase.AiShoppingLine(
            title = title,
            quantity = o["quantity"]?.jsonPrimitive?.doubleOrNull
                ?: o["quantity"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                ?: 1.0,
            unit = o["unit"]?.jsonPrimitive?.contentOrNull,
            category = enumOr(o["category"]?.jsonPrimitive?.contentOrNull, ShoppingCategory.PRODUCTS),
        )
    }

    private fun kotlinx.serialization.json.JsonElement.asTaskLine(): ApplyAiTaskSetUseCase.AiTaskLine? {
        val o = runCatching { jsonObject }.getOrNull() ?: return null
        val title = o["title"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (title.isEmpty()) return null
        val dueInDays = o["dueInDays"]?.jsonPrimitive?.intOrNull
        val dueAt = dueInDays?.let { System.currentTimeMillis() + it * 24L * 60L * 60L * 1000L }
        return ApplyAiTaskSetUseCase.AiTaskLine(
            title = title,
            description = o["description"]?.jsonPrimitive?.contentOrNull,
            priority = enumOr(o["priority"]?.jsonPrimitive?.contentOrNull, TaskPriority.MEDIUM),
            dueAt = dueAt,
        )
    }

    private inline fun <reified T : Enum<T>> enumOr(raw: String?, fallback: T): T =
        raw?.let { runCatching { enumValueOf<T>(it.uppercase()) }.getOrNull() } ?: fallback

    private fun String?.toBudgetCategory(): BudgetCategory = when (this?.uppercase()) {
        "TRANSPORT" -> BudgetCategory.CAR
        "INCOME", "HOUSING", "SHOPPING", "SAVINGS" -> BudgetCategory.OTHER
        null -> BudgetCategory.OTHER
        else -> enumOr(this, BudgetCategory.OTHER)
    }
}

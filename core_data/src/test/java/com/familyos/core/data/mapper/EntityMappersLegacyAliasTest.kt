package com.familyos.core.data.mapper

import com.familyos.core.domain.model.ShoppingCategory
import com.familyos.core.domain.model.TaskStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EntityMappersLegacyAliasTest {

    @Test
    fun shoppingCategory_mapsLegacyAliases() {
        assertThat(EntityMappers.mapShoppingCategory("GROCERIES")).isEqualTo(ShoppingCategory.PRODUCTS)
        assertThat(EntityMappers.mapShoppingCategory("HOUSEHOLD")).isEqualTo(ShoppingCategory.HOME)
        assertThat(EntityMappers.mapShoppingCategory("PERSONAL_CARE")).isEqualTo(ShoppingCategory.PHARMACY)
    }

    @Test
    fun shoppingCategory_mapsCurrentValues() {
        assertThat(EntityMappers.mapShoppingCategory("PRODUCTS")).isEqualTo(ShoppingCategory.PRODUCTS)
        assertThat(EntityMappers.mapShoppingCategory("AUTO")).isEqualTo(ShoppingCategory.AUTO)
        assertThat(EntityMappers.mapShoppingCategory("KIDS")).isEqualTo(ShoppingCategory.KIDS)
        assertThat(EntityMappers.mapShoppingCategory("PHARMACY")).isEqualTo(ShoppingCategory.PHARMACY)
    }

    @Test
    fun shoppingCategory_unknownFallsBackToOther() {
        assertThat(EntityMappers.mapShoppingCategory("UNKNOWN_CAT")).isEqualTo(ShoppingCategory.OTHER)
    }

    @Test
    fun taskStatus_mapsTodoToNew() {
        assertThat(EntityMappers.mapTaskStatus("TODO")).isEqualTo(TaskStatus.NEW)
        assertThat(EntityMappers.mapTaskStatus("WAITING")).isEqualTo(TaskStatus.WAITING)
        assertThat(EntityMappers.mapTaskStatus("OVERDUE")).isEqualTo(TaskStatus.OVERDUE)
        assertThat(EntityMappers.mapTaskStatus("NEW")).isEqualTo(TaskStatus.NEW)
    }
}

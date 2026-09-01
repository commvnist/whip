package com.whip.app.domain

sealed interface AreaScope {
    val storageKey: String

    data object All : AreaScope { override val storageKey = "all" }
    data object Unassigned : AreaScope { override val storageKey = "unassigned" }
    data class One(val areaId: String) : AreaScope {
        init { require(areaId.isNotBlank()) }
        override val storageKey: String = "area:$areaId"
    }

    companion object {
        fun fromStorageKey(value: String?): AreaScope = when {
            value == Unassigned.storageKey -> Unassigned
            value?.startsWith("area:") == true && value.removePrefix("area:").isNotBlank() ->
                One(value.removePrefix("area:"))
            else -> All
        }
    }
}

fun AreaScope.matches(areaId: String?): Boolean = when (this) {
    AreaScope.All -> true
    AreaScope.Unassigned -> areaId == null
    is AreaScope.One -> areaId == this.areaId
}

fun AreaScope.defaultAreaId(): String? = (this as? AreaScope.One)?.areaId

/** The narrowest currently selectable scope that can truthfully reveal a newly saved entity. */
fun scopeForSavedArea(
    areaId: String?,
    availableAreaIds: Set<String>? = null,
): AreaScope = when {
    areaId == null -> AreaScope.Unassigned
    availableAreaIds == null || areaId in availableAreaIds -> AreaScope.One(areaId)
    else -> AreaScope.All
}

package com.flxProviders.stremio.api.dto

import com.flixclusive.core.util.exception.safeCall

data class StremioAddOn(
    val id: String?,
    val name: String?,
    val version: String?,
    val logo: String?,
    val description: String?,
    val baseUrl: String? = null,
    val resources: List<*> = listOf<Any>(),
    val types: List<String> = emptyList()
) {
    val isCatalog: Boolean
        get() {
            val firstItem = resources.firstOrNull()
                ?: return false

            if (firstItem is String) {
                return resources.any { resource ->
                    (resource as String).equals("catalog", true)
                }
            }

            if (firstItem is Map<*, *>) {
                return resources.any { resource ->
                    safeCall {
                        val resourceMap = resource as Map<*, *>

                        (resourceMap["name"] as String).equals("catalog", true)
                    } ?: false
                }
            }

            return false
        }
}
package com.flxProviders.superstream.api.dto.old

internal data class CommonResponse<T>(
    val code: Int? = null,
    val msg: String? = null,
    val data: T? = null,
)
package com.flxProviders.superstream.api.dto

import com.google.gson.annotations.SerializedName

data class ExternalResponse(
    @SerializedName("data") val data: Data? = null,
) {
    data class Data(
        @SerializedName("link") val link: String? = null,
        @SerializedName("file_list") val fileList: List<FileList>? = listOf(),
    ) {
        data class FileList(
            @SerializedName("fid") val fid: Long? = null,
            @SerializedName("file_name") val fileName: String? = null,
            private val is_dir: Int = 0,
        ) {
            val isDirectory: Boolean
                get() = is_dir == 1
        }
    }
}
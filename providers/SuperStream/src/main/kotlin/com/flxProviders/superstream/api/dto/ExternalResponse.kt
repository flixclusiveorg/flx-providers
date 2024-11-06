package com.flxProviders.superstream.api.dto

import com.google.gson.annotations.SerializedName

data class ExternalResponse(
    @SerializedName("data") val data: Data? = null,
) {
    data class Data(
        @SerializedName("link", alternate = ["share_link"]) val link: String? = null,
        @SerializedName("file_list") val files: List<StreamFile>? = listOf(),
    ) {
        data class StreamFile(
            @SerializedName("fid") val fid: Long? = null,
            @SerializedName("file_name") val fileName: String = "UNKNOWN FILENAME",
            @SerializedName("file_size") val fileSize: String = "UNKNOWN FILE SIZE",
            @SerializedName("add_time") val addedOn: String = "UNKNOWN DATE",
            private val is_dir: Int = 0,
        ) {
            val isDirectory: Boolean
                get() = is_dir == 1
        }
    }
}
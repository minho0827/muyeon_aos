package com.muyeon.app.data.models.file

data class FileImageInfo(
    val fileData: String,
    val fileName: String,
    val fileExt: String,
    val fileSize: Long,
    val fileDistCd: String,
    val fileMime: String
)
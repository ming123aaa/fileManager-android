package com.ohuang.filemanager.server.bean

import com.google.gson.annotations.SerializedName

data class FileBean(
    @SerializedName("name")
    var name: String = "",
    
    @SerializedName("length")
    var length: Long = 0,
    
    @SerializedName("isFolder")
    var isFolder: Boolean = false,
    
    @SerializedName("lastModified")
    var lastModified: Long = 0
)
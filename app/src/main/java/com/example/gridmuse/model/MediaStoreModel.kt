package com.example.gridmuse.model

import android.net.Uri

data class DevicePhoto (
  val id: Long,
  val name: String,
  val path: String,
  val imgSrc: String,
  var sort: Int,
  var isHidden: Boolean = false
){
  // 在需要使用 Uri 的地方提供一個擴展屬性
  val uri: Uri
    get() = Uri.parse(imgSrc)
}


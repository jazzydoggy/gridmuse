package com.example.gridmuse.network

import android.icu.text.SimpleDateFormat
import com.example.gridmuse.R
import com.example.gridmuse.model.DevicePhoto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface IGPhotosService {
  @GET
  suspend fun getMediaPhotos(@Url url: String): IGPhotoResponse
}

// 定義用於接收 API 回應的資料類型
data class IGPhotoResponse(
  val data: List<InstagramPhoto>,
  val paging: Paging?
)

data class Paging(
  val next: String? // `next` 指向下一頁的 URL，如果沒有下一頁則為 null
)

data class InstagramPhoto(
  val id: String,
  val media_type: String,
  val media_url: String,
  val thumbnail_url: String?,
  val timestamp: String,
  val permalink: String
) {
  fun toDevicePhoto(): DevicePhoto {
    val imgSrcUri = when {
      media_type == "IMAGE" || media_type == "CAROUSEL_ALBUM" -> media_url
      media_type == "VIDEO" && thumbnail_url != null -> thumbnail_url
      else -> "${R.drawable.ic_broken_image}"
    }
    return DevicePhoto(
      id = id.toLongOrNull() ?: 0L, // 如果 id 是字串，轉換成 Long，若失敗設為 0L
      name = media_type, // 可以自訂名稱
      path = permalink, // 使用 Instagram 連結作為 path
      imgSrc = imgSrcUri,
      sort = 0 // 預設排序值
    )
  }
}

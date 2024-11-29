package com.example.gridmuse.data

import android.content.Context
import com.example.gridmuse.model.DevicePhoto
import com.example.gridmuse.network.IGPhotosService
import com.example.gridmuse.network.InstagramPhoto
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileWriter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

//token
//jazzychuan
//9297205920307321
//IGQWRNWFEyMXJad3BJTkZAzU1hyenY3MGdSZAk9QMFBtZAlBGWUhMa21fS0YxbGd1TkRkUTF2NlloUXh4cndfOGVacnFQandOelBMa2tyUEJyVWdiVlo1OWlsNHJLWGNDdnVteEsxd0JXeldSbFZAhejRhd2FBc3hLZAWcZD
//st.rambler
//8543930829061574
//IGQWRPaFhUN3VOUXE1dUxjcTZAzWU1Ta1ZA3cGY0TU9BQnBiUTVEU2Q5QXNobWp0Ti1la25qU0lvOEQxb1BOWWJya1ZACNHNrRWdqLVhFRUE0dWJwVXJBRnd0aGI4MWtGbUZAhaE1UV2NVRHN1aGtXdno1QVR5bS04dDgZD

interface NetworkPhotosRepository {
  /** Fetches list of MarsPhoto from marsApi */
  suspend fun getNetworkPhotos(): List<DevicePhoto>
  suspend fun getImageList(): List<DevicePhoto>
  //suspend fun swapPhotos(sortA: Int,sortB: Int)
}

class IGPhotosRepository(private val context: Context) : NetworkPhotosRepository{
  var imageList = mutableListOf<DevicePhoto>() // 供外部調用的照片清單
  private val baseUrl = "https://graph.instagram.com"
  private val apiVersion = "v21.0"
  private val userID = "8543930829061574"
  private val fields = "id,timestamp,permalink,media_type,media_url,thumbnail_url"
  private val accessKey = "IGQWRPV0pCdlZAvWU5lWHptRi1nd3VfWXJkdTFUenBvdHA1X2xueEdnSEdrVnVGZA1BnbUxXaDNrS2VUYmRoSTdZAU2VibXVmb0RJWnFMbnNQVTdPSHphWkp0c0YzbjdRYks0TkxlUTNlWU5RMG1aX3VvX0xOb1lwQ1UZD"
  private val gson = Gson()
  private val jsonFile = File(context.filesDir, "network_photo_data.json") // 紀錄照片排序之DB

  private val retrofit = Retrofit.Builder()
    .baseUrl(baseUrl)
    .addConverterFactory(GsonConverterFactory.create())
    .client(OkHttpClient.Builder().build())
    .build()

  private val service: IGPhotosService = retrofit.create(IGPhotosService::class.java)

  override suspend fun getNetworkPhotos(): List<DevicePhoto> = suspendCoroutine { continuous ->
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val photos = getNetworkPhotosImpl()
        continuous.resume(photos)
      }catch (e: Exception) {
        continuous.resumeWithException(e)
      }
    }
  }

  private suspend fun getNetworkPhotosImpl(): List<DevicePhoto> {
    clearJsonFile()
    val allPhotos = mutableListOf<InstagramPhoto>()
    var nextUrl: String? = "$baseUrl/$apiVersion/$userID/media?fields=$fields&access_token=$accessKey"

    try {
      while (nextUrl != null) {
        val response = service.getMediaPhotos(nextUrl)
        allPhotos.addAll(response.data)
        nextUrl = response.paging?.next // 獲取下一頁的 URL
      }
      // 更新 imageList 並寫入 JSON 文件
      imageList.clear()
      imageList = allPhotos.map { it.toDevicePhoto() }.toMutableList()
      writeNetworkPhotosToJson(imageList)

      return imageList
    } catch (e: Exception) {
      e.printStackTrace()
      return emptyList()
    }
  }
  override suspend fun getImageList(): List<DevicePhoto> {
    return imageList.sortedByDescending { it.sort }
  }

  /*清除DB資料*/
  fun clearJsonFile() {
    if (jsonFile.exists()) {
      FileWriter(jsonFile).use { writer ->
        writer.write("[]") // 空的 JSON 列表
      }
    }
  }

  /*整筆覆蓋現有DB資料*/
  private fun writeNetworkPhotosToJson(photos: List<DevicePhoto>) {
    val jsonString = gson.toJson(photos)
    FileWriter(jsonFile).use { writer ->
      writer.write(jsonString)
    }
  }
}

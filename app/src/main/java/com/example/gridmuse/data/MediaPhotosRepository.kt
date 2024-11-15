package com.example.gridmuse.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.room.Room
import com.example.gridmuse.data.db.AppDatabase
import com.example.gridmuse.model.DevicePhoto
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter

interface MediaPhotosRepository {
  /** Fetches list of MarsPhoto from marsApi */
  suspend fun getMediaPhotos(): List<DevicePhoto>
  suspend fun getImageList(): List<DevicePhoto>
  suspend fun swapPhotos(selected: Int, target: Int)
  suspend fun insertPhotoAtSort(selected: Int, target: Int)
  suspend fun updatePhotoVisibility(photoId: Long, isHidden: Boolean)
  fun clearDatabase()
}

class LightRoomPhotosRepository(private val context: Context) : MediaPhotosRepository{
  var imageList = mutableListOf<DevicePhoto>() // 供外部調用的照片清單
  private val gson = Gson()
  private val jsonFile = File(context.filesDir, "photo_data.json") // 紀錄照片排序之DB
  private val db = Room.databaseBuilder(
    context.applicationContext,
    AppDatabase::class.java, "app_database"
  ).build()
  private val devicePhotoDao = db.devicePhotoDao()

  /*實作 getMediaPhotos 功能*/
  override suspend fun getMediaPhotos(): List<DevicePhoto> = withContext(Dispatchers.IO) {
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
      MediaStore.Images.Media._ID,
      MediaStore.Images.Media.DISPLAY_NAME,
      MediaStore.Images.Media.DATE_TAKEN,
      MediaStore.Images.Media.RELATIVE_PATH,
    )
    val selection =  "("+ MediaStore.Downloads.RELATIVE_PATH +" LIKE ?)"
    val selectionArgs = arrayOf("Pictures/AdobeLightroom/%")
    val imageSortOrder = "${MediaStore.Images.Media.DATE_TAKEN} "
    val cursor = context.contentResolver.query(
      collection,
      projection,
      selection,
      selectionArgs,
      imageSortOrder
    )
    cursor.use {
      it?.let {
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val pathColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
        imageList.clear()
        //clearDatabase()
        while (it.moveToNext()) {
          val id = it.getLong(idColumn)
          val name = it.getString(nameColumn)
          val path = it.getString(pathColumn)
          val contentUriString = ContentUris.withAppendedId(collection, id).toString()
          val photo = DevicePhoto(
            id = id,
            name = name,
            path = path,
            imgSrc = contentUriString,
            sort = 0,
            isHidden = false
          )
          insertDevicePhotoToDatabase(photo)
          imageList.add(photo)
        }
      }
    }
    synchronizeDatabaseWithImageList() //只對jsonFile做操作
    imageList = makeSortContinuous().toMutableList() //排序重整並同步imageList
    return@withContext imageList.sortedByDescending { it.sort }
  }

  /*返回imageList給外部使用*/
  override suspend fun getImageList(): List<DevicePhoto> = withContext(Dispatchers.IO) {
    return@withContext imageList.sortedByDescending { it.sort }
  }

  /*照片排序值交換*/
  override suspend fun swapPhotos(selected: Int,target: Int) = withContext(Dispatchers.IO) {
    // 讀取 JSON 中的現有照片
    val existingPhotos = readDevicePhotosFromJson().toMutableList()
    // 尋找JSON file對應photo
    val photoA = existingPhotos.find { it.sort == selected }
    val photoB = existingPhotos.find { it.sort == target }
    // 交換 JSON file sort 值
    if (photoA != null && photoB != null) {
      photoA.sort = target
      photoB.sort = selected
      // 寫入更新後的照片列表到 JSON 文件
      writeDevicePhotosToJson(existingPhotos)
      // 更新 imageList 中的對應項目
      val imageListPhotoA = imageList.find { it.id == photoA.id }
      val imageListPhotoB = imageList.find { it.id == photoB.id }
      imageListPhotoA?.sort = target
      imageListPhotoB?.sort = selected
    }
  }

  /*照片插入排序*/
  override suspend fun insertPhotoAtSort(selected: Int,target: Int) = withContext(Dispatchers.IO){
    val existingPhotos = readDevicePhotosFromJson().toMutableList()
    val selectedPhoto = existingPhotos.find { it.sort == selected } ?: return@withContext
    existingPhotos.forEach { photo ->
      when {
        // 如果原始照片排序高於目標排序且低於等於選定排序值，則將排序值 -1
        //[1,2,3,4,5,6] sel=5 tar=2 -> [1,5,2,3,4,6]
        photo.sort in (target until selected) -> {
          photo.sort += 1
        }
        // 如果原始照片排序低於目標排序且高於等於選定排序值，則將排序值 +1
        //[1,2,3,4,5,6] sel=2 tar=5 -> [1,3,4,5,2,6]
        photo.sort in (selected + 1)..target -> {
          photo.sort -= 1
        }
      }
    }
    selectedPhoto.sort = target
    existingPhotos.sortBy { it.sort }
    writeDevicePhotosToJson(existingPhotos)
    updateImageListSortBy(existingPhotos) // 同步更新 imageList
  }

  /*更改照片是否隱藏*/
  override suspend fun updatePhotoVisibility(photoId: Long, isHidden: Boolean) = withContext(Dispatchers.IO) {
    val existingPhotos = readDevicePhotosFromJson().toMutableList()
    // 找到要更新顯示狀態的照片
    val photoToUpdate = existingPhotos.find { it.id == photoId }
    if (photoToUpdate != null) {
      // 更新照片的 isHidden 屬性
      photoToUpdate.isHidden = isHidden
      // 寫回更新後的資料到 JSON 文件
      writeDevicePhotosToJson(existingPhotos)
      // 也需要更新 imageList 中對應照片的狀態
      val imageListPhoto = imageList.find { it.id == photoId }
      imageListPhoto?.isHidden = isHidden
    }
  }

  /*插入一筆照片至現有DB*/
  private fun insertDevicePhotoToDatabase(newPhoto: DevicePhoto) {
    // 讀取 JSON 現有資料
    val existingPhotos = readDevicePhotosFromJson().toMutableList()
    val maxSortValue = existingPhotos.maxOfOrNull { it.sort } ?: 0
    // 檢查 id 是否唯一
    val existingPhoto = existingPhotos.find { it.id == newPhoto.id }
    //若照片已存在
    if (existingPhoto != null) {
      if (existingPhoto.sort < 1) {
        // 更改existingPhotos該ID的sort值
        existingPhoto.sort = maxSortValue + 1
        writeDevicePhotosToJson(existingPhotos)
      }
    } else {
      // 若 id 不存在，分配新的 sort 值
      newPhoto.sort = maxSortValue + 1
      // 增加新照片到existingPhotos
      existingPhotos.add(newPhoto)
      writeDevicePhotosToJson(existingPhotos)
    }
  }

  /*讀取現有DB資料*/
  private fun readDevicePhotosFromJson(): List<DevicePhoto> {
    return if (jsonFile.exists()) {
      FileReader(jsonFile).use { reader ->
        val devicePhotosArray = gson.fromJson(reader, Array<DevicePhoto>::class.java)
        devicePhotosArray?.toList() ?: emptyList()
      }
    } else {
      emptyList()
    }
  }

  /*整筆覆蓋現有DB資料*/
  private fun writeDevicePhotosToJson(photos: List<DevicePhoto>) {
    val jsonString = gson.toJson(photos)
    FileWriter(jsonFile).use { writer ->
      writer.write(jsonString)
    }
  }

  /*清除DB資料*/
  override fun clearDatabase() {
    if (jsonFile.exists()) {
      FileWriter(jsonFile).use { writer ->
        writer.write("[]") // 空的 JSON 列表
      }
    }
  }

  /*同步DB資料為裝置讀入資料*/
  private fun synchronizeDatabaseWithImageList(): List<DevicePhoto> {
    // 讀取 JSON 文件中的現有照片
    val existingPhotos = readDevicePhotosFromJson().toMutableList()
    // 只保留 imageList 中存在的 id
    val updatedPhotos = existingPhotos.filter { existingPhoto ->
      imageList.any { it.id == existingPhoto.id }
    }.toMutableList()
    // 將更新後的照片列表寫入 JSON 文件
    writeDevicePhotosToJson(updatedPhotos)
    return existingPhotos
  }

  /*更新照片Sort值為連續整數*/
  private fun makeSortContinuous():List<DevicePhoto> {
    val existingPhotos = readDevicePhotosFromJson().toMutableList()
    //var hiddenNum = 98///
    //existingPhotos[hiddenNum-1].isHidden = true///
    existingPhotos.sortBy { it.sort }
    for (i in existingPhotos.indices) {
      existingPhotos[i].sort = i + 1
      //existingPhotos[i].isHidden = false
    }
    writeDevicePhotosToJson(existingPhotos)
    return existingPhotos
  }

  /*依據傳入 List 更新 imageList sort 值*/
  private fun updateImageListSortBy(updatedPhotos: List<DevicePhoto>) {
    imageList.forEach { imagePhoto ->
      updatedPhotos.find { it.id == imagePhoto.id }?.let { updatedPhoto ->
        imagePhoto.sort = updatedPhoto.sort
      }
    }
  }

}

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
    imageList = devicePhotoDao.getAllPhotos().sortedByDescending { it.sort }.toMutableList()
    return@withContext imageList
  }

  /*照片排序值交換*/
  override suspend fun swapPhotos(selected: Int,target: Int) = withContext(Dispatchers.IO) {
    val photoA = devicePhotoDao.getAllPhotos().find { it.sort == selected }
    val photoB = devicePhotoDao.getAllPhotos().find { it.sort == target }
    if (photoA != null && photoB != null) {
      photoA.sort = target
      photoB.sort = selected
      devicePhotoDao.updatePhoto(photoA)
      devicePhotoDao.updatePhoto(photoB)
//      // 更新 imageList 中的對應項目
//      val imageListPhotoA = imageList.find { it.id == photoA.id }
//      val imageListPhotoB = imageList.find { it.id == photoB.id }
//      imageListPhotoA?.sort = target
//      imageListPhotoB?.sort = selected
    }
  }

  /*照片插入排序*/
  override suspend fun insertPhotoAtSort(selected: Int,target: Int) = withContext(Dispatchers.IO){
    val existingPhotos = devicePhotoDao.getAllPhotos().toMutableList()
    val selectedPhoto = existingPhotos.find { it.sort == selected } ?: return@withContext
    existingPhotos.forEach { photo ->
      when {
        photo.sort in (target until selected) -> photo.sort += 1
        photo.sort in (selected + 1)..target -> photo.sort -= 1
      }
    }
    selectedPhoto.sort = target
    existingPhotos.sortBy { it.sort }
    existingPhotos.forEach { devicePhotoDao.updatePhoto(it) }
    updateImageListSortBy(existingPhotos) // 同步更新 imageList
  }

  /*更改照片是否隱藏*/
  override suspend fun updatePhotoVisibility(photoId: Long, isHidden: Boolean): Unit = withContext(Dispatchers.IO) {
    val photo = devicePhotoDao.getPhotoById(photoId)
    photo?.let {
      it.isHidden = isHidden
      devicePhotoDao.updatePhoto(it)
      imageList.find { image -> image.id == photoId }?.isHidden = isHidden
    }
  }

  /*插入一筆照片至現有DB*/
  private fun insertDevicePhotoToDatabase(newPhoto: DevicePhoto) {
    val existingPhoto = devicePhotoDao.getPhotoById(newPhoto.id)
    val maxSortValue = devicePhotoDao.getAllPhotos().maxOfOrNull { it.sort } ?: 0
    if (existingPhoto != null) {
      if (existingPhoto.sort < 1) {
        existingPhoto.sort = maxSortValue + 1
        devicePhotoDao.updatePhoto(existingPhoto)
      }
    } else {
      newPhoto.sort = maxSortValue + 1
      devicePhotoDao.insertPhoto(newPhoto)
    }
  }

  /*讀取現有DB資料*/
  private fun readDevicePhotosFromDatabase(): List<DevicePhoto> {
    return devicePhotoDao.getAllPhotos()
  }

  /*整筆覆蓋現有DB資料*/
  private fun writeDevicePhotosToDatabase(photos: List<DevicePhoto>) {
//    val jsonString = gson.toJson(photos)
//    FileWriter(jsonFile).use { writer ->
//      writer.write(jsonString)
//    }
  }

  /*清除DB資料*/
  override fun clearDatabase() {
    devicePhotoDao.getAllPhotos().forEach {devicePhotoDao.deletePhotoById(it.id)}
  }

  /*同步DB資料為裝置讀入資料*/
  private fun synchronizeDatabaseWithImageList(): List<DevicePhoto> {
    val existingPhotos = devicePhotoDao.getAllPhotos()
    val updatedPhotos =  existingPhotos.filter { existingPhoto ->
      if( imageList.any { it.id == existingPhoto.id } ) {
        true //保留
      } else {
        devicePhotoDao.deletePhotoById(existingPhoto.id)
        false //刪除
      }
    }
    updatedPhotos.forEach { devicePhotoDao.updatePhoto(it) }
    return updatedPhotos
  }

  /*更新照片Sort值為連續整數*/
  private fun makeSortContinuous():List<DevicePhoto> {
    val existingPhotos = devicePhotoDao.getAllPhotos().sortedBy { it.sort }.toMutableList()
    existingPhotos.forEachIndexed{ index, photo ->
      devicePhotoDao.updatePhoto( photo.apply { sort = index + 1 } )
    }
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

package com.example.gridmuse.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.gridmuse.model.DevicePhoto

@Dao
interface DevicePhotoDao {
  @Insert
  fun insertPhoto(photo: DevicePhoto)

  @Update
  fun updatePhoto(photo: DevicePhoto)

  @Query("SELECT * FROM device_photos WHERE id = :id")
  fun getPhotoById(id: Long): DevicePhoto?

  @Query("SELECT * FROM device_photos")
  fun getAllPhotos(): List<DevicePhoto>

  @Query("DELETE FROM device_photos WHERE id = :id")
  fun deletePhotoById(id: Long)
}

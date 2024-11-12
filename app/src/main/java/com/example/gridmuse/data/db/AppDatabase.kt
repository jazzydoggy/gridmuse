package com.example.gridmuse.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.gridmuse.model.DevicePhoto

@Database(entities = [DevicePhoto::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
  abstract fun devicePhotoDao(): DevicePhotoDao
}

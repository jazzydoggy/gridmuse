package com.example.gridmuse

import android.app.Application
import com.example.gridmuse.data.AppContainer
import com.example.gridmuse.data.DefaultAppContainer

class PhotosApplication : Application() {
  /** AppContainer instance used by the rest of classes to obtain dependencies */
  lateinit var container: AppContainer
  override fun onCreate() {
    super.onCreate()
    container = DefaultAppContainer(applicationContext)
  }
}

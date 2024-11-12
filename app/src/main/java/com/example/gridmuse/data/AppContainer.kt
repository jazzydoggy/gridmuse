package com.example.gridmuse.data

import android.content.Context
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType

/**
 * Dependency Injection container at the application level.
 */
interface AppContainer {
  val mediaPhotosRepository: MediaPhotosRepository
  val mediaPhotosRepositoryJson: MediaPhotosRepositoryJson
  val networkPhotosRepository: NetworkPhotosRepository
}

/**
 * Implementation for the Dependency Injection container at the application level.
 *
 * Variables are initialized lazily and the same instance is shared across the whole app.
 */
class DefaultAppContainer(context: Context) : AppContainer {
  override val mediaPhotosRepository: MediaPhotosRepository by lazy {
    LightRoomPhotosRepository(context)
  }

  override val mediaPhotosRepositoryJson: MediaPhotosRepositoryJson by lazy {
    LightRoomPhotosRepositoryJson(context)
  }

  override val networkPhotosRepository: NetworkPhotosRepository by lazy {
    IGPhotosRepository(context)
  }
}

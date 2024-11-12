package com.example.gridmuse.ui.screens

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.network.HttpException
import com.example.gridmuse.PhotosApplication
import com.example.gridmuse.data.MediaPhotosRepository
import com.example.gridmuse.data.NetworkPhotosRepository
import com.example.gridmuse.model.DevicePhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okio.IOException


sealed interface MediaUiState {
  data class Success(val photos: List<DevicePhoto>) : MediaUiState
  object Error : MediaUiState
  object Loading : MediaUiState
}

class MediaViewModel(
  application: Application,
  private val mediaPhotosRepository: MediaPhotosRepository,
  private val networkPhotosRepository: NetworkPhotosRepository
) : AndroidViewModel(application) {
  // 用 MutableLiveData 來儲存照片列表
  private var _devicePhotos = MutableStateFlow<List<DevicePhoto>> ( emptyList() )
  val devicePhotos = _devicePhotos.asStateFlow()

  private var _networkPhotos = mutableListOf<DevicePhoto>()

  var selectedSort by mutableStateOf<Int?>(null)

  // 用來管理 UI 狀態的 mutableStateOf
  private var _mediaUiStateFlow = MutableStateFlow<MediaUiState> ( MediaUiState.Loading )
  val mediaUiState = _mediaUiStateFlow.asStateFlow()
  //val mediaUiState: StateFlow<MediaUiState> get() = _mediaUiStateFlow

  init {
    viewModelScope.launch{
      loadDevicePhotos()
    }
  }

  suspend fun loadDevicePhotos() {
    _mediaUiStateFlow.value = MediaUiState.Loading
    //println("D123_network: "+ networkPhotosRepository.getNetworkPhotos())
    try {
      // 使用 mediaPhotosRepository 獲取照片列表
      val localPhotos = mediaPhotosRepository.getMediaPhotos()
      // 使用 networkPhotosRepository 獲取網路照片列表
      val networkPhotos = networkPhotosRepository.getNetworkPhotos()
      _networkPhotos = networkPhotos.toMutableList()
      // 合併兩個來源的照片列表
      val listResult = (localPhotos + _networkPhotos)//.sortedByDescending { it.sort }
      //val listResult = mediaPhotosRepository.getMediaPhotos()
      //val listResult = networkPhotosRepository.getNetworkPhotos()
      // 更新"被觀察data"的值
      _devicePhotos.value = listResult
      // 更新 UI 狀態為 Success
      _mediaUiStateFlow.value = MediaUiState.Success(listResult)
    }
    catch (e: IOException){
      _mediaUiStateFlow.value = MediaUiState.Error
    }
    catch (e: HttpException){
      _mediaUiStateFlow.value = MediaUiState.Error
    }
  }

  fun setSelectedSort(sort: Int) {
    selectedSort = sort
  }

  fun swapWithSelectedSort(targetSort: Int) {
    selectedSort?.let { selectedSortValue ->
      swapPhotos(selectedSortValue, targetSort)
      selectedSort = null  // 重置選中的 sort 值
    }
  }

  fun insertAtSelectedSort(targetSort: Int) {
    selectedSort?.let { selectedSortValue ->
      insertAtSort(selectedSortValue, targetSort)
      selectedSort = null  // 重置選中的 sort 值
    }
  }

  fun swapPhotos(sortA: Int, sortB: Int) {
    viewModelScope.launch {
      _mediaUiStateFlow.value = MediaUiState.Loading
      try {
        mediaPhotosRepository.swapPhotos(sortA, sortB)
        _devicePhotos.value = getRefreshPhotos()
        _mediaUiStateFlow.value = MediaUiState.Success(_devicePhotos.value)
      } catch (e: Exception) {
        _mediaUiStateFlow.value = MediaUiState.Error
        e.printStackTrace() // 可選：打印錯誤訊息方便調試
      }
    }
  }

  fun insertAtSort(selected: Int, target: Int) {
    viewModelScope.launch {
      try {
        _mediaUiStateFlow.value = MediaUiState.Loading
        mediaPhotosRepository.insertPhotoAtSort(selected, target)
        _devicePhotos.value = getRefreshPhotos()
        _mediaUiStateFlow.value = MediaUiState.Success(_devicePhotos.value)
      } catch (e: Exception) {
        //println("D123_insertAtSort_Error: ")
        _mediaUiStateFlow.value = MediaUiState.Error
        e.printStackTrace() // 可選：打印錯誤訊息方便調試
      }
    }
  }

  fun updatePhotoVisibility(photoId: Long, isHidden: Boolean) {
    println("D123_photoId: "+photoId+" isHidden: "+isHidden)
    viewModelScope.launch {
      try {
        // 呼叫 repository 更新照片顯示狀態
        mediaPhotosRepository.updatePhotoVisibility(photoId, isHidden)
        _devicePhotos.value = getRefreshPhotos()
      } catch (e: Exception) {
        // 處理錯誤（例如顯示錯誤訊息等）
        Log.e("MediaViewModel", "Error updating photo visibility: ${e.message}")
      }
    }
  }

  private suspend fun getRefreshPhotos(): List<DevicePhoto> {
    return try {
      val updatedLocalList = mediaPhotosRepository.getImageList()
      val updatedList = updatedLocalList + _networkPhotos
      updatedList
    } catch (e: Exception) {
      _mediaUiStateFlow.value = MediaUiState.Error
      e.printStackTrace() // 可選：打印錯誤訊息方便調試
      emptyList()
    }
  }

  companion object {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val application = (this[APPLICATION_KEY] as PhotosApplication)
        val mediaPhotosRepository = application.container.mediaPhotosRepository
        val networkPhotosRepository = application.container.networkPhotosRepository
        MediaViewModel(
          application,
          mediaPhotosRepository = mediaPhotosRepository,
          networkPhotosRepository= networkPhotosRepository
        )
      }
    }
  }
}

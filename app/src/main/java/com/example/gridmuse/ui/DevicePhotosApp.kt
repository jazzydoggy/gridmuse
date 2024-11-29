@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gridmuse.ui

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gridmuse.ui.screens.HomeScreen
import com.example.gridmuse.ui.screens.MediaViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.time.Duration
import com.example.gridmuse.ui.screens.PermissionHandler

@Composable
fun PhotosAppOpener() {
  PermissionHandler(
    permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      Manifest.permission.READ_MEDIA_IMAGES
    } else {
      Manifest.permission.READ_EXTERNAL_STORAGE
    },
    onPermissionGranted = {
      // 權限授予後顯示的內容
      DevicePhotosApp()
    },
    onPermissionDenied = {
      // 權限被拒絕後的提示內容
      PermissionDenied("無圖片存取權限")
    }
  )
}

@Composable
fun DevicePhotosApp() {
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
  val mediaViewModel: MediaViewModel = viewModel(factory = MediaViewModel.Factory)
  var isVisibilityMode by remember { mutableStateOf(false) }
  // 顯示主畫面內容
  Scaffold (
    modifier = Modifier
      //.fillMaxSize()
      .nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      DeviceTopAppBar(
        scrollBehavior = scrollBehavior,
        onMenuOptionSelected = { option ->
          if (option == "toggleVisibility") {
            isVisibilityMode = !isVisibilityMode
          }
        }
      )
    }
  ) {
    var isRefreshing by remember { mutableStateOf(false) }
    val state = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()

    val onRefresh: () -> Unit = {
      isRefreshing = true
      coroutineScope.launch {
        try {
          mediaViewModel.refreshDevicePhotos() // 確保 loadDevicePhotos 包含異步處理，coroutineScope起點
        } catch (e: Exception) {
          // 這裡可以加入日誌或錯誤提示處理
          Log.e("DevicePhotosApp", "Error during refresh: ${e.message}")
        } finally {
          delay(Duration.ofMillis(10))
          isRefreshing = false // 確保在成功或失敗後都能停止刷新指示
        }
      }
    }

    PullToRefreshBox(
      modifier = Modifier.padding(it),
      state = state,
      isRefreshing = isRefreshing,
      onRefresh = onRefresh,
    ) {
      HomeScreen(
        viewModel = mediaViewModel,
        retryAction = {
          coroutineScope.launch {
            mediaViewModel.loadDevicePhotos()
          }
        },
        contentPadding = it,
        isVisibilityMode = isVisibilityMode
      )
    }
  }
}

@Composable
fun PermissionDenied(Msg: String, modifier: Modifier = Modifier) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
  ) {
    Text(text = Msg)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceTopAppBar(
  scrollBehavior: TopAppBarScrollBehavior,
  modifier: Modifier = Modifier,
  onMenuOptionSelected: (String) -> Unit
) {

  var showMenu by remember { mutableStateOf(false) }
  CenterAlignedTopAppBar(
    modifier = modifier,
    scrollBehavior = scrollBehavior,
    expandedHeight = 50.dp,
    title = {
//      Text(
//        text = "Device Photos",
//        style = MaterialTheme.typography.headlineSmall,
//        maxLines = 1,
//      )
    },
    actions = {
      IconButton(onClick = { showMenu = !showMenu }) {
        Icon(
          imageVector = Icons.Filled.Menu,
          contentDescription = "Localized description"
        )
      }
      // DropdownMenu 顯示選單項目
      DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
      ) {
        DropdownMenuItem(
          text = { Text("隱藏/顯示") },
          onClick = {
            onMenuOptionSelected("toggleVisibility")
            showMenu = false
          }
        )
        DropdownMenuItem(
          text = { Text("選擇圖庫") },
          onClick = {
            // 選擇圖庫功能
            showMenu = false
          }
        )
        DropdownMenuItem(
          text = { Text("登出") },
          onClick = {
            // 登出功能
            showMenu = false
          }
        )
      }
    }
  )
}

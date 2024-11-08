package com.example.gridmuse.ui.screens

import android.app.Activity
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun PermissionHandler(
  permission: String,
  onPermissionGranted: @Composable () -> Unit,
  onPermissionDenied: @Composable () -> Unit = {}
) {
  val context = LocalContext.current as Activity
  var hasPermission by remember { mutableStateOf(false) }
  var shouldRefresh by remember { mutableStateOf(false) } // 新增狀態控制變數

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
    hasPermission = isGranted
    if (isGranted) {
      shouldRefresh = true // 設置為 true 以刷新畫面
    } else {
      Toast.makeText(context, "需要存取權限以讀取照片", Toast.LENGTH_SHORT).show()
    }
  }

  // 檢查權限
  LaunchedEffect(Unit) {
    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
      hasPermission = true
    } else {
      permissionLauncher.launch(permission)
    }
  }

  // 重新組合畫面以刷新
  if (shouldRefresh) {
    hasPermission = true // 確保重新進入授權後邏輯
    shouldRefresh = false // 重置刷新狀態
  }

  if (hasPermission) {
    onPermissionGranted()
  } else {
    onPermissionDenied()
  }
}

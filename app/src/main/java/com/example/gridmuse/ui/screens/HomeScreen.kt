package com.example.gridmuse.ui.screens

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.example.gridmuse.R
import com.example.gridmuse.model.DevicePhoto

/**
 * The home screen displaying the result photos.
 */
@Composable
fun HomeScreen(
//  mediaUiState: MediaUiState,
  viewModel: MediaViewModel,
  retryAction: () -> Unit,
  modifier: Modifier = Modifier.background(color = Color.Black),
  contentPadding: PaddingValues = PaddingValues(0.dp),
) {
  val mediaUiState by viewModel.mediaUiState.collectAsStateWithLifecycle() //觀察Flow變化
  val devicePhotos = viewModel.devicePhotos.collectAsStateWithLifecycle()

  Box(modifier = Modifier.fillMaxSize()) {
    // 顯示照片網格
    PhotosGridScreen(devicePhotos.value, viewModel, modifier)

    // 如果是 Loading 狀態，顯示半透明的覆蓋層
    if (mediaUiState is MediaUiState.Loading) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
      ) {
        // 你可以使用一個進度指示器或者自定義的圖片
        CircularProgressIndicator(color = Color.White)
      }
    }
    // 如果是 Error 狀態，顯示錯誤畫面
    if (mediaUiState is MediaUiState.Error) {
      ErrorScreen(retryAction, modifier = Modifier.fillMaxSize())
    }
  }
}

@Composable
fun PhotoCard(
  photo: DevicePhoto,
  viewModel: MediaViewModel,
  modifier: Modifier = Modifier.fillMaxSize()
) {
  val context = LocalContext.current
  val vibrator = context.getSystemService(Vibrator::class.java)
  val isSelected = viewModel.selectedSort == photo.sort
  val isHighlighted = viewModel.selectedSort != null && !isSelected
  val isLocked = photo.sort == 0
  var showMenu by remember { mutableStateOf(false) }

  val imageModifier = Modifier.fillMaxSize()
    .then(
      when{
        isLocked && viewModel.selectedSort != null -> Modifier.graphicsLayer { alpha = 2f }
        isSelected -> Modifier.graphicsLayer { alpha = 1f }  // 被選中的照片半透明
        isHighlighted -> Modifier.graphicsLayer { alpha = 0.5f }  // 其他照片高亮
        else -> Modifier  // 無狀態時不應用效果
      }
    )
  val placeholderColor = remember { randomGrayColor() }
  Box(
    modifier = Modifier
      .pointerInput(Unit) {
        detectTapGestures(
          onLongPress = {
            if(!isLocked) {
              vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.EFFECT_TICK))
              viewModel.setSelectedSort(photo.sort)
            }
          },
          onTap = {
            if(viewModel.selectedSort != null && !isLocked ) {
              vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.EFFECT_TICK))
              //viewModel.swapWithSelectedSort(photo.sort)  // 點擊時與選中的 sort 值交換
              showMenu = true
            }
          }
        )
      }
  ) {
    AsyncImage(
      model = ImageRequest.Builder(context = LocalContext.current)
        .data(photo.imgSrc)
        .build(),
      error = painterResource(R.drawable.ic_broken_image),
      placeholder = ColorPainter(placeholderColor),
      contentDescription = stringResource(R.string.grid_muse),
      contentScale = ContentScale.Crop,
      modifier = imageModifier
        .aspectRatio(1f)
        .padding(1.dp)
    )
    // 使用選項對話框
    if(showMenu) {
      SelectPhotoCardAction(
        expanded = showMenu,
        onDismissRequest = {
          viewModel.selectedSort = null
          showMenu = false
        },
        onSwapSelected = {
          vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.EFFECT_TICK))
          viewModel.swapWithSelectedSort(photo.sort)
        },
        onInsertSelected = {
          vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.EFFECT_TICK))
          viewModel.insertAtSelectedSort(photo.sort)
        },
        offset = DpOffset(0.dp, 0.dp)
      )
    }

    Text(
      modifier = Modifier
        .fillMaxWidth(),
      text = photo.sort.toString(),
      textAlign = TextAlign.Center
    )
  }
}

@Composable
fun SelectPhotoCardAction(
  expanded: Boolean,
  onDismissRequest: () -> Unit,
  onSwapSelected: () -> Unit,
  onInsertSelected: () -> Unit,
  modifier: Modifier = Modifier,
  offset: DpOffset = DpOffset(0.dp, 0.dp)
) {
  DropdownMenu(
    expanded = expanded,
    onDismissRequest = onDismissRequest,
    modifier = modifier,
    offset = offset
  ) {
    DropdownMenuItem(
      onClick = {
        onSwapSelected()
        onDismissRequest()
      },
      text = { Text("交換") }
    )
    DropdownMenuItem(
      onClick = {
        onInsertSelected()
        onDismissRequest()
      },
      text = { Text("插入") }
    )
  }
}

@Composable
fun PhotosGridScreen(
  photos: List<DevicePhoto>,
  viewModel: MediaViewModel,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(top = 0.dp)
) {
  val visiblePhotos = photos.filter { !it.isHidden }
  LazyVerticalGrid(
    //columns = GridCells.Adaptive(128.dp),
    columns = GridCells.Fixed(3),
    modifier = modifier,
    contentPadding = contentPadding,
  ) {
    items(items = visiblePhotos, key = { photo -> photo.id }) { photo ->
      //println("D123_photo_hide: "+photo.isHidden)
      PhotoCard(
        photo,
        viewModel,
        modifier = modifier
          .fillMaxSize()
          .aspectRatio(1.5f)
      )
    }
  }
}

fun randomGrayColor(): Color {
  val randomValue = (0..255).random() // 隨機生成 0 到 255 之間的值
  return Color(randomValue, randomValue, randomValue) // 生成灰階顏色
}

@Composable
fun ErrorScreen(retryAction: () -> Unit, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Image(
      painter = painterResource(id = R.drawable.ic_connection_error), contentDescription = ""
    )
    Text(text = stringResource(R.string.loading_failed), modifier = Modifier.padding(16.dp))
    Button(onClick = retryAction) {
      Text(stringResource(R.string.retry))
    }
  }
}

@Composable
fun ResultScreen(photos: String, modifier: Modifier = Modifier) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
  ) {
    Text(text = photos)
  }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
  Image(
    modifier = modifier.size(200.dp),
    painter = painterResource(R.drawable.loading_img),
    contentDescription = stringResource(R.string.loading)
  )
}

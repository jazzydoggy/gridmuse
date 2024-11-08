package com.example.gridmuse

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.gridmuse.ui.PhotosAppOpener
import com.example.gridmuse.ui.theme.GridMuseTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    setContent {
      GridMuseTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
        ) {
          PhotosAppOpener()
        }
      }
    }
  }
  override fun onResume() {
    super.onResume()
  }
}

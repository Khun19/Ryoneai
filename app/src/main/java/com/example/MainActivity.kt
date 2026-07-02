package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.ui.AssistantScreen
import com.example.ui.AssistantViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private lateinit var viewModel: AssistantViewModel

  private val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.CALL_PHONE,
    Manifest.permission.READ_CONTACTS
  )

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Retrieve ViewModel on startup so we can interact with it across lifecycle/callbacks
    viewModel = ViewModelProvider(this)[AssistantViewModel::class.java]

    // Request necessary permissions on startup
    if (!hasRequiredPermissions()) {
      ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
    }

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          AssistantScreen(
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == PERMISSIONS_REQUEST_CODE) {
      val recordAudioIndex = permissions.indexOf(Manifest.permission.RECORD_AUDIO)
      if (recordAudioIndex != -1 && grantResults.getOrNull(recordAudioIndex) == PackageManager.PERMISSION_GRANTED) {
        if (::viewModel.isInitialized) {
          viewModel.onPermissionsGranted()
        }
      }
    }
  }

  private fun hasRequiredPermissions(): Boolean {
    return REQUIRED_PERMISSIONS.all { permission ->
      ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
  }

  companion object {
    private const val PERMISSIONS_REQUEST_CODE = 1001
  }
}

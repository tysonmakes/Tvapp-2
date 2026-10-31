package com.tysonmakes.tvremoteapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tysonmakes.tvremoteapp.ui.TvRemoteScreen
import com.tysonmakes.tvremoteapp.ui.TvRemoteViewModel
import com.tysonmakes.tvremoteapp.ui.theme.TvRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TvRemoteTheme {
                val viewModel: TvRemoteViewModel = viewModel()
                TvRemoteScreen(viewModel = viewModel)
            }
        }
    }
}

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.JobApplicationRepository
import com.example.ui.RecruitmentApp
import com.example.ui.RecruitmentViewModel
import com.example.ui.RecruitmentViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val database by lazy { AppDatabase.getDatabase(applicationContext) }
  private val repository by lazy { JobApplicationRepository(database.jobApplicationDao()) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: RecruitmentViewModel = viewModel(
          factory = RecruitmentViewModelFactory(repository)
        )
        RecruitmentApp(viewModel = viewModel)
      }
    }
  }
}

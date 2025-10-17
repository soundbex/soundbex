package com.soundbex.soundbex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.soundbex.soundbex.ui.screens.HomeScreen
import com.soundbex.soundbex.ui.screens.LibraryScreen
import com.soundbex.soundbex.ui.screens.SearchScreen
import com.soundbex.soundbex.ui.theme.SoundbexTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoundbexApp()
        }
    }
}

@Composable
fun SoundbexApp() {
    SoundbexTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = "home"
            ) {
                composable("home") {
                    HomeScreen(
                        onSearchClick = { navController.navigate("search") },
                        onLibraryClick = { navController.navigate("library") }
                    )
                }
                composable("search") {
                    SearchScreen(onBackClick = { navController.popBackStack() })
                }
                composable("library") {
                    LibraryScreen(onBackClick = { navController.popBackStack() })
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SoundbexAppPreview() {
    SoundbexTheme {
        SoundbexApp()
    }
}
package com.example.rinasystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.rinasystem.ui.navigation.AriaNavGraph
import com.example.rinasystem.ui.theme.RinaSystemTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RinaSystemTheme {
                val navController = rememberNavController()
                AriaNavGraph(navController = navController)
            }
        }
    }
}

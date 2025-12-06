package com.example.maping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.maping.screens.* // Importa las pantallas que creamos
import com.example.maping.ui.theme.MapIngTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MapIngTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // --- CAMBIA ESTO PARA PROBAR DIFERENTES PANTALLAS ---
                    LoginScreen()
                    // MainMapScreen()
                    // UploadPostScreen()
                    // PlaceDetailScreen()
                    // ProfileScreen()
                }
            }
        }
    }
}
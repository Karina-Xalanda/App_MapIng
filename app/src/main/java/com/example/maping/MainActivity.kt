package com.example.maping

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.os.Build // Importación necesaria para el chequeo de versión
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.maping.screens.*
import com.example.maping.ui.theme.MapIngTheme
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


import androidx.navigation.compose.navArgument // Para usar argumentos en la navegación
class MainActivity : ComponentActivity() {

    // Función auxiliar para extraer datos de texto NDEF del Intent
    private fun getNfcDataFromIntent(intent: Intent?): String? {
        if (intent == null || intent.action != NfcAdapter.ACTION_NDEF_DISCOVERED) return null

        // Manejo de 'getParcelableArrayExtra' deprecado (Línea 32 corregida)
        val rawMessages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, android.os.Parcelable::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        }

        if (rawMessages != null && rawMessages.isNotEmpty()) {
            val message = rawMessages[0] as? NdefMessage
            val record = message?.records?.firstOrNull()

            if (record != null && record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT)) {
                return try {
                    val payload = record.payload
                    // Lógica estándar para decodificar un registro NDEF de texto
                    val textEncoding = if ((payload[0].toInt() and 0x80) == 0) StandardCharsets.UTF_8 else StandardCharsets.UTF_16
                    val languageCodeLength = payload[0].toInt() and 0x3f
                    String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, textEncoding)
                } catch (e: Exception) {
                    "Error al leer el NDEF: ${e.message}"
                }
            }
        }
        return "Tag detectado, pero sin contenido de texto simple NDEF."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //  Verificar si el Activity fue iniciado por un Intent de NFC
        val nfcData = getNfcDataFromIntent(intent)
        // Definir la ruta de inicio. Si hay datos NFC, vamos a NfcDetail.
        val startRoute = if (nfcData != null) {
            // Codificamos la data para que pueda ser pasada como argumento en la ruta
            val encodedData = URLEncoder.encode(nfcData, StandardCharsets.UTF_8.toString())
            AppScreen.NfcDetail.createRoute(encodedData)
        } else {
            // Si no hay NFC, la ruta normal es Login
            AppScreen.Login.route
        }

        setContent {
            MapIngTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
// Configuramos el NavHost con la ruta de inicio dinámica
                    NavHost(navController = navController, startDestination = startRoute) {

                        // 1. LOGIN
                        composable(AppScreen.Login.route) {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate(AppScreen.Map.route) {
                                        popUpTo(AppScreen.Login.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 2. MAPA PRINCIPAL
                        composable(AppScreen.Map.route) {
                            MainMapScreen(
                                onNavigateToUpload = { navController.navigate(AppScreen.Upload.route) },
                                onNavigateToProfile = { navController.navigate(AppScreen.Profile.route) },
                                onNavigateToDetail = { postId ->
                                    // Navegamos pasando el ID
                                    navController.navigate(AppScreen.Detail.createRoute(postId))
                                }
                            )
                        }

                        // 3. SUBIR PUBLICACIÓN
                        composable(AppScreen.Upload.route) {
                            UploadPostScreen(
                                onPostUploaded = { navController.popBackStack() }
                            )
                        }

                        // 4. PERFIL
                        composable(AppScreen.Profile.route) {
                            ProfileScreen(
                                onLogout = {
                                    navController.navigate(AppScreen.Login.route) {
                                        popUpTo(AppScreen.Map.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 5. DETALLE DEL LUGAR
                        composable(
                            route = AppScreen.Detail.route,
                            arguments = listOf(navArgument("postId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val postId = backStackEntry.arguments?.getString("postId") ?: "Desconocido"
                            PlaceDetailScreen(
                                postId = postId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 6. NFC DETAIL
                        composable(
                            route = AppScreen.NfcDetail.route,
                            arguments = listOf(navArgument("tagData") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val tagData = backStackEntry.arguments?.getString("tagData") ?: "Error"
                            val decodedData = remember(tagData) {
                                URLDecoder.decode(tagData, StandardCharsets.UTF_8.toString())
                            }
                            NfcDetailScreen(
                                tagData = decodedData,
                                onNavigateBack = {
                                    navController.navigate(AppScreen.Login.route) {
                                        popUpTo(AppScreen.NfcDetail.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Sobrescribir onNewIntent para manejar la lectura NFC mientras la Activity ya está activa
    // Se corrige la firma de la función (Línea 142 corregida)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val nfcData = getNfcDataFromIntent(intent)
        if (nfcData != null) {
            // Si se detecta un nuevo tag, actualizamos el intent y forzamos la recreación
            this.intent = intent
            recreate()
        }
    }
}
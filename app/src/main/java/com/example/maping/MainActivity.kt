package com.example.maping

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
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
import com.google.gson.Gson

// CLASE DE DATOS PARA EL JSON DEL NFC
data class NfcData(
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double
)

class MainActivity : ComponentActivity() {

    // Función  para extraer datos JSON del NFC
    private fun getNfcDataFromIntent(intent: Intent?): String? {
        if (intent == null || intent.action != NfcAdapter.ACTION_NDEF_DISCOVERED) return null

        val rawMessages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, android.os.Parcelable::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        }

        if (rawMessages != null && rawMessages.isNotEmpty()) {
            val message = rawMessages[0] as? NdefMessage
            val record = message?.records?.firstOrNull()

            if (record != null && record.tnf == NdefRecord.TNF_MIME_MEDIA &&
                record.type.contentEquals("application/json".toByteArray(StandardCharsets.US_ASCII))) {

                return try {
                    val jsonBytes = record.payload
                    val jsonString = String(jsonBytes, StandardCharsets.UTF_8)

                    val gson = Gson()
                    val nfcDataObject = gson.fromJson(jsonString, NfcData::class.java)

                    val formattedData = "${nfcDataObject.name}|||${nfcDataObject.description}|||${nfcDataObject.latitude},${nfcDataObject.longitude}"
                    return formattedData

                } catch (e: Exception) {
                    return "Error al parsear el JSON: ${e.message}. El tag debe contener: {\"name\": \"...\", \"description\": \"...\", \"latitude\": 0.0, \"longitude\": 0.0}"
                }
            }
        }
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val nfcData = getNfcDataFromIntent(intent)
        val initialNfcDataEncoded = if (nfcData != null) {
            URLEncoder.encode(nfcData, StandardCharsets.UTF_8.toString())
        } else {
            null
        }

        val startRoute = AppScreen.Login.route

        setContent {
            MapIngTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()

                    // Solo navega si hay datos NFC y la ruta es válida.
                    LaunchedEffect(initialNfcDataEncoded) {
                        if (initialNfcDataEncoded != null && navController.currentDestination?.route == startRoute) {
                            navController.navigate(AppScreen.NfcDetail.createRoute(initialNfcDataEncoded)) {
                                popUpTo(AppScreen.Login.route) { inclusive = true }
                            }
                        }
                    }

                    NavHost(navController = navController, startDestination = startRoute) {

                        // LOGIN
                        composable(AppScreen.Login.route) {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate(AppScreen.Map.route) {
                                        popUpTo(AppScreen.Login.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // MAPA PRINCIPAL
                        composable(AppScreen.Map.route) {
                            MainMapScreen(
                                onNavigateToUpload = { navController.navigate(AppScreen.Upload.route) },
                                onNavigateToProfile = { navController.navigate(AppScreen.Profile.route) },
                                onNavigateToDetail = { postId ->
                                    navController.navigate(AppScreen.Detail.createRoute(postId))
                                }
                            )
                        }

                        // SUBIR PUBLICACIÓN
                        composable(AppScreen.Upload.route) {
                            UploadPostScreen(
                                onPostUploaded = { navController.popBackStack() }
                            )
                        }

                        // PERFIL
                        composable(AppScreen.Profile.route) {
                            ProfileScreen(
                                onLogout = {
                                    navController.navigate(AppScreen.Login.route) {
                                        popUpTo(AppScreen.Map.route) { inclusive = true }
                                    }
                                },
                                onNavigateToFindFriends = { navController.navigate(AppScreen.FindFriends.route) }
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
                            val tagData = backStackEntry.arguments?.getString("tagData") ?: "Error al leer Tag"
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

                        // 7. BÚSQUEDA DE AMIGOS
                        composable(AppScreen.FindFriends.route) {
                            FindFriendScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
    }
}
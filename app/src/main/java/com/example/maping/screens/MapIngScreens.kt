package com.example.maping.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.maping.R // Asegúrate de que esto importe tus recursos
import com.example.maping.ui.theme.InstitutionalGreen

// -----------------------
// 1. PANTALLA DE INICIO DE SESIÓN
// Referencia: PDF Página 1
// -----------------------
@Composable
fun LoginScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo Placeholder
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(InstitutionalGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("MapIng", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Inicio de sesión", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = "", onValueChange = {},
            label = { Text("Correo Electrónico") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = "", onValueChange = {},
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(containerColor = InstitutionalGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Iniciar sesión")
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar", color = InstitutionalGreen)
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            // Icono de Google simulado
            Icon(Icons.Default.AccountCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Continuar con Google", color = Color.Black)
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = {}) {
            Text("¿Olvidaste tu contraseña?", color = Color.Gray)
        }
    }
}

// -----------------------
// 2. PANTALLA DEL MAPA PRINCIPAL
// Referencia: PDF Página 2
// -----------------------
@Composable
fun MainMapScreen() {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* Navegar a subir foto */ },
                containerColor = InstitutionalGreen,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Subir foto")
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Header
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Mapa", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Facultad de Ingeniería", fontSize = 16.sp)
                Text("UV", fontSize = 16.sp, color = Color.Gray)
            }

            // Placeholder del Mapa
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text("Vista de Mapa Aquí\n(Integración Google Maps)", textAlign = TextAlign.Center)
                // Aquí irían los pines verdes
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = InstitutionalGreen, modifier = Modifier.size(48.dp).offset(x= (-50).dp, y= (-50).dp))
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = InstitutionalGreen, modifier = Modifier.size(48.dp).offset(x= 50.dp, y= 20.dp))
            }
        }
    }
}

// -----------------------
// 3. PANTALLA DE SUBIR PUBLICACIÓN
// Referencia: PDF Página 3
// -----------------------
@Composable
fun UploadPostScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Publicación", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = InstitutionalGreen)

        Spacer(modifier = Modifier.height(24.dp))

        // Área de Imagen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.LightGray, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                Text("Selecciona una imagen", color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(containerColor = InstitutionalGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tomar foto")
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = "",
            onValueChange = {},
            label = { Text("Agrega un comentario:") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            singleLine = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Detalles de ubicación:", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Text("Facultad de ingeniería\nSalon #", color = Color.Gray, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(containerColor = InstitutionalGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Subir publicación")
        }
    }
}

// -----------------------
// 4. PANTALLA DETALLE DEL LUGAR
// Referencia: PDF Página 4
// -----------------------
@Composable
fun PlaceDetailScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Detalle del Lugar", fontSize = 22.sp, color = InstitutionalGreen, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(16.dp))

        // Imagen del lugar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(Color.LightGray, RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Laboratorio de robotica", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Esta muy bien equipado, se encuentra entre el edificio N y L, en planta baja.",
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Subido por @user123", color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.FavoriteBorder, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Me gusta", color = Color.Black)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Text("Comentar", color = Color.Black)
        }
    }
}

// -----------------------
// 5. PANTALLA DE PERFIL
// Referencia: PDF Página 5
// -----------------------
@Composable
fun ProfileScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Perfil", fontSize = 22.sp, color = InstitutionalGreen, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Color.LightGray, CircleShape)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("@usuario123", fontWeight = FontWeight.Bold, fontSize = 18.sp)

        Spacer(modifier = Modifier.height(24.dp))

        // Estadísticas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("6", "Publicaciones")
            StatItem("48", "Likes")
            StatItem("12", "Visitados")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Grid de fotos
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(9) {
                Box(modifier = Modifier
                    .aspectRatio(1f)
                    .background(Color.LightGray, RoundedCornerShape(4.dp)))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(containerColor = InstitutionalGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar sesión")
        }
    }
}

@Composable
fun StatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}
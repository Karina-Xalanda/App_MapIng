package com.example.maping.screens


import android.annotation.SuppressLint
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.maping.ui.theme.InstitutionalGreen
import com.example.maping.viewmodel.AuthViewModel
import com.example.maping.viewmodel.UserState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
// imports para dibujar marcadores en el mapa
import com.example.maping.viewmodel.MapViewModel
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
// otros imports
import com.example.maping.viewmodel.UploadViewModel
import com.example.maping.viewmodel.PostUploadState
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast // necesario para mostrar el mensaje de exito
import com.example.maping.viewmodel.ProfileViewModel
import com.example.maping.viewmodel.DetailViewModel // NUEVA LÍNEA
import com.google.firebase.auth.ktx.auth // NUEVA LÍNEA
import com.google.firebase.ktx.Firebase // NUEVA LÍNEA
import com.example.maping.model.Comment
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardOptions

// -----------------------
// 1. PANTALLA DE INICIO DE SESIÓN
// -----------------------
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val userState by viewModel.userState.collectAsState()

    // Configuración de Google
    val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("888575070102-2b0j2lo66aub1j5596i1nt2p7clnlc11.apps.googleusercontent.com") // ID CLIENTE WEB
        .requestEmail()
        .build()

    val googleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            viewModel.signInWithGoogle(task)
        }
    }

    // Efecto de navegación si el login es exitoso
    LaunchedEffect(userState) {
        if (userState is UserState.Success) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
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
        OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Text("Registrar", color = InstitutionalGreen)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón Google Real
        OutlinedButton(
            onClick = { launcher.launch(googleSignInClient.signInIntent) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Continuar con Google", color = Color.Black)
        }

        // Mensaje de error
        if (userState is UserState.Error) {
            Text(
                text = (userState as UserState.Error).message,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- PUERTA TRASERA (SOLO PARA DESARROLLO) ---
        Button(
            onClick = onLoginSuccess,
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("⚡ Entrar como Desarrollador (Bypass) ⚡")
        }
    }
}

// -----------------------
// 2. PANTALLA DEL MAPA PRINCIPAL (MODIFICADA)
// -----------------------
@Composable
fun MainMapScreen(
    viewModel: MapViewModel = viewModel(),
    onNavigateToUpload: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val posts by viewModel.posts.collectAsState()

    // Coordenadas iniciales (FIEE UV)
    val fieeLocation = LatLng(19.1673, -96.1216)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(fieeLocation, 16f)
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToUpload,
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Mapa", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Facultad de Ingeniería UV", fontSize = 16.sp, color = Color.Gray)
                }
                IconButton(onClick = onNavigateToProfile) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Perfil", tint = InstitutionalGreen, modifier = Modifier.size(40.dp))
                }
            }

            // Mapa de Google
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false)
                ) {
                    posts.forEach { post ->
                        Marker(
                            state = MarkerState(position = LatLng(post.latitude, post.longitude)),
                            title = post.comment,
                            snippet = "Toca para ver detalles",
                            onInfoWindowClick = {

                                onNavigateToDetail(post.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

// -----------------------
// 3. PANTALLA DE SUBIR PUBLICACIÓN (CON GPS)
// -----------------------
@Composable
fun UploadPostScreen(
    viewModel: UploadViewModel = viewModel(),
    onPostUploaded: () -> Unit
) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var comment by remember { mutableStateOf("") }

    // Observar el estado de subida
    val uploadState by viewModel.postState.collectAsState()

    // Variables para el GPS
    var locationText by remember { mutableStateOf("Buscando ubicación GPS...") }
    var coordinates by remember { mutableStateOf<LatLng?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // logica para obtener la ubicación
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                @SuppressLint("MissingPermission")
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        coordinates = LatLng(location.latitude, location.longitude)
                        locationText = "Lat: ${location.latitude}, Long: ${location.longitude}"
                    } else {
                        locationText = "GPS activado, pero sin señal."
                    }
                }
            }
        } else {
            locationText = "Permiso de ubicación denegado."
        }
    }

    // Pedir permiso al entrar
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    // Fin de la logica de ubicacion


    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    // EFECTO PARA MANEJAR EL ESTADO DE LA SUBIDA ---
    LaunchedEffect(uploadState) {
        when (uploadState) {
            is PostUploadState.Success -> {
                Toast.makeText(context, "Publicación subida con éxito!", Toast.LENGTH_LONG).show()
                viewModel.resetState()
                onPostUploaded()
            }
            is PostUploadState.Error -> {
                val message = (uploadState as PostUploadState.Error).message
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    // Determinar si la UI está bloqueada
    val isLoading = uploadState is PostUploadState.Loading

    // UI
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Publicación", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = InstitutionalGreen)
        Spacer(modifier = Modifier.height(24.dp))

        // Área de Imagen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.LightGray, RoundedCornerShape(12.dp))
                .clickable(enabled = !isLoading) { galleryLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = InstitutionalGreen, modifier = Modifier.size(64.dp))
                    Text("¡Imagen lista!", color = InstitutionalGreen, fontWeight = FontWeight.Bold)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                    Text("Toca para seleccionar imagen", color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { galleryLauncher.launch("image/*") },
            colors = ButtonDefaults.buttonColors(containerColor = InstitutionalGreen),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Abrir Galería")
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("Agrega un comentario...") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            singleLine = false,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mostrar estado de la subida
        if (isLoading) {
            val loadingMessage = (uploadState as PostUploadState.Loading).message
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = InstitutionalGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text(loadingMessage, color = InstitutionalGreen, fontSize = 12.sp)
            }
        } else {
            // estado del GPS
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = if(coordinates!=null) InstitutionalGreen else Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text(locationText, color = Color.Gray, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            //CONEXION DEL BOTON ---
            onClick = {
                imageUri?.let { uri ->
                    coordinates?.let { coords ->
                        viewModel.uploadPost(
                            photoUri = uri,
                            lat = coords.latitude,
                            lng = coords.longitude,
                            comment = comment
                        )
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = InstitutionalGreen),
            modifier = Modifier.fillMaxWidth(),
            // Habilitado solo si hay imagen, GPS y NO está cargando
            enabled = (imageUri != null && coordinates != null && !isLoading)
        ) {
            Text(if (isLoading) "Subiendo..." else "Subir publicación")
        }
    }
}



// -----------------------
// 4. PANTALLA DETALLE DEL LUGAR (MODIFICADA: AHORA DINÁMICA CON LIKES Y COMENTARIOS)
// -----------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailScreen(
    postId: String,
    onNavigateBack: () -> Unit,
    viewModel: DetailViewModel = viewModel() // INYECTAR VIEWMODEL
) {
    // Estados de datos
    val post by viewModel.post.collectAsState()
    val comments by viewModel.comments.collectAsState() // NUEVO: Estado de comentarios

    // Estado de la UI
    var commentInput by remember { mutableStateOf("") } // Estado del campo de comentario
    val currentUserId = remember { Firebase.auth.currentUser?.uid }

    // Cargar datos al iniciar
    LaunchedEffect(postId) {
        viewModel.fetchPostDetail(postId)
    }

    // Determinar el estado del like
    val isLiked = remember(post, currentUserId) {
        post?.likedBy?.contains(currentUserId) == true
    }

    // Lógica para enviar comentario
    val sendComment = {
        if (commentInput.isNotBlank()) {
            viewModel.addComment(postId, commentInput.trim())
            commentInput = "" // Limpiar el campo
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(post?.comment?.take(20) ?: "Cargando...", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = InstitutionalGreen)
            )
        }
    ) { padding ->
        // Si el post es nulo, mostrar un indicador de carga
        if (post == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = InstitutionalGreen)
            }
            return@Scaffold
        }

        val currentPost = post!!

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // SCROLL VIEW PARA EL CONTENIDO PRINCIPAL Y COMENTARIOS
            Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {

                // --- SECCIÓN DE PUBLICACIÓN ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .background(Color.LightGray, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Placeholder visual (sustituir con librería de imágenes en un proyecto real)
                    Text("Post: ${currentPost.comment.take(15)}...", color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(currentPost.comment, fontSize = 24.sp, fontWeight = FontWeight.Bold)

                Text("ID del Post: $postId", fontSize = 12.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(8.dp))
                Text("Esta muy bien equipado, se encuentra entre el edificio N y L, en planta baja.", fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                // Mostrar el UID del dueño del post
                Text("Subido por UID: ${currentPost.userId.take(6)}...", color = Color.Gray, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(24.dp))

                // BOTÓN LIKES DINÁMICO
                Button(
                    onClick = {
                        if (currentUserId != null) {
                            viewModel.toggleLike(currentPost.id, isLiked)
                        }
                    },
                    colors = if (isLiked) ButtonDefaults.buttonColors(containerColor = Color.Red) else ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isLiked) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Me gusta (${currentPost.likeCount})",
                        color = if (isLiked) Color.White else Color.Black
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // --- SECCIÓN DE COMENTARIOS ---
                Text("Comentarios (${comments.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))

                if (comments.isEmpty()) {
                    Text("Sé el primero en comentar.", color = Color.Gray, fontSize = 14.sp)
                } else {
                    comments.forEach { comment ->
                        CommentItem(comment)
                    }
                }
            }

            // --- CAJA DE COMENTARIOS (PARTE FIJA INFERIOR) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentInput,
                    onValueChange = { commentInput = it },
                    label = { Text("Añadir comentario...") },
                    modifier = Modifier.weight(1f).heightIn(min = 50.dp, max = 150.dp),
                    singleLine = false,
                    // Con las importaciones añadidas, la línea debe ser:
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = sendComment,
                    enabled = commentInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = InstitutionalGreen)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar")
                }
            }
        }
    }
}

// NUEVA FUNCIÓN COMPOSABLE: Estructura de un solo comentario
@Composable
fun CommentItem(comment: Comment) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("@${comment.username}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(comment.text, fontSize = 16.sp)
            // Opcional: mostrar la hora
            // Text(formatTimestamp(comment.timestamp), color = Color.Gray, fontSize = 10.sp)
        }
    }
}

// -----------------------
// 5. PANTALLA DE PERFIL (MODIFICADA: AHORA DINÁMICA)
// -----------------------
@Composable
fun ProfileScreen(
    // AÑADIR VIEWMODEL PARA DATOS DINÁMICOS
    viewModel: ProfileViewModel = viewModel(),
    onLogout: () -> Unit
) {
    // Recolectar datos del ViewModel en tiempo real
    val userProfile by viewModel.userProfile.collectAsState()
    val userPosts by viewModel.userPosts.collectAsState()

    // Manejar el cierre de sesión dentro del ViewModel
    val handleLogout = {
        viewModel.signOut(onLogout)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Perfil", fontSize = 22.sp, color = InstitutionalGreen, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        // Si el perfil no se ha cargado, mostrar un loading o un placeholder simple.
        userProfile?.let { user ->
            // Contenedor de la foto de perfil
            Box(
                modifier = Modifier.size(100.dp).background(Color.LightGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // NOTA: Para mostrar la imagen real de Google (user.profileImageUrl) se necesita
                // una librería de carga de imágenes (como Coil o Glide). Por ahora, usamos un icono.
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.White)

                // Muestra un indicador si la URL existe, aunque no carguemos la imagen
                if (user.profileImageUrl.isNotEmpty()) {
                    Text("📷", modifier = Modifier.align(Alignment.BottomEnd).clip(CircleShape).background(InstitutionalGreen).padding(4.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Nombre de usuario dinámico
            Text("@${user.username}", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            Spacer(modifier = Modifier.height(24.dp))

            // Stats dinámicos (usando los contadores del modelo User)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(user.postCount.toString(), "Publicaciones")
                StatItem(user.likeCount.toString(), "Likes")
                StatItem(user.visitedCount.toString(), "Visitados")
            }
        } ?: run {
            // Placeholder mientras carga o si hay un error
            CircularProgressIndicator(color = InstitutionalGreen)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Cargando perfil...")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Grid de publicaciones (se adapta al número real de posts)
        val postCount = userPosts.size

        if (postCount > 0) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(postCount) { index ->
                    // Las cajas ahora representan el número real de publicaciones.
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(Color.LightGray, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Muestra el comentario como placeholder del contenido
                        Text(
                            userPosts[index].comment.take(15) + "...", // Snippet del comentario
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        } else if (userProfile != null) {
            // Mensaje si no hay publicaciones y el perfil ya cargó
            Text(
                "¡Aún no tienes publicaciones!",
                color = Color.Gray,
                fontSize = 16.sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 48.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = handleLogout,
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

// -----------------------
// 6. PANTALLA DETALLE NFC (NUEVA)
// -----------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcDetailScreen(
    tagData: String,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Información NFC", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = InstitutionalGreen)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Nfc, // Icono NFC
                contentDescription = "NFC Tag",
                tint = InstitutionalGreen,
                modifier = Modifier.size(96.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "¡Tag NFC detectado!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = InstitutionalGreen
            )
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = InstitutionalGreen.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Contenido del Tag:", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(tagData, color = Color.Black, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Esta funcionalidad permite a los estudiantes obtener información sobre laboratorios, salones o puntos de interés al acercar su dispositivo a un tag NFC.",
                textAlign = TextAlign.Center,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}
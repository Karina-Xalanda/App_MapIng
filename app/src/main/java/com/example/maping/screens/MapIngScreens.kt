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
import androidx.compose.foundation.combinedClickable
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
import android.widget.Toast
import com.example.maping.viewmodel.ProfileViewModel
import com.example.maping.viewmodel.DetailViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.example.maping.model.Comment
import com.example.maping.model.Post
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.maping.model.User

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

//import para la img del logo
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.maping.R

import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.ui.text.font.FontStyle

// -----------------------
// 1. PANTALLA DE INICIO DE SESION
// -----------------------
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val userState by viewModel.userState.collectAsState()

    // Estados para capturar la entrada del usuario
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }

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

    //  navegacion si el login es exitoso
    LaunchedEffect(userState) {
        if (userState is UserState.Success) {
            email = ""
            password = ""
            username = ""
            isRegistering = false
            viewModel.resetState()
            onLoginSuccess()
        }
    }

    // Determina si la UI está bloqueada
    val isUiEnabled = userState != UserState.Loading

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
            Image(
                painter = painterResource(id = R.drawable.logo_maping),
                contentDescription = "Logo de MapIng",
                modifier = Modifier.size(80.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(if(isRegistering) "Registro de usuario" else "Inicio de sesión", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        //CAMPOS para registro de cuenta
        // nombre de usuario
        if (isRegistering) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Nombre de Usuario") },
                modifier = Modifier.fillMaxWidth(),
                enabled = isUiEnabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        //  Correo Electrónico
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo Electrónico") },
            modifier = Modifier.fillMaxWidth(),
            enabled = isUiEnabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(modifier = Modifier.height(8.dp))
        //  Contraseña
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            enabled = isUiEnabled,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Boton principal (Login o Registrar)
        Button(
            onClick = {
                if (isRegistering) {
                    viewModel.register(email, password, username)
                } else {
                    viewModel.signIn(email, password)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = InstitutionalGreen),
            modifier = Modifier.fillMaxWidth(),
            enabled = isUiEnabled && email.isNotBlank() && password.isNotBlank() && (!isRegistering || username.isNotBlank())
        ) {
            Text(if (isRegistering) "Completar Registro" else "Iniciar sesión")
        }

        Spacer(modifier = Modifier.height(8.dp))
        // Botón secundario (Alternar entre Login y Registro)
        OutlinedButton(
            onClick = {
                isRegistering = !isRegistering
                viewModel.resetState()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isUiEnabled
        ) {
            Text(if (isRegistering) "¿Ya tienes cuenta? Inicia sesión" else "Registrar nueva cuenta", color = InstitutionalGreen)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Separador visual
        Text("o", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))

        // Boton Google Real
        OutlinedButton(
            onClick = {
                if (isUiEnabled) {
                    launcher.launch(googleSignInClient.signInIntent)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isUiEnabled
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Continuar con Google", color = Color.Black)

            if (userState is UserState.Loading) {
                Spacer(modifier = Modifier.width(8.dp))
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = InstitutionalGreen)
            }
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
    }
}

// -----------------------
// 2. PANTALLA DEL MAPA PRINCIPAL
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

// ----------------------------------
// 3. PANTALLA DE SUBIR PUBLICACION
// ----------------------------------
@Composable
fun UploadPostScreen(
    viewModel: UploadViewModel = viewModel(),
    onPostUploaded: () -> Unit
) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var comment by remember { mutableStateOf("") }

   //URI para que la cámara guarde la foto
    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }

    //  estado de subida
    val uploadState by viewModel.postState.collectAsState()

    // Variables para el GPS
    var locationText by remember { mutableStateOf("Buscando ubicación GPS...") }
    var coordinates by remember { mutableStateOf<LatLng?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // LOGICA DE CAMARA
    // 1. Funcion para crear la URI temporal
    val createTempUri: () -> Uri = {
        val file = java.io.File(context.filesDir, "temp_photo_${System.currentTimeMillis()}.jpg")
        androidx.core.content.FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
    }

    // launcher para Tomar Foto
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraTempUri != null) {
            imageUri = cameraTempUri
        } else {
            cameraTempUri = null
        }
    }

    //  Funcion para ejecutar la camara
    val launchCamera = { uri: Uri ->
        cameraTempUri = uri
        cameraLauncher.launch(uri)
    }

    // launcher para solicitar el permiso de la cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCamera(createTempUri())
        } else {
            Toast.makeText(context, "Permiso de cámara denegado.", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        cameraTempUri = null
    }


    // obtener la ubicación
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

    // Pedir permiso de ubicación al entrar
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
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

        // Contenedor para ambos botones
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Botón ABRIR CÁMARA
            Button(
                onClick = {
                    // Verificar y pedir el permiso antes de abrir la cámara
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        launchCamera(createTempUri())
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = InstitutionalGreen),
                enabled = !isLoading,
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            ) {
                Icon(Icons.Default.Camera, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cámara")
            }

            // Botón ABRIR GALERÍA
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = InstitutionalGreen),
                enabled = !isLoading,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Galería")
            }
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
// 4. PANTALLA DETALLE DEL LUGAR
// -----------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailScreen(
    postId: String,
    onNavigateBack: () -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    val context = LocalContext.current
    // Estados de datos
    val post by viewModel.post.collectAsState()
    val comments by viewModel.comments.collectAsState()

    // Estado de la UI para comentarios
    var commentInput by remember { mutableStateOf("") }
    val currentUserId = remember { Firebase.auth.currentUser?.uid }

    // Estados para Edición de Comentario
    var commentToEdit by remember { mutableStateOf<Comment?>(null) }
    var editInput by remember { mutableStateOf("") }
    val showEditDialog = commentToEdit != null

    // Cargar datos al iniciar
    LaunchedEffect(postId) {
        viewModel.fetchPostDetail(postId)
    }

    // Determinar el estado del like
    val isLiked = remember(post, currentUserId) {
        post?.likedBy?.contains(currentUserId) == true
    }

    //  enviar comentario
    val sendComment = {
        if (commentInput.isNotBlank()) {
            viewModel.addComment(postId, commentInput.trim())
            commentInput = "" // Limpiar el campo
        }
    }

    //  guardar edición de comentario
    val saveEdit = {
        commentToEdit?.let { comment ->
            if (editInput.isNotBlank() && editInput != comment.text) {
                viewModel.editComment(comment.id, editInput.trim())
            }
            commentToEdit = null // Cerrar diálogo
            editInput = ""
        }
    }

    // dialogo de edicion de Comentario
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { commentToEdit = null },
            title = { Text("Editar Comentario") },
            text = {
                OutlinedTextField(
                    value = editInput,
                    onValueChange = { editInput = it },
                    label = { Text("Nuevo Comentario") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp, max = 150.dp),
                    singleLine = false
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        saveEdit()
                        Unit
                    },
                    enabled = editInput.isNotBlank()
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { commentToEdit = null }) {
                    Text("Cancelar")
                }
            }
        )
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
                AsyncImage(
                    model = currentPost.imageUrl,
                    contentDescription = currentPost.comment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(currentPost.comment, fontSize = 24.sp, fontWeight = FontWeight.Bold)

                Text("ID del Post: $postId", fontSize = 12.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(8.dp))

                // Botón para Borrar Publicación dueno del perfil
                if (currentUserId == currentPost.userId) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.deletePost(
                                postId = currentPost.id,
                                imageUrl = currentPost.imageUrl,
                                postOwnerId = currentPost.userId,
                                onPostDeleted = { onNavigateBack() }
                            )
                            Toast.makeText(context, "Eliminando publicación...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Borrar Publicación")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Borrar Publicación Propia")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }


                // UID del dueño del post
                Text("Subido por UID: ${currentPost.userId.take(6)}...", color = Color.Gray, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(24.dp))

                // BOTÓN LIKES
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
                        CommentItem(
                            comment = comment,
                            currentUserId = currentUserId,
                            onDelete = { commentId -> viewModel.deleteComment(commentId) },
                            onEdit = { editedComment ->
                                commentToEdit = editedComment
                                editInput = editedComment.text
                            }
                        )
                    }
                }
            }

            //  CAJA DE COMENTARIOS ---
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


// Estructura de un solo comentario, opciones de edición/borrado
@Composable
fun CommentItem(
    comment: Comment,
    currentUserId: String?,
    onDelete: (String) -> Unit,
    onEdit: (Comment) -> Unit
) {
    val isOwner = comment.userId == currentUserId
    val showMenu = remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("@${comment.username}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(comment.text, fontSize = 16.sp)
            }

            if (isOwner) {
                Box {
                    IconButton(onClick = { showMenu.value = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones de Comentario")
                    }
                    DropdownMenu(
                        expanded = showMenu.value,
                        onDismissRequest = { showMenu.value = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            onClick = {
                                onEdit(comment)
                                showMenu.value = false
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Borrar") },
                            onClick = {
                                onDelete(comment.id)
                                showMenu.value = false
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                        )
                    }
                }
            }
        }
    }
}

// -----------------------
// 5. PANTALLA DE PERFIL
// -----------------------
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onLogout: () -> Unit,
    onNavigateToFindFriends: () -> Unit
) {
    val context = LocalContext.current
    // Recolectar datos del ViewModel en tiempo real
    val userProfile by viewModel.userProfile.collectAsState()
    val userPosts by viewModel.userPosts.collectAsState()

    //diálogo de confirmación de borrado
    var postToDelete by remember { mutableStateOf<Post?>(null) }
    val showDeleteDialog = postToDelete != null

    // cierre de sesión dentro del ViewModel
    val handleLogout = {
        viewModel.signOut(onLogout)
    }

    //  Confirmación de Borrado de Post
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = { Text("Confirmar Borrado") },
            text = { Text("¿Estás seguro de que quieres eliminar esta publicación permanentemente? Esto no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        postToDelete?.let { post ->
                            viewModel.deletePost(post)
                            Toast.makeText(context, "Eliminando publicación...", Toast.LENGTH_SHORT).show()
                        }
                        postToDelete = null
                        Unit
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { postToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header del Perfil ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Perfil", fontSize = 22.sp, color = InstitutionalGreen, fontWeight = FontWeight.Bold)
            IconButton(onClick = onNavigateToFindFriends) { // BOTÓN DE AMIGOS
                Icon(Icons.Default.GroupAdd, contentDescription = "Buscar amigos", tint = InstitutionalGreen, modifier = Modifier.size(32.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        userProfile?.let { user ->
            Box(
                modifier = Modifier.size(100.dp).clip(CircleShape).background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                if (user.profileImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = user.profileImageUrl, // URL de la foto de Google
                        contentDescription = "Foto de Perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Mantiene el icono si no hay URL de foto
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Nombre de usuario dinámico
            Text("@${user.username}", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(user.postCount.toString(), "Publicaciones")
                StatItem(user.likeCount.toString(), "Likes")
                StatItem(user.friends.size.toString(), "Amigos")
            }
        } ?: run {
            CircularProgressIndicator(color = InstitutionalGreen)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Cargando perfil...")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (userPosts.isNotEmpty()) {
            Text("Mantén presionado para borrar una publicación.", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Grid de publicaciones
        val postCount = userPosts.size

        if (postCount > 0) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(postCount) { index ->
                    val post = userPosts[index]
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .combinedClickable(
                                onClick = { /* Podría navegar a detalle si se quisiera */ },
                                onLongClick = {
                                    postToDelete = post
                                }
                            )
                            .background(Color.LightGray, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = post.imageUrl,
                            contentDescription = post.comment,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        } else if (userProfile != null) {

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

/// -----------------------
// 6. PANTALLA DETALLE NFC
// -----------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcDetailScreen(
    tagData: String,
    onNavigateBack: () -> Unit
) {
    val parts = tagData.split("|||")

    val name = parts.getOrElse(0) { "Nombre no encontrado" }
    val description = parts.getOrElse(1) { "Descripción no encontrada" }
    val coordinatesString = parts.getOrElse(2) { "0.0,0.0" }

    val latLng = try {
        val coords = coordinatesString.split(",")
        val lat = coords.getOrElse(0) { "0.0" }.toDoubleOrNull() ?: 0.0
        val lng = coords.getOrElse(1) { "0.0" }.toDoubleOrNull() ?: 0.0
        LatLng(lat, lng)
    } catch (e: Exception) {
        LatLng(0.0, 0.0)
    }

    // Configuración del Mapa en el punto de interés
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(latLng, 17f)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Punto de Interés: $name", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = InstitutionalGreen)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Punto de Interés en MAPS
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f),
                cameraPositionState = cameraPositionState
            ) {
                Marker(
                    state = MarkerState(position = latLng),
                    title = name,
                    snippet = description,
                    visible = (latLng.latitude != 0.0 || latLng.longitude != 0.0)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3f)
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Título del Lugar
                Text(
                    text = name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Descripción
                Text(
                    text = description,
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Coordenadas
                Text(
                    text = "Coordenadas: ${latLng.latitude}, ${latLng.longitude}",
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}
// -----------------------
// 7. PANTALLA DE BÚSQUEDA DE USUARIOS/AMIGOS
// -----------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindFriendScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val searchResults by viewModel.searchResults.collectAsState()
    val currentUserProfile by viewModel.userProfile.collectAsState()
    val currentUserId = remember { Firebase.auth.currentUser?.uid }

    var searchText by remember { mutableStateOf("") }

    // Dispara la búsqueda cuando el texto cambia
    LaunchedEffect(searchText) {
        if (searchText.length > 2) {
            viewModel.searchUsers(searchText.trim())
        } else {
            viewModel.searchUsers("")
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Buscar Usuarios", color = Color.White) },
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
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Campo de Búsqueda
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Buscar por nombre de usuario...") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (searchText.length < 3) {
                Text("Ingresa al menos 3 caracteres para buscar.", color = Color.Gray, modifier = Modifier.padding(8.dp))
            } else if (searchResults.isEmpty() && searchText.length >= 3) {
                Text("No se encontraron usuarios con ese nombre.", color = Color.Gray, modifier = Modifier.padding(8.dp))
            } else {
                // Lista de Resultados
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults) { user ->
                        val isFriend = remember(currentUserProfile) {
                            currentUserProfile?.friends?.contains(user.uid) == true
                        }

                        UserSearchResultItem(
                            user = user,
                            isFriend = isFriend,
                            onToggleFriend = {
                                viewModel.toggleFriend(user.uid, isFriend)
                            }
                        )
                    }
                }
            }
        }
    }
}

//resultado de búsqueda--
@Composable
fun UserSearchResultItem(
    user: User,
    isFriend: Boolean,
    onToggleFriend: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Información del usuario
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(InstitutionalGreen.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    // Usar AsyncImage si hay URL
                    if (user.profileImageUrl.isNotEmpty()) {
                        AsyncImage(model = user.profileImageUrl, contentDescription = "Perfil de ${user.username}", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text("@${user.username}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Posts: ${user.postCount}", fontSize = 12.sp, color = Color.Gray)
                }
            }

            // Botón Añadir/Eliminar
            Button(
                onClick = onToggleFriend,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFriend) Color.Red else InstitutionalGreen
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(if (isFriend) "Eliminar" else "Añadir", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}
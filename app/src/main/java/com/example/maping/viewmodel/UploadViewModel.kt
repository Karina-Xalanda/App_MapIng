package com.example.maping.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.maping.model.Post // importamos el modelo Post
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue // IMPORTAR ESTA LÍNEA PARA EL INCREMENTO
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class UploadViewModel : ViewModel() {
    // instancias de firebase
    private val auth = Firebase.auth
    private val storage = Firebase.storage
    private val firestore = Firebase.firestore

    // Estado de Subida:se observa en la pantalla Compose
    private val _postState = MutableStateFlow<PostUploadState>(PostUploadState.Idle)
    val postState: StateFlow<PostUploadState> = _postState

    // funcion principal para subir la publicacion
    fun uploadPost(photoUri: Uri, lat: Double, lng: Double, comment: String) {
        viewModelScope.launch {
            _postState.value = PostUploadState.Loading("Iniciando subida...")

            val currentUserId = auth.currentUser?.uid ?: run {
                // Si el usuario no está autenticado, se detiene la subida
                _postState.value = PostUploadState.Error("Usuario no autenticado. Intente iniciar sesion de nuevo.")
                return@launch
            }

            try {
                // 1. Subir Imagen a Storage ---
                val fileName = "posts/${currentUserId}_${UUID.randomUUID()}.jpg"
                val photoRef = storage.reference.child(fileName)

                _postState.value = PostUploadState.Loading("Subiendo imagen...")
                val uploadTask = photoRef.putFile(photoUri).await()

                // obtener la URL publica para Firestore
                val imageUrl = photoRef.downloadUrl.await().toString()

                // 2. guardar Metadatos en Firestore ---
                _postState.value = PostUploadState.Loading("Guardando ubicación...")
                val newPost = Post(
                    userId = currentUserId,
                    imageUrl = imageUrl,
                    latitude = lat,
                    longitude = lng,
                    comment = comment,
                    timestamp = System.currentTimeMillis() // Usamos el timestamp para ordenar
                )

                // guardar en la coleccion "posts"
                firestore.collection("posts")
                    .add(newPost)
                    .await()

                // 3. ACTUALIZAR CONTADOR DEL USUARIO (NUEVA LÓGICA) ---
                _postState.value = PostUploadState.Loading("Actualizando perfil...")
                val userRef = firestore.collection("users").document(currentUserId)
                // Usamos FieldValue.increment(1) para aumentar el contador de forma segura
                userRef.update("postCount", FieldValue.increment(1)).await()
                // -----------------------------------------------------

                // exito ---
                _postState.value = PostUploadState.Success

            } catch (e: Exception) {
                Log.e("UploadViewModel", "Error al subir publicacion: ${e.message}", e)
                _postState.value = PostUploadState.Error("Error al subir: ${e.message}")
            }
        }
    }

    // Funcion para limpiar el estado despues de un exito o error
    fun resetState() {
        _postState.value = PostUploadState.Idle
    }
}

//estados posibles durante la subida
sealed class PostUploadState {
    object Idle : PostUploadState()
    data class Loading(val message: String) : PostUploadState()
    object Success : PostUploadState()
    data class Error(val message: String) : PostUploadState()
}
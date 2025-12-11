package com.example.maping.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.maping.model.Post
import com.example.maping.model.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val db = Firebase.firestore
    private val storage = Firebase.storage
    private val currentUserId = auth.currentUser?.uid

    // Estado para el perfil del usuario autenticado
    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile

    // Estado para las publicaciones hechas por este usuario
    private val _userPosts = MutableStateFlow<List<Post>>(emptyList())
    val userPosts: StateFlow<List<Post>> = _userPosts

    // NUEVO: Estado para resultados de búsqueda de usuarios
    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults

    init {
        if (currentUserId != null) {
            listenToUserProfile(currentUserId)
            listenToUserPosts(currentUserId)
        } else {
            Log.e("ProfileViewModel", "User is null. Cannot fetch profile data.")
        }
    }

    // Escucha el documento de perfil del usuario en tiempo real
    private fun listenToUserProfile(uid: String) {
        db.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ProfileViewModel", "Error al escuchar perfil del usuario", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    _userProfile.value = snapshot.toObject(User::class.java)
                }
            }
    }

    // Escucha las publicaciones del usuario en tiempo real
    private fun listenToUserPosts(uid: String) {
        db.collection("posts")
            .whereEqualTo("userId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ProfileViewModel", "Error al escuchar posts del usuario", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val postList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Post::class.java)
                    }
                    _userPosts.value = postList
                }
            }
    }

    // ✅ ACTUALIZADA: Borrar un post desde el perfil (maneja contadores)
    fun deletePost(post: Post) {
        viewModelScope.launch {
            try {
                val postRef = db.collection("posts").document(post.id)
                val userRef = db.collection("users").document(post.userId)

                // 1. Borrar la imagen de Storage
                if (post.imageUrl.isNotEmpty()) {
                    try {
                        val imageRef = storage.getReferenceFromUrl(post.imageUrl)
                        imageRef.delete().await()
                        Log.d("ProfileViewModel", "Imagen borrada de Storage")
                    } catch (e: Exception) {
                        Log.e("ProfileViewModel", "Error al borrar imagen: ${e.message}")
                    }
                }

                // 2. Transacción para actualizar contadores y borrar el post
                db.runTransaction { transaction ->
                    // Decrementar postCount en el perfil del usuario
                    transaction.update(userRef, "postCount", FieldValue.increment(-1))

                    // Actualizar el contador de likes si el post tenía likes
                    if (post.likeCount > 0) {
                        transaction.update(userRef, "likeCount", FieldValue.increment(-post.likeCount.toLong()))
                    }

                    // Eliminar el documento del post
                    transaction.delete(postRef)
                    null
                }.await()

                Log.d("ProfileViewModel", "Post borrado con éxito")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error al borrar post: ${e.message}")
            }
        }
    }


    // =====================================================================
    // FUNCIONES DE AMIGOS/BÚSQUEDA
    // =====================================================================

    fun searchUsers(query: String) {
        if (query.isBlank() || currentUserId == null) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                // Realiza una consulta para buscar usuarios por nombre de usuario (búsqueda aproximada)
                val endString = query + "\uf8ff" // truco para obtener todos los documentos que comienzan con 'query'

                val snapshot = db.collection("users")
                    .whereGreaterThanOrEqualTo("username", query)
                    .whereLessThanOrEqualTo("username", endString)
                    .limit(10)
                    .get()
                    .await()

                val results = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)
                }.filter { it.uid != currentUserId }

                _searchResults.value = results
                Log.d("ProfileViewModel", "Búsqueda exitosa, ${results.size} resultados.")

            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error al buscar usuarios: ${e.message}")
                _searchResults.value = emptyList()
            }
        }
    }

    // NUEVA FUNCIÓN: Añadir o eliminar amigo
    fun toggleFriend(friendUserId: String, isCurrentlyFriend: Boolean) {
        if (currentUserId == null || currentUserId == friendUserId) return

        viewModelScope.launch {
            val currentUserRef = db.collection("users").document(currentUserId)
            val friendUserRef = db.collection("users").document(friendUserId)

            try {
                if (isCurrentlyFriend) {
                    // Quitar amigo (arrayRemove)
                    currentUserRef.update("friends", FieldValue.arrayRemove(friendUserId)).await()
                    friendUserRef.update("friends", FieldValue.arrayRemove(currentUserId)).await()
                    Log.d("ProfileViewModel", "Amigo eliminado: $friendUserId")
                } else {
                    // Añadir amigo (arrayUnion)
                    currentUserRef.update("friends", FieldValue.arrayUnion(friendUserId)).await()
                    friendUserRef.update("friends", FieldValue.arrayUnion(currentUserId)).await()
                    Log.d("ProfileViewModel", "Amigo añadido: $friendUserId")
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error al cambiar estado de amistad: ${e.message}")
            }
        }
    }

    // Funcion para cerrar sesión
    fun signOut(onSuccess: () -> Unit) {
        auth.signOut()
        onSuccess()
    }
}
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

    // ✅ NUEVA FUNCIÓN: Borrar un post desde el perfil
    fun deletePost(post: Post) {
        viewModelScope.launch {
            try {
                val postRef = db.collection("posts").document(post.id)

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

                // 2. Borrar el post de Firestore
                postRef.delete().await()
                Log.d("ProfileViewModel", "Post borrado de Firestore")

                // 3. Actualizar contadores del usuario
                val userRef = db.collection("users").document(post.userId)
                userRef.update("postCount", FieldValue.increment(-1)).await()

                // 4. Actualizar el contador de likes si el post tenía likes
                if (post.likeCount > 0) {
                    userRef.update("likeCount", FieldValue.increment(-post.likeCount.toLong())).await()
                }

                Log.d("ProfileViewModel", "Post borrado con éxito")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error al borrar post: ${e.message}")
            }
        }
    }

    // Funcion para cerrar sesión
    fun signOut(onSuccess: () -> Unit) {
        auth.signOut()
        onSuccess()
    }
}
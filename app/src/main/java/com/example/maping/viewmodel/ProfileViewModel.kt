package com.example.maping.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.maping.model.Post
import com.example.maping.model.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query // Necesario para ordenar las publicaciones

class ProfileViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val db = Firebase.firestore
    private val currentUserId = auth.currentUser?.uid

    // Estado para el perfil del usuario autenticado (User model)
    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile

    // Estado para las publicaciones hechas por este usuario (List<Post>)
    private val _userPosts = MutableStateFlow<List<Post>>(emptyList())
    val userPosts: StateFlow<List<Post>> = _userPosts

    init {
        // Solo intentamos escuchar si hay un usuario logueado
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
            .whereEqualTo("userId", uid) // Filtrar solo las publicaciones de este usuario
            .orderBy("timestamp", Query.Direction.DESCENDING) // Ordenar por fecha
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

    // Funcion para cerrar sesión
    fun signOut(onSuccess: () -> Unit) {
        auth.signOut()
        onSuccess()
    }
}
package com.example.maping.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.maping.model.Post
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {
    private val db = Firebase.firestore

    // Estado que contendrá la lista de publicaciones para el mapa
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    init {
        listenToPosts()
    }

    private fun listenToPosts() {
        // Escuchamos la colección "posts" en tiempo real
        db.collection("posts")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("MapViewModel", "Error al escuchar posts", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val postList = snapshot.documents.mapNotNull { doc ->
                        // Convierte el documento JSON a un objeto Post
                        doc.toObject(Post::class.java)
                    }
                    _posts.value = postList
                }
            }
    }
}
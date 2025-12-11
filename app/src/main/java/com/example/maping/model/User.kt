package com.example.maping.model

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val uid: String = "", // Usamos el UID de Firebase Auth como ID del documento
    val username: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val postCount: Int = 0, // Contador de Publicaciones (comienza en 0)
    val likeCount: Int = 0, // Contador de Likes recibidos (comienza en 0)
    val visitedCount: Int = 0, // Contador de Lugares visitados (comienza en 0)
    val timestamp: Long = System.currentTimeMillis() // Fecha de registro
)
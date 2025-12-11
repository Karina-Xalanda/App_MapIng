package com.example.maping.model

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val uid: String = "", // Usamos el UID de Firebase Auth como ID del documento
    val username: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val postCount: Int = 0, // cuenta las Publicaciones
    val likeCount: Int = 0, // Cuenta Likes recibidos
    val visitedCount: Int = 0, // Cuenta los Lugares visitados
    val friends: List<String> = emptyList(), // cuenta amigos
    val timestamp: Long = System.currentTimeMillis() // Fecha de registro
)
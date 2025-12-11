package com.example.maping.model

import com.google.firebase.firestore.DocumentId

data class Comment(
    @DocumentId val id: String = "",
    val postId: String = "", // ID del post al que pertenece el comentario
    val userId: String = "", // UID del usuario que comentó
    val username: String = "Usuario Desconocido", // Nombre del usuario
    val text: String = "", // Contenido del comentario
    val timestamp: Long = 0
)
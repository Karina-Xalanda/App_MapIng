package com.example.maping.model

import com.google.firebase.firestore.DocumentId

//  campos que  se guardan en Firestore
data class Post(
    @DocumentId val id: String = "", // Firestore asignara el ID del documento aqui
    val userId: String = "",
    val comment: String = "",
    val imageUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0
)
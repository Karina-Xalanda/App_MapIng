package com.example.maping.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.maping.model.Comment
import com.example.maping.model.Post
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DetailViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val storage = Firebase.storage
    private val currentUserId = auth.currentUser?.uid

    // Estado para el Post específico
    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post

    // Estado para los comentarios del Post
    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments


    fun fetchPostDetail(postId: String) {
        // Escuchar el post en tiempo real
        db.collection("posts").document(postId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("DetailViewModel", "Error al escuchar el post", e)
                    _post.value = null
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    _post.value = snapshot.toObject(Post::class.java)
                    // Una vez que tenemos el post, cargamos los comentarios
                    listenToComments(postId)
                } else {
                    _post.value = null
                }
            }
    }

    // NUEVA FUNCIÓN: Escucha comentarios en tiempo real
    private fun listenToComments(postId: String) {
        db.collection("comments")
            .whereEqualTo("postId", postId)
            .orderBy("timestamp", Query.Direction.ASCENDING) // Ordenar por fecha de subida
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("DetailViewModel", "Error al escuchar comentarios", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val commentList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Comment::class.java)
                    }
                    _comments.value = commentList
                }
            }
    }

    // NUEVA FUNCIÓN: Añadir un comentario
    fun addComment(postId: String, commentText: String) {
        viewModelScope.launch {
            if (currentUserId == null || commentText.isBlank()) return@launch

            // 1. Obtener el nombre de usuario para el comentario
            val username = try {
                db.collection("users").document(currentUserId).get().await().toObject(com.example.maping.model.User::class.java)?.username ?: "Anónimo"
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error al obtener username: ${e.message}")
                "Anónimo"
            }

            // 2. Crear el objeto Comment
            val newComment = Comment(
                postId = postId,
                userId = currentUserId,
                username = username,
                text = commentText,
                timestamp = System.currentTimeMillis()
            )

            // 3. Guardar en Firestore
            try {
                db.collection("comments")
                    .add(newComment)
                    .await()
                Log.d("DetailViewModel", "Comentario añadido con éxito.")
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error al añadir comentario: ${e.message}")
            }
        }
    }

    // =====================================================================
    // FUNCIONES PARA BORRAR/EDITAR COMENTARIOS Y BORRAR PUBLICACIÓN
    // =====================================================================

    // NUEVA FUNCIÓN: Eliminar un comentario
    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            try {
                db.collection("comments").document(commentId).delete().await()
                Log.d("DetailViewModel", "Comentario eliminado con éxito: $commentId")
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error al eliminar comentario: ${e.message}")
            }
        }
    }

    // NUEVA FUNCIÓN: Editar un comentario
    fun editComment(commentId: String, newText: String) {
        if (newText.isBlank()) return

        viewModelScope.launch {
            try {
                db.collection("comments").document(commentId).update(
                    mapOf(
                        "text" to newText,
                        "timestamp" to System.currentTimeMillis() // Actualiza el timestamp de edición
                    )
                ).await()
                Log.d("DetailViewModel", "Comentario editado con éxito: $commentId")
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error al editar comentario: ${e.message}")
            }
        }
    }

    // NUEVA FUNCIÓN: Eliminar una publicación completa
    fun deletePost(postId: String, imageUrl: String, postOwnerId: String, onPostDeleted: () -> Unit) {
        if (currentUserId == null || currentUserId != postOwnerId) {
            Log.e("DetailViewModel", "Usuario no autorizado para eliminar post.")
            return
        }

        viewModelScope.launch {
            try {
                // 1. Iniciar la transacción para actualizar el contador del usuario y borrar el post
                db.runTransaction { transaction ->
                    val userRef = db.collection("users").document(postOwnerId)

                    // Decrementar postCount en el perfil del usuario
                    transaction.update(userRef, "postCount", FieldValue.increment(-1))

                    // Eliminar el documento del post
                    val postRef = db.collection("posts").document(postId)
                    transaction.delete(postRef)

                    null
                }.await()

                // 2. Eliminar la imagen de Storage
                if (imageUrl.isNotEmpty()) {
                    storage.getReferenceFromUrl(imageUrl).delete().await()
                }

                // 3. Eliminar todos los comentarios asociados a este post
                val commentsSnapshot = db.collection("comments").whereEqualTo("postId", postId).get().await()
                if (!commentsSnapshot.isEmpty) {
                    val batch = db.batch()
                    commentsSnapshot.documents.forEach { doc ->
                        batch.delete(doc.reference)
                    }
                    batch.commit().await()
                }

                Log.d("DetailViewModel", "Publicación, imagen y comentarios eliminados con éxito.")
                onPostDeleted() // Llama al callback para que la UI navegue de vuelta

            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error al eliminar la publicación: ${e.message}")
            }
        }
    }

    // Lógica para dar o quitar 'Like' (Corregida)
    fun toggleLike(postId: String, isCurrentlyLiked: Boolean) {
        if (currentUserId == null) {
            Log.e("DetailViewModel", "Usuario no autenticado para dar like.")
            return
        }

        viewModelScope.launch {
            val postRef = db.collection("posts").document(postId)

            try {
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(postRef)
                    val post = snapshot.toObject(Post::class.java) ?: return@runTransaction

                    // Declarar la referencia del dueño aquí para que sea accesible en ambos bloques
                    val postOwnerRef = db.collection("users").document(post.userId)

                    if (isCurrentlyLiked) {
                        // El usuario va a quitar el like
                        transaction.update(postRef, "likedBy", FieldValue.arrayRemove(currentUserId))
                        transaction.update(postRef, "likeCount", FieldValue.increment(-1))

                        // Decrementar likeCount en el perfil del usuario que creó el post
                        transaction.update(postOwnerRef, "likeCount", FieldValue.increment(-1))

                    } else {
                        // El usuario va a dar like
                        transaction.update(postRef, "likedBy", FieldValue.arrayUnion(currentUserId))
                        transaction.update(postRef, "likeCount", FieldValue.increment(1))

                        // Incrementar likeCount en el perfil del usuario que creó el post
                        transaction.update(postOwnerRef, "likeCount", FieldValue.increment(1))
                    }
                    null
                }.await()
                Log.d("DetailViewModel", "Like toggle exitoso.")
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error en la transacción de like: ${e.message}")
            }
        }
    }
}
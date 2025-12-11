package com.example.maping.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.maping.model.Comment // IMPORTAR NUEVO MODELO
import com.example.maping.model.Post
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DetailViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
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


    // Lógica para dar o quitar 'Like' (Se mantiene igual)
    fun toggleLike(postId: String, isCurrentlyLiked: Boolean) {
        if (currentUserId == null) {
            Log.e("DetailViewModel", "Usuario no autenticado para dar like.")
            return
        }

        viewModelScope.launch {
            val postRef = db.collection("posts").document(postId)

            // Usa Transaction para asegurar que el contador y la lista se actualicen juntos.
            try {
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(postRef)
                    val post = snapshot.toObject(Post::class.java) ?: return@runTransaction

                    if (isCurrentlyLiked) {
                        // El usuario va a quitar el like
                        transaction.update(postRef, "likedBy", FieldValue.arrayRemove(currentUserId))
                        transaction.update(postRef, "likeCount", FieldValue.increment(-1))

                        // Decrementar likeCount en el perfil del usuario que creó el post
                        val postOwnerRef = db.collection("users").document(post.userId)
                        transaction.update(postOwnerRef, "likeCount", FieldValue.increment(-1))

                    } else {
                        // El usuario va a dar like
                        transaction.update(postRef, "likedBy", FieldValue.arrayUnion(currentUserId))
                        transaction.update(postRef, "likeCount", FieldValue.increment(1))

                        // Incrementar likeCount en el perfil del usuario que creó el post
                        val postOwnerRef = db.collection("users").document(post.userId)
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
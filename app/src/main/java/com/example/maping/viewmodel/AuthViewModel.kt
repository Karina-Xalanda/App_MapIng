package com.example.maping.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.maping.model.User // IMPORTAR EL NUEVO MODELO
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore // IMPORTAR FIRESTORE
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val db = Firebase.firestore // INSTANCIA DE FIRESTORE

    // Estado para saber si el usuario está logueado o cargando
    private val _userState = MutableStateFlow<UserState>(UserState.LoggedOut)
    val userState: StateFlow<UserState> = _userState

    // Función que recibe el resultado del pop-up de Google
    fun signInWithGoogle(task: Task<GoogleSignInAccount>) {
        viewModelScope.launch {
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!, account) // Pasar la cuenta
            } catch (e: ApiException) {
                _userState.value = UserState.Error("Error en Google Sign-In: ${e.message}")
            }
        }
    }

    private suspend fun firebaseAuthWithGoogle(idToken: String, account: GoogleSignInAccount) {
        _userState.value = UserState.Loading
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user

            // --- Lógica adicional: Guardar/Actualizar perfil en Firestore ---
            if (firebaseUser != null) {
                saveOrUpdateUserProfile(firebaseUser.uid, account)
            }
            // -----------------------------------------------------------------

            _userState.value = UserState.Success // ¡Éxito!
        } catch (e: Exception) {
            _userState.value = UserState.Error(e.message ?: "Error desconocido")
        }
    }

    // Nueva función para manejar el perfil del usuario en Firestore
    private suspend fun saveOrUpdateUserProfile(uid: String, account: GoogleSignInAccount) {
        val userRef = db.collection("users").document(uid)

        // Intentar obtener el documento del usuario
        val userDocument = userRef.get().await()

        if (!userDocument.exists()) {
            // Si el usuario es nuevo, creamos su perfil inicial
            val newUser = User(
                uid = uid,
                // Usamos el nombre de Google o un placeholder si es nulo
                username = account.displayName ?: "usuario_${uid.substring(0, 6)}",
                email = account.email ?: "email_no_disponible",
                // La URL de la foto puede ser nula, Firestore lo acepta
                profileImageUrl = account.photoUrl.toString()
            )
            // Usamos set para que el ID del documento sea el mismo que el UID
            userRef.set(newUser).await()
        }
        // Nota: Si el usuario ya existe, no hacemos nada por ahora.
        // Podrías agregar lógica de actualización aquí si fuera necesario.
    }
}

// Estados posibles de la autenticación
sealed class UserState {
    object LoggedOut : UserState()
    object Loading : UserState()
    object Success : UserState()
    data class Error(val message: String) : UserState()
}
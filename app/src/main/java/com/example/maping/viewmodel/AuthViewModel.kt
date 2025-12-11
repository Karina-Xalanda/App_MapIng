package com.example.maping.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.maping.model.User
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log // Asegúrate de importar Log

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val db = Firebase.firestore

    private val _userState = MutableStateFlow<UserState>(UserState.LoggedOut)
    val userState: StateFlow<UserState> = _userState

    fun signInWithGoogle(task: Task<GoogleSignInAccount>) {
        viewModelScope.launch {
            try {
                // Verificar si la tarea falló antes de obtener el resultado
                if (task.exception != null) {
                    Log.e("AuthViewModel", "Google Sign-In Task Fallida: ${task.exception?.message}")
                    _userState.value = UserState.Error("Fallo de Google: ${task.exception?.message}")
                    return@launch
                }

                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!, account)
            } catch (e: ApiException) {
                // Captura el error de Google si el usuario canceló o falló la conexión
                Log.e("AuthViewModel", "Error en Google Sign-In (ApiException): ${e.message}")
                _userState.value = UserState.Error("Error en Google Sign-In: ${e.message}")
            } catch (e: Exception) {
                // Captura cualquier otro error
                Log.e("AuthViewModel", "Error desconocido en signInWithGoogle: ${e.message}")
                _userState.value = UserState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    private suspend fun firebaseAuthWithGoogle(idToken: String, account: GoogleSignInAccount) {
        _userState.value = UserState.Loading
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // 1. Guarda o actualiza el perfil en Firestore
                saveOrUpdateUserProfile(firebaseUser.uid, account)

                // 2. Transición a éxito SÓLO si todo lo anterior es exitoso
                _userState.value = UserState.Success
                Log.d("AuthViewModel", "Login Exitoso. Navegando a Mapa.")
            } else {
                Log.e("AuthViewModel", "Firebase User es nulo después de la autenticación.")
                _userState.value = UserState.Error("Autenticación fallida en Firebase.")
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error en Firebase Auth: ${e.message}", e)
            _userState.value = UserState.Error("Error de Firebase: ${e.message ?: "Error desconocido"}")
        }
    }

    private suspend fun saveOrUpdateUserProfile(uid: String, account: GoogleSignInAccount) {
        val userRef = db.collection("users").document(uid)

        try {
            val userDocument = userRef.get().await()

            if (!userDocument.exists()) {
                val newUser = User(
                    uid = uid,
                    username = account.displayName ?: "usuario_${uid.substring(0, 6)}",
                    email = account.email ?: "email_no_disponible",
                    profileImageUrl = account.photoUrl.toString()
                )
                userRef.set(newUser).await()
                Log.d("AuthViewModel", "Nuevo perfil de Firestore creado para UID: $uid")
            } else {
                Log.d("AuthViewModel", "Perfil de Firestore existente para UID: $uid")
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error al crear/actualizar perfil en Firestore: ${e.message}", e)
            // Si esto falla, la autenticación puede continuar, pero es bueno registrar el error.
        }
    }
}

// Estados posibles de la autenticación
sealed class UserState {
    object LoggedOut : UserState()
    object Loading : UserState()
    object Success : UserState()
    data class Error(val message: String) : UserState()
}
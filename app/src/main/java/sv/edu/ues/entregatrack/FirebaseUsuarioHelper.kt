package sv.edu.ues.entregatrack

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

// Modelo para guardar datos basicos del usuario
data class UsuarioFirebase(
    var uid: String = "",
    var nombre: String = "",
    var correo: String = "",
    var telefono: String = "",
    var rol: String = ""
)

object FirebaseUsuarioHelper {

    private val auth = FirebaseAuth.getInstance()

    // Referencia a la base de datos del proyecto
    private val database = FirebaseDatabase
        .getInstance("https://entregatrackpdm-1792b-default-rtdb.firebaseio.com/")
        .reference

    // Registra usuario en Firebase Auth y guarda sus datos en Realtime Database
    fun registrarUsuario(
        nombre: String,
        correo: String,
        password: String,
        telefono: String,
        rol: String,
        onSuccess: (UsuarioFirebase) -> Unit,
        onError: (String) -> Unit
    ) {
        if (nombre.isBlank() || correo.isBlank() || password.isBlank() || rol.isBlank()) {
            onError("Completa nombre, correo, contraseña y rol")
            return
        }

        if (password.length < 6) {
            onError("La contraseña debe tener al menos 6 caracteres")
            return
        }

        auth.createUserWithEmailAndPassword(correo, password)
            .addOnSuccessListener { resultado ->
                val uid = resultado.user?.uid ?: ""

                val usuario = UsuarioFirebase(
                    uid = uid,
                    nombre = nombre,
                    correo = correo,
                    telefono = telefono,
                    rol = rol
                )

                // Guarda datos adicionales del usuario
                database.child("usuarios")
                    .child(uid)
                    .setValue(usuario)
                    .addOnSuccessListener {
                        onSuccess(usuario)
                    }
                    .addOnFailureListener { error ->
                        onError(error.message ?: "Error al guardar datos del usuario")
                    }
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Error al registrar usuario")
            }
    }

    // Inicia sesion y obtiene el rol del usuario
    fun iniciarSesion(
        correo: String,
        password: String,
        onSuccess: (UsuarioFirebase) -> Unit,
        onError: (String) -> Unit
    ) {
        if (correo.isBlank() || password.isBlank()) {
            onError("Ingresa correo y contraseña")
            return
        }

        auth.signInWithEmailAndPassword(correo, password)
            .addOnSuccessListener { resultado ->
                val uid = resultado.user?.uid ?: ""

                // Busca el perfil del usuario por UID
                database.child("usuarios")
                    .child(uid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val usuario = snapshot.getValue(UsuarioFirebase::class.java)

                        if (usuario != null) {
                            onSuccess(usuario)
                        } else {
                            onError("No se encontró el perfil del usuario")
                        }
                    }
                    .addOnFailureListener { error ->
                        onError(error.message ?: "Error al consultar usuario")
                    }
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Correo o contraseña incorrectos")
            }
    }

    // Cierra la sesion actual
    fun cerrarSesion() {
        auth.signOut()
    }
}
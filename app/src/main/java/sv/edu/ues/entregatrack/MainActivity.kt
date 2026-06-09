package sv.edu.ues.entregatrack

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    private lateinit var edtCorreoLogin: EditText
    private lateinit var edtPasswordLogin: EditText

    // Pantalla principal de inicio de sesion
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Colores principales de la app
        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        setContentView(R.layout.activity_main)

        edtCorreoLogin = findViewById(R.id.edtCorreoLogin)
        edtPasswordLogin = findViewById(R.id.edtPasswordLogin)

        val btnIniciarSesion = findViewById<Button>(R.id.btnIniciarSesion)
        val btnCrearCuenta = findViewById<Button>(R.id.btnCrearCuenta)

        // Inicia sesion con Firebase Authentication
        btnIniciarSesion.setOnClickListener {
            iniciarSesion()
        }

        // Abre la pantalla de registro
        btnCrearCuenta.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }
    }

    // Valida credenciales y consulta el rol del usuario
    private fun iniciarSesion() {
        val correo = edtCorreoLogin.text.toString().trim()
        val password = edtPasswordLogin.text.toString().trim()

        FirebaseUsuarioHelper.iniciarSesion(
            correo = correo,
            password = password,
            onSuccess = { usuario ->
                Toast.makeText(this, "Bienvenido ${usuario.nombre}", Toast.LENGTH_SHORT).show()
                abrirPantallaPorRol(usuario.rol)
            },
            onError = { mensaje ->
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            }
        )
    }

    // Redirige segun el rol guardado en Firebase
    private fun abrirPantallaPorRol(rol: String) {
        val intent = if (rol == "cliente") {
            // El cliente primero ve su historial de pedidos
            Intent(this, HistorialPedidosActivity::class.java)
        } else {
            Intent(this, RepartidorActivity::class.java)
        }

        startActivity(intent)
    }
}
package sv.edu.ues.entregatrack

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.ComponentActivity

class RegistroActivity : ComponentActivity() {

    private lateinit var edtNombreRegistro: EditText
    private lateinit var edtCorreoRegistro: EditText
    private lateinit var edtTelefonoRegistro: EditText
    private lateinit var edtPasswordRegistro: EditText
    private lateinit var spinnerRolRegistro: Spinner

    // Pantalla para registrar usuarios basicos
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Colores principales de la app
        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        setContentView(R.layout.activity_registro)

        edtNombreRegistro = findViewById(R.id.edtNombreRegistro)
        edtCorreoRegistro = findViewById(R.id.edtCorreoRegistro)
        edtTelefonoRegistro = findViewById(R.id.edtTelefonoRegistro)
        edtPasswordRegistro = findViewById(R.id.edtPasswordRegistro)
        spinnerRolRegistro = findViewById(R.id.spinnerRolRegistro)

        val btnGuardarRegistro = findViewById<Button>(R.id.btnGuardarRegistro)
        val btnVolverRegistro = findViewById<Button>(R.id.btnVolverRegistro)

        configurarSpinnerRoles()

        // Guarda el usuario en Firebase Authentication y Realtime Database
        btnGuardarRegistro.setOnClickListener {
            registrarUsuario()
        }

        // Regresa al login
        btnVolverRegistro.setOnClickListener {
            finish()
        }
    }

    // Carga los roles disponibles
    private fun configurarSpinnerRoles() {
        val roles = listOf("Cliente", "Repartidor")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            roles
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRolRegistro.adapter = adapter
    }

    // Valida y registra el usuario
    private fun registrarUsuario() {
        val nombre = edtNombreRegistro.text.toString().trim()
        val correo = edtCorreoRegistro.text.toString().trim()
        val telefono = edtTelefonoRegistro.text.toString().trim()
        val password = edtPasswordRegistro.text.toString().trim()

        val rolSeleccionado = spinnerRolRegistro.selectedItem.toString()

        val rolFirebase = if (rolSeleccionado == "Cliente") {
            "cliente"
        } else {
            "repartidor"
        }

        FirebaseUsuarioHelper.registrarUsuario(
            nombre = nombre,
            correo = correo,
            password = password,
            telefono = telefono,
            rol = rolFirebase,
            onSuccess = { usuario ->
                Toast.makeText(this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show()
                abrirPantallaPorRol(usuario.rol)
            },
            onError = { mensaje ->
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            }
        )
    }

    // Abre la pantalla segun el rol
    // Abre la pantalla segun el rol
    private fun abrirPantallaPorRol(rol: String) {
        val intent = if (rol == "cliente") {
            Intent(this, HistorialPedidosActivity::class.java)
        } else {
            Intent(this, RepartidorActivity::class.java)
        }

        startActivity(intent)
        finish()
    }
}
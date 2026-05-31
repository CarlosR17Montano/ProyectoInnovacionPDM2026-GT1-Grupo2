package sv.edu.ues.entregatrack

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    // Pantalla principal de login
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        setContentView(R.layout.activity_main)

        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val btnDemoRepartidor = findViewById<Button>(R.id.btnDemoRepartidor)
        val btnDemoCliente = findViewById<Button>(R.id.btnDemoCliente)

        // Luego este boton se conectara con Firebase Authentication
        btnIngresar.setOnClickListener {
            Toast.makeText(this, "Login pendiente de conectar con Firebase", Toast.LENGTH_SHORT).show()
        }

        // Acceso temporal al panel del repartidor
        btnDemoRepartidor.setOnClickListener {
            val intent = Intent(this, RepartidorActivity::class.java)
            startActivity(intent)
        }

        // Acceso temporal al panel del cliente
        btnDemoCliente.setOnClickListener {
            val intent = Intent(this, ClienteActivity::class.java)
            startActivity(intent)
        }
    }
}
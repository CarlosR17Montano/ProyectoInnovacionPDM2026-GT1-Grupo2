package sv.edu.ues.entregatrack

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity

class ClienteActivity : ComponentActivity() {

    // Pantalla principal para el cliente
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        setContentView(R.layout.activity_cliente)

        val btnVerUbicacion = findViewById<Button>(R.id.btnVerUbicacion)
        val btnVerEvidencia = findViewById<Button>(R.id.btnVerEvidencia)

        btnVerUbicacion.setOnClickListener {
            Toast.makeText(this, "Aquí se mostrará Google Maps", Toast.LENGTH_SHORT).show()
        }

        btnVerEvidencia.setOnClickListener {
            Toast.makeText(this, "Aquí se mostrará la evidencia con Glide", Toast.LENGTH_SHORT).show()
        }
    }
}
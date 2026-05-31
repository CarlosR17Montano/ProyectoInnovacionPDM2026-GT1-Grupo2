package sv.edu.ues.entregatrack

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity

class RepartidorActivity : ComponentActivity() {

    // Pantalla principal para el repartidor
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        setContentView(R.layout.activity_repartidor)

        val btnIniciarEntrega = findViewById<Button>(R.id.btnIniciarEntrega)
        val btnActualizarUbicacion = findViewById<Button>(R.id.btnActualizarUbicacion)
        val btnTomarEvidencia = findViewById<Button>(R.id.btnTomarEvidencia)
        val btnFinalizarEntrega = findViewById<Button>(R.id.btnFinalizarEntrega)

        btnIniciarEntrega.setOnClickListener {
            Toast.makeText(this, "Entrega iniciada", Toast.LENGTH_SHORT).show()
        }

        btnActualizarUbicacion.setOnClickListener {
            Toast.makeText(this, "Ubicación actualizada en tiempo real", Toast.LENGTH_SHORT).show()
        }

        btnTomarEvidencia.setOnClickListener {
            Toast.makeText(this, "Aquí se abrirá CameraX", Toast.LENGTH_SHORT).show()
        }

        btnFinalizarEntrega.setOnClickListener {
            Toast.makeText(this, "Entrega finalizada correctamente", Toast.LENGTH_SHORT).show()
        }
    }
}
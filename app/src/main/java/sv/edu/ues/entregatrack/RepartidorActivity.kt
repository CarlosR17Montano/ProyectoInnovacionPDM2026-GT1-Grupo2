package sv.edu.ues.entregatrack

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import android.content.Intent

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

        // Abre la pantalla donde luego se integrara Google Maps
        btnActualizarUbicacion.setOnClickListener {
            val intent = Intent(this, MapaActivity::class.java)
            startActivity(intent)
        }

        // Abre la pantalla temporal de evidencia fotografica
        btnTomarEvidencia.setOnClickListener {
            val intent = Intent(this, EvidenciaActivity::class.java)
            startActivity(intent)
        }

        btnFinalizarEntrega.setOnClickListener {
            Toast.makeText(this, "Entrega finalizada correctamente", Toast.LENGTH_SHORT).show()
        }
    }
}
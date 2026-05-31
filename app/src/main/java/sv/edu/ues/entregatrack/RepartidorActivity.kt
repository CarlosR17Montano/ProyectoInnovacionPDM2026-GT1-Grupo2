package sv.edu.ues.entregatrack

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

class RepartidorActivity : ComponentActivity() {

    private lateinit var txtEstadoRepartidor: TextView

    // Pantalla principal para el repartidor
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Colores principales de la app
        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        setContentView(R.layout.activity_repartidor)

        val btnIniciarEntrega = findViewById<Button>(R.id.btnIniciarEntrega)
        val btnActualizarUbicacion = findViewById<Button>(R.id.btnActualizarUbicacion)
        val btnTomarEvidencia = findViewById<Button>(R.id.btnTomarEvidencia)
        val btnFinalizarEntrega = findViewById<Button>(R.id.btnFinalizarEntrega)

        txtEstadoRepartidor = findViewById(R.id.txtEstadoRepartidor)

        // Muestra el estado actual al abrir la pantalla
        actualizarEstadoPantalla()

        // Inicia la entrega de prueba
        btnIniciarEntrega.setOnClickListener {
            DatosEntrega.iniciarEntrega()
            actualizarEstadoPantalla()
            Toast.makeText(this, "Entrega iniciada", Toast.LENGTH_SHORT).show()
        }

        // Abre la pantalla del mapa GPS
        btnActualizarUbicacion.setOnClickListener {
            val intent = Intent(this, MapaActivity::class.java)
            startActivity(intent)
        }

        // Abre la pantalla de evidencia fotografica
        btnTomarEvidencia.setOnClickListener {
            val intent = Intent(this, EvidenciaActivity::class.java)
            startActivity(intent)
        }

        // Finaliza la entrega de prueba
        btnFinalizarEntrega.setOnClickListener {
            DatosEntrega.finalizarEntrega()
            actualizarEstadoPantalla()
            Toast.makeText(this, "Entrega finalizada correctamente", Toast.LENGTH_SHORT).show()
        }
    }

    // Refresca el estado visible del pedido
    private fun actualizarEstadoPantalla() {
        txtEstadoRepartidor.text = "Estado: ${DatosEntrega.estadoPedido}"
    }

    // Actualiza el estado al regresar desde otra pantalla
    override fun onResume() {
        super.onResume()
        if (::txtEstadoRepartidor.isInitialized) {
            actualizarEstadoPantalla()
        }
    }
}
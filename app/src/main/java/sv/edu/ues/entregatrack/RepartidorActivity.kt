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

        // Inicia la entrega y sincroniza el estado en Firebase
        btnIniciarEntrega.setOnClickListener {
            DatosEntrega.iniciarEntrega()
            actualizarEstadoPantalla()

            FirebaseEntregaHelper.guardarUbicacionPedido(
                onSuccess = {
                    Toast.makeText(this, "Entrega iniciada y sincronizada", Toast.LENGTH_SHORT).show()
                },
                onError = { mensaje ->
                    Toast.makeText(this, "Error Firebase: $mensaje", Toast.LENGTH_LONG).show()
                }
            )
        }

        // Abre el mapa en modo repartidor
        btnActualizarUbicacion.setOnClickListener {
            val intent = Intent(this, MapaActivity::class.java)
            intent.putExtra("modo", "repartidor")
            startActivity(intent)
        }

        // Abre la pantalla de evidencia fotografica
        btnTomarEvidencia.setOnClickListener {
            val intent = Intent(this, EvidenciaActivity::class.java)
            startActivity(intent)
        }

        // Finaliza la entrega y sincroniza el estado en Firebase
        btnFinalizarEntrega.setOnClickListener {
            DatosEntrega.finalizarEntrega()
            actualizarEstadoPantalla()

            FirebaseEntregaHelper.guardarUbicacionPedido(
                onSuccess = {
                    Toast.makeText(this, "Entrega finalizada y sincronizada", Toast.LENGTH_SHORT).show()
                },
                onError = { mensaje ->
                    Toast.makeText(this, "Error Firebase: $mensaje", Toast.LENGTH_LONG).show()
                }
            )
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
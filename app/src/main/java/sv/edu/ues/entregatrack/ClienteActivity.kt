package sv.edu.ues.entregatrack

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity

class ClienteActivity : ComponentActivity() {

    private lateinit var txtEstadoCliente: TextView
    private lateinit var txtUltimaActualizacionCliente: TextView

    // Pantalla principal para el cliente
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Colores principales de la app
        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        setContentView(R.layout.activity_cliente)

        val btnVerUbicacion = findViewById<Button>(R.id.btnVerUbicacion)
        val btnVerEvidencia = findViewById<Button>(R.id.btnVerEvidencia)

        txtEstadoCliente = findViewById(R.id.txtEstadoCliente)
        txtUltimaActualizacionCliente = findViewById(R.id.txtUltimaActualizacionCliente)

        // Muestra el estado actual del pedido
        actualizarDatosPedido()

        // Abre la pantalla de seguimiento GPS
        // Abre la pantalla de mapa en modo cliente
        btnVerUbicacion.setOnClickListener {
            val intent = Intent(this, MapaActivity::class.java)
            intent.putExtra("modo", "cliente")
            startActivity(intent)
        }

        // Abre la pantalla de evidencia
        btnVerEvidencia.setOnClickListener {
            val intent = Intent(this, EvidenciaActivity::class.java)
            startActivity(intent)
        }
    }

    // Refresca los datos visibles del pedido
    private fun actualizarDatosPedido() {
        txtEstadoCliente.text = "Estado actual: ${DatosEntrega.estadoPedido}"
        txtUltimaActualizacionCliente.text = "Última actualización: ${DatosEntrega.ultimaActualizacion}"
    }

    // Actualiza datos al regresar desde mapa o evidencia
    override fun onResume() {
        super.onResume()
        if (::txtEstadoCliente.isInitialized) {
            actualizarDatosPedido()
        }
    }
}
package sv.edu.ues.entregatrack

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.database.ValueEventListener

class ClienteActivity : ComponentActivity() {

    private lateinit var txtCodigoPedidoCliente: TextView
    private lateinit var txtNombreCliente: TextView
    private lateinit var txtRepartidorCliente: TextView
    private lateinit var txtDireccionCliente: TextView
    private lateinit var txtEstadoCliente: TextView
    private lateinit var txtUltimaActualizacionCliente: TextView
    private lateinit var txtEvidenciaCliente: TextView

    private lateinit var btnVerUbicacion: Button
    private lateinit var btnVerEvidencia: Button

    private var listenerFirebase: ValueEventListener? = null
    private var pedidoFinalizadoSeleccionado: Boolean = false

    // Pantalla de detalle del pedido seleccionado por el cliente
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Colores principales de la app
        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        setContentView(R.layout.activity_cliente)

        btnVerUbicacion = findViewById(R.id.btnVerUbicacion)
        btnVerEvidencia = findViewById(R.id.btnVerEvidencia)

        txtCodigoPedidoCliente = findViewById(R.id.txtCodigoPedidoCliente)
        txtNombreCliente = findViewById(R.id.txtNombreCliente)
        txtRepartidorCliente = findViewById(R.id.txtRepartidorCliente)
        txtDireccionCliente = findViewById(R.id.txtDireccionCliente)
        txtEstadoCliente = findViewById(R.id.txtEstadoCliente)
        txtUltimaActualizacionCliente = findViewById(R.id.txtUltimaActualizacionCliente)
        txtEvidenciaCliente = findViewById(R.id.txtEvidenciaCliente)

        // Recibe el pedido seleccionado desde el historial
        val codigoPedido = intent.getStringExtra("codigoPedido") ?: DatosEntrega.codigoPedido

        cargarPedidoSeleccionado(codigoPedido)

        // Abre el mapa en modo cliente
        btnVerUbicacion.setOnClickListener {
            val intent = Intent(this, MapaActivity::class.java)
            intent.putExtra("modo", "cliente")
            startActivity(intent)
        }

        // Abre la evidencia en modo cliente, solo lectura
        btnVerEvidencia.setOnClickListener {
            val intent = Intent(this, EvidenciaActivity::class.java)
            intent.putExtra("modo", "cliente")
            startActivity(intent)
        }
    }

    // Carga el pedido seleccionado desde Firebase
    private fun cargarPedidoSeleccionado(codigoPedido: String) {
        FirebasePedidoHelper.cargarPedidoPorCodigo(
            codigoPedido = codigoPedido,
            onSuccess = { pedido ->
                FirebasePedidoHelper.aplicarPedidoADatosEntrega(pedido)

                pedidoFinalizadoSeleccionado = pedido.finalizado ||
                        pedido.estadoPedido.equals("Entregado", ignoreCase = true)

                actualizarDatosPedido()

                // Solo escucha ubicación en tiempo real si el pedido sigue activo
                if (!pedidoFinalizadoSeleccionado) {
                    iniciarEscuchaFirebase()
                }
            },
            onError = { mensaje ->
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()

                // Si falla la consulta, muestra los datos temporales actuales
                actualizarDatosPedido()
            }
        )
    }

    // Escucha cambios del pedido activo en tiempo real
    private fun iniciarEscuchaFirebase() {
        FirebaseEntregaHelper.detenerEscuchaPedido(listenerFirebase)

        listenerFirebase = FirebaseEntregaHelper.escucharUbicacionPedido(
            onChange = { datos ->
                DatosEntrega.estadoPedido = datos.estadoPedido
                DatosEntrega.latitud = datos.latitud
                DatosEntrega.longitud = datos.longitud
                DatosEntrega.ultimaActualizacion = datos.ultimaActualizacion
                DatosEntrega.evidenciaRegistrada = datos.evidenciaRegistrada
                DatosEntrega.rutaFotoEvidencia = datos.rutaFotoEvidencia

                pedidoFinalizadoSeleccionado =
                    DatosEntrega.estadoPedido.equals("Entregado", ignoreCase = true)

                actualizarDatosPedido()
            },
            onError = { mensaje ->
                Toast.makeText(this, "Error Firebase: $mensaje", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Refresca los datos visibles del pedido
    private fun actualizarDatosPedido() {
        txtCodigoPedidoCliente.text = "Pedido: ${DatosEntrega.codigoPedido}"
        txtNombreCliente.text = "Cliente: ${DatosEntrega.clientePedido}"
        txtRepartidorCliente.text = "Repartidor: ${DatosEntrega.repartidorPedido}"
        txtDireccionCliente.text = "Dirección: ${DatosEntrega.direccionPedido}"

        txtEstadoCliente.text = "Estado actual: ${DatosEntrega.estadoPedido}"
        txtUltimaActualizacionCliente.text =
            "Última actualización: ${DatosEntrega.ultimaActualizacion}"

        txtEvidenciaCliente.text = if (DatosEntrega.evidenciaRegistrada) {
            "Evidencia registrada: Sí"
        } else {
            "Evidencia registrada: No"
        }

        configurarBotonesPorEstado()
    }

    // Muestra u oculta botones según el estado del pedido
    private fun configurarBotonesPorEstado() {
        val pedidoEntregado = pedidoFinalizadoSeleccionado ||
                DatosEntrega.estadoPedido.equals("Entregado", ignoreCase = true)

        if (pedidoEntregado) {
            // Si el pedido ya finalizó, solo se permite ver la evidencia
            btnVerUbicacion.visibility = View.GONE

            btnVerEvidencia.visibility = if (DatosEntrega.evidenciaRegistrada) {
                View.VISIBLE
            } else {
                View.GONE
            }

            btnVerEvidencia.text = "Ver evidencia entregada"
        } else {
            // Si el pedido está activo, se muestra la ubicación en tiempo real
            btnVerUbicacion.visibility = View.VISIBLE

            btnVerEvidencia.visibility = if (DatosEntrega.evidenciaRegistrada) {
                View.VISIBLE
            } else {
                View.GONE
            }

            btnVerEvidencia.text = "Ver evidencia de entrega"
        }
    }

    // Actualiza datos al regresar desde mapa o evidencia
    override fun onResume() {
        super.onResume()

        if (::txtEstadoCliente.isInitialized) {
            actualizarDatosPedido()
        }
    }

    // Detiene la escucha al cerrar la pantalla
    override fun onDestroy() {
        super.onDestroy()
        FirebaseEntregaHelper.detenerEscuchaPedido(listenerFirebase)
    }
}
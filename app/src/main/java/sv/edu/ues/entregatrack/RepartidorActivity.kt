package sv.edu.ues.entregatrack

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

class RepartidorActivity : ComponentActivity() {

    private lateinit var txtCodigoPedidoRepartidor: TextView
    private lateinit var txtClienteRepartidor: TextView
    private lateinit var txtDireccionRepartidor: TextView
    private lateinit var txtEstadoRepartidor: TextView
    private lateinit var txtGananciaRepartidor: TextView

    private lateinit var btnIrRecogerPaquete: Button
    private lateinit var btnLleguePuntoRecogida: Button
    private lateinit var btnConfirmarPaqueteRecogido: Button
    private lateinit var btnIniciarRutaEntrega: Button
    private lateinit var btnActualizarUbicacion: Button
    private lateinit var btnTomarEvidencia: Button
    private lateinit var btnFinalizarEntrega: Button

    private var codigoPedidoActual: String = ""

    // Pantalla principal para que el repartidor gestione el pedido aceptado
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        setContentView(R.layout.activity_repartidor)

        txtCodigoPedidoRepartidor = findViewById(R.id.txtCodigoPedidoRepartidor)
        txtClienteRepartidor = findViewById(R.id.txtClienteRepartidor)
        txtDireccionRepartidor = findViewById(R.id.txtDireccionRepartidor)
        txtEstadoRepartidor = findViewById(R.id.txtEstadoRepartidor)
        txtGananciaRepartidor = findViewById(R.id.txtGananciaRepartidor)

        val btnVerSolicitudesDisponibles = findViewById<Button>(R.id.btnVerSolicitudesDisponibles)

        btnIrRecogerPaquete = findViewById(R.id.btnIrRecogerPaquete)
        btnLleguePuntoRecogida = findViewById(R.id.btnLleguePuntoRecogida)
        btnConfirmarPaqueteRecogido = findViewById(R.id.btnConfirmarPaqueteRecogido)
        btnIniciarRutaEntrega = findViewById(R.id.btnIniciarRutaEntrega)
        btnActualizarUbicacion = findViewById(R.id.btnActualizarUbicacion)
        btnTomarEvidencia = findViewById(R.id.btnTomarEvidencia)
        btnFinalizarEntrega = findViewById(R.id.btnFinalizarEntrega)

        // Recibe el pedido real aceptado desde SolicitudesRepartidorActivity
        codigoPedidoActual = intent.getStringExtra("codigoPedido") ?: ""

        if (codigoPedidoActual.isNotBlank()) {
            cargarPedidoAsignado(codigoPedidoActual)
        } else {
            mostrarSinPedido()
        }

        // Abre solicitudes disponibles
        btnVerSolicitudesDisponibles.setOnClickListener {
            val intent = Intent(this, SolicitudesRepartidorActivity::class.java)
            startActivity(intent)
        }

        // Estado: En camino a recogida
        btnIrRecogerPaquete.setOnClickListener {
            cambiarEstadoPedido(
                nuevoEstado = "En camino a recogida",
                mensaje = "El repartidor va en camino al punto de recogida",
                finalizado = false
            )
        }

        // Estado: En punto de recogida
        btnLleguePuntoRecogida.setOnClickListener {
            cambiarEstadoPedido(
                nuevoEstado = "En punto de recogida",
                mensaje = "El repartidor llegó al punto donde debe recoger el paquete",
                finalizado = false
            )
        }

        // Estado: Paquete recogido
        btnConfirmarPaqueteRecogido.setOnClickListener {
            cambiarEstadoPedido(
                nuevoEstado = "Paquete recogido",
                mensaje = "El repartidor ya recibió el paquete",
                finalizado = false
            )

            // Abrimos evidencia indicando que es evidencia de recogida
            val intent = Intent(this, EvidenciaActivity::class.java)
            intent.putExtra("modo", "repartidor")
            intent.putExtra("codigoPedido", codigoPedidoActual)
            intent.putExtra("tipoEvidencia", "recogida")
            startActivity(intent)
        }

        // Estado: En ruta al punto de entrega
        btnIniciarRutaEntrega.setOnClickListener {
            cambiarEstadoPedido(
                nuevoEstado = "En ruta al punto de entrega",
                mensaje = "El repartidor ya va hacia el punto de entrega",
                finalizado = false
            )
        }

        // Actualiza ubicación GPS
        btnActualizarUbicacion.setOnClickListener {
            if (codigoPedidoActual.isBlank()) {
                Toast.makeText(this, "Primero acepta una solicitud", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, MapaActivity::class.java)
            intent.putExtra("modo", "repartidor")
            intent.putExtra("codigoPedido", codigoPedidoActual)
            startActivity(intent)
        }

        // Evidencia final de entrega
        btnTomarEvidencia.setOnClickListener {
            if (codigoPedidoActual.isBlank()) {
                Toast.makeText(this, "Primero acepta una solicitud", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, EvidenciaActivity::class.java)
            intent.putExtra("modo", "repartidor")
            intent.putExtra("codigoPedido", codigoPedidoActual)
            intent.putExtra("tipoEvidencia", "entrega")
            startActivity(intent)
        }

        // Finaliza la solicitud
        btnFinalizarEntrega.setOnClickListener {
            cambiarEstadoPedido(
                nuevoEstado = "Finalizado",
                mensaje = "Solicitud finalizada. El paquete fue entregado correctamente",
                finalizado = true
            )
        }
    }

    // Carga desde Firebase los datos reales del pedido aceptado
    private fun cargarPedidoAsignado(codigoPedido: String) {
        FirebasePedidoHelper.cargarPedidoPorCodigo(
            codigoPedido = codigoPedido,
            onSuccess = { pedido ->

                val pedidoFinalizado = pedido.finalizado ||
                        pedido.estadoPedido == "Entregado" ||
                        pedido.estadoPedido == "Finalizado"

                txtCodigoPedidoRepartidor.text = "Código: ${pedido.codigoPedido}"

                if (pedidoFinalizado) {
                    // Por seguridad, ya no se muestran datos sensibles al finalizar
                    txtClienteRepartidor.text = "Cliente: Información protegida"
                    txtDireccionRepartidor.text = "Dirección: Información protegida"
                } else {
                    txtClienteRepartidor.text = "Cliente: ${pedido.clienteNombre}"
                    txtDireccionRepartidor.text = "Dirección: ${pedido.direccionEntrega}"
                }

                txtEstadoRepartidor.text = "Estado: ${pedido.estadoPedido}"

                val ganancia = calcularGananciaRepartidor(pedido.precioEstimado)

                txtGananciaRepartidor.text = if (pedidoFinalizado) {
                    "Ganancia final: $${String.format("%.2f", ganancia)}"
                } else {
                    "Ganancia estimada: $${String.format("%.2f", ganancia)}"
                }

                // Actualiza datos temporales usados por mapa, evidencia y seguimiento
                FirebasePedidoHelper.aplicarPedidoADatosEntrega(pedido)

                actualizarBotonesSegunEstado(pedido.estadoPedido, pedidoFinalizado)
            },
            onError = { mensaje ->
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
                mostrarSinPedido()
            }
        )
    }

    // Cambia estado real del pedido en Firebase y notifica al cliente
    private fun cambiarEstadoPedido(
        nuevoEstado: String,
        mensaje: String,
        finalizado: Boolean
    ) {
        if (codigoPedidoActual.isBlank()) {
            Toast.makeText(this, "Primero acepta una solicitud", Toast.LENGTH_SHORT).show()
            return
        }

        DatosEntrega.estadoPedido = nuevoEstado
        DatosEntrega.ultimaActualizacion = mensaje
        txtEstadoRepartidor.text = "Estado: $nuevoEstado"

        FirebasePedidoHelper.actualizarEstadoPedido(
            codigoPedido = codigoPedidoActual,
            nuevoEstado = nuevoEstado,
            mensajeActualizacion = mensaje,
            finalizado = finalizado,
            onSuccess = {
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
                cargarPedidoAsignado(codigoPedidoActual)
            },
            onError = { error ->
                Toast.makeText(this, "Error Firebase: $error", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Muestra pantalla sin pedido seleccionado
    private fun mostrarSinPedido() {
        txtCodigoPedidoRepartidor.text = "Código: Sin pedido"
        txtClienteRepartidor.text = "Cliente: No asignado"
        txtDireccionRepartidor.text = "Dirección: No asignada"
        txtEstadoRepartidor.text = "Estado: Selecciona una solicitud disponible"
        txtGananciaRepartidor.text = "Ganancia estimada: $0.00"

        habilitarBoton(btnIrRecogerPaquete, false)
        habilitarBoton(btnLleguePuntoRecogida, false)
        habilitarBoton(btnConfirmarPaqueteRecogido, false)
        habilitarBoton(btnIniciarRutaEntrega, false)
        habilitarBoton(btnActualizarUbicacion, false)
        habilitarBoton(btnTomarEvidencia, false)
        habilitarBoton(btnFinalizarEntrega, false)
    }

    // Controla qué botón puede usarse según el estado actual
    private fun actualizarBotonesSegunEstado(
        estado: String,
        pedidoFinalizado: Boolean
    ) {
        if (pedidoFinalizado) {
            habilitarBoton(btnIrRecogerPaquete, false)
            habilitarBoton(btnLleguePuntoRecogida, false)
            habilitarBoton(btnConfirmarPaqueteRecogido, false)
            habilitarBoton(btnIniciarRutaEntrega, false)
            habilitarBoton(btnActualizarUbicacion, false)
            habilitarBoton(btnTomarEvidencia, false)
            habilitarBoton(btnFinalizarEntrega, false)
            return
        }

        habilitarBoton(btnIrRecogerPaquete, false)
        habilitarBoton(btnLleguePuntoRecogida, false)
        habilitarBoton(btnConfirmarPaqueteRecogido, false)
        habilitarBoton(btnIniciarRutaEntrega, false)
        habilitarBoton(btnActualizarUbicacion, false)
        habilitarBoton(btnTomarEvidencia, false)
        habilitarBoton(btnFinalizarEntrega, false)

        when (estado) {
            "Aceptado por repartidor" -> {
                habilitarBoton(btnIrRecogerPaquete, true)
                habilitarBoton(btnActualizarUbicacion, true)
            }

            "En camino a recogida" -> {
                habilitarBoton(btnLleguePuntoRecogida, true)
                habilitarBoton(btnActualizarUbicacion, true)
            }

            "En punto de recogida" -> {
                habilitarBoton(btnConfirmarPaqueteRecogido, true)
                habilitarBoton(btnActualizarUbicacion, true)
            }

            "Paquete recogido" -> {
                habilitarBoton(btnIniciarRutaEntrega, true)
                habilitarBoton(btnActualizarUbicacion, true)
            }

            "En ruta al punto de entrega",
            "Cerca del punto de entrega" -> {
                habilitarBoton(btnActualizarUbicacion, true)
                habilitarBoton(btnTomarEvidencia, true)
                habilitarBoton(btnFinalizarEntrega, true)
            }

            "Entregado" -> {
                habilitarBoton(btnFinalizarEntrega, true)
            }

            else -> {
                habilitarBoton(btnIrRecogerPaquete, true)
                habilitarBoton(btnActualizarUbicacion, true)
            }
        }
    }

    // Habilita o deshabilita visualmente botones
    private fun habilitarBoton(
        boton: Button,
        habilitado: Boolean
    ) {
        boton.isEnabled = habilitado
        boton.alpha = if (habilitado) 1.0f else 0.45f
    }

    // Fórmula simple de ganancia para el repartidor
    private fun calcularGananciaRepartidor(precioEstimado: Double): Double {
        return precioEstimado * 0.70
    }

    // Refresca al regresar desde mapa o evidencia
    override fun onResume() {
        super.onResume()

        if (::txtEstadoRepartidor.isInitialized && codigoPedidoActual.isNotBlank()) {
            cargarPedidoAsignado(codigoPedidoActual)
        }
    }
}
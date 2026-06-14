package sv.edu.ues.entregatrack

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

// Gestiona el pedido asignado al repartidor
class RepartidorActivity : ComponentActivity() {

    // Información del pedido
    private lateinit var txtCodigoPedidoRepartidor: TextView
    private lateinit var txtClienteRepartidor: TextView
    private lateinit var txtDireccionRepartidor: TextView
    private lateinit var txtEstadoRepartidor: TextView
    private lateinit var txtGananciaRepartidor: TextView

    // Botones del proceso
    private lateinit var btnIrRecogerPaquete: Button
    private lateinit var btnLleguePuntoRecogida: Button
    private lateinit var btnConfirmarPaqueteRecogido: Button
    private lateinit var btnIniciarRutaEntrega: Button
    private lateinit var btnActualizarUbicacion: Button
    private lateinit var btnTomarEvidencia: Button
    private lateinit var btnFinalizarEntrega: Button

    // Código del pedido seleccionado
    private var codigoPedidoActual: String = ""

    // Solicita permisos de ubicación
    private val solicitarPermisosUbicacionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permisos ->

            val permisoPreciso =
                permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true

            val permisoAproximado =
                permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (permisoPreciso || permisoAproximado) {
                iniciarServicioUbicacion()

                Toast.makeText(
                    this,
                    "Seguimiento GPS automático iniciado",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Debes permitir la ubicación para compartir el recorrido",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        setContentView(R.layout.activity_repartidor)

        // Relaciona los textos
        txtCodigoPedidoRepartidor =
            findViewById(R.id.txtCodigoPedidoRepartidor)

        txtClienteRepartidor =
            findViewById(R.id.txtClienteRepartidor)

        txtDireccionRepartidor =
            findViewById(R.id.txtDireccionRepartidor)

        txtEstadoRepartidor =
            findViewById(R.id.txtEstadoRepartidor)

        txtGananciaRepartidor =
            findViewById(R.id.txtGananciaRepartidor)

        // Relaciona el botón de solicitudes
        val btnVerSolicitudesDisponibles =
            findViewById<Button>(
                R.id.btnVerSolicitudesDisponibles
            )

        // Relaciona los botones del proceso
        btnIrRecogerPaquete =
            findViewById(R.id.btnIrRecogerPaquete)

        btnLleguePuntoRecogida =
            findViewById(R.id.btnLleguePuntoRecogida)

        btnConfirmarPaqueteRecogido =
            findViewById(R.id.btnConfirmarPaqueteRecogido)

        btnIniciarRutaEntrega =
            findViewById(R.id.btnIniciarRutaEntrega)

        btnActualizarUbicacion =
            findViewById(R.id.btnActualizarUbicacion)

        btnTomarEvidencia =
            findViewById(R.id.btnTomarEvidencia)

        btnFinalizarEntrega =
            findViewById(R.id.btnFinalizarEntrega)

        // Recibe el pedido aceptado
        codigoPedidoActual =
            intent.getStringExtra("codigoPedido") ?: ""

        if (codigoPedidoActual.isNotBlank()) {
            cargarPedidoAsignado(codigoPedidoActual)
        } else {
            mostrarSinPedido()
        }

        // Abre las solicitudes disponibles
        btnVerSolicitudesDisponibles.setOnClickListener {
            val intent =
                Intent(
                    this,
                    SolicitudesRepartidorActivity::class.java
                )

            startActivity(intent)
        }

        // Inicia el recorrido y el GPS automático
        btnIrRecogerPaquete.setOnClickListener {
            cambiarEstadoPedido(
                nuevoEstado = "En camino a recogida",
                mensaje =
                    "El repartidor va en camino al punto de recogida",
                finalizado = false
            )
        }

        // Confirma llegada al punto de recogida
        btnLleguePuntoRecogida.setOnClickListener {
            cambiarEstadoPedido(
                nuevoEstado = "En punto de recogida",
                mensaje =
                    "El repartidor llegó al punto donde debe recoger el paquete",
                finalizado = false
            )
        }

        // Confirma recogida y abre la evidencia
        btnConfirmarPaqueteRecogido.setOnClickListener {
            if (codigoPedidoActual.isBlank()) {
                Toast.makeText(
                    this,
                    "Primero acepta una solicitud",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // El estado cambiará únicamente después de guardar la fotografía
            val intent = Intent(
                this,
                EvidenciaActivity::class.java
            )

            intent.putExtra("modo", "repartidor")
            intent.putExtra("codigoPedido", codigoPedidoActual)
            intent.putExtra("tipoEvidencia", "recogida")

            startActivity(intent)
        }

        // Inicia la ruta hacia la entrega
        btnIniciarRutaEntrega.setOnClickListener {
            cambiarEstadoPedido(
                nuevoEstado = "En ruta al punto de entrega",
                mensaje =
                    "El repartidor ya va hacia el punto de entrega",
                finalizado = false
            )
        }

        // Abre el mapa como respaldo manual
        btnActualizarUbicacion.setOnClickListener {
            if (codigoPedidoActual.isBlank()) {
                Toast.makeText(
                    this,
                    "Primero acepta una solicitud",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val intent =
                Intent(
                    this,
                    MapaActivity::class.java
                )

            intent.putExtra("modo", "repartidor")
            intent.putExtra("codigoPedido", codigoPedidoActual)

            startActivity(intent)
        }

        // Abre la evidencia final
        btnTomarEvidencia.setOnClickListener {
            if (codigoPedidoActual.isBlank()) {
                Toast.makeText(
                    this,
                    "Primero acepta una solicitud",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val intent =
                Intent(
                    this,
                    EvidenciaActivity::class.java
                )

            intent.putExtra("modo", "repartidor")
            intent.putExtra("codigoPedido", codigoPedidoActual)
            intent.putExtra("tipoEvidencia", "entrega")

            startActivity(intent)
        }

        // Finaliza el pedido y detiene el GPS
        btnFinalizarEntrega.setOnClickListener {
            cambiarEstadoPedido(
                nuevoEstado = "Finalizado",
                mensaje =
                    "Solicitud finalizada. El paquete fue entregado correctamente",
                finalizado = true
            )
        }
    }

    // Carga el pedido asignado desde Firebase
    private fun cargarPedidoAsignado(
        codigoPedido: String
    ) {
        FirebasePedidoHelper.cargarPedidoPorCodigo(
            codigoPedido = codigoPedido,

            onSuccess = { pedido ->

                val pedidoFinalizado =
                    pedido.finalizado ||
                            pedido.estadoPedido.equals(
                                "Entregado",
                                ignoreCase = true
                            ) ||
                            pedido.estadoPedido.equals(
                                "Finalizado",
                                ignoreCase = true
                            )

                txtCodigoPedidoRepartidor.text =
                    "Código: ${pedido.codigoPedido}"

                if (pedidoFinalizado) {
                    txtClienteRepartidor.text =
                        "Cliente: Información protegida"

                    txtDireccionRepartidor.text =
                        "Dirección: Información protegida"
                } else {
                    txtClienteRepartidor.text =
                        "Cliente: ${pedido.clienteNombre}"

                    txtDireccionRepartidor.text =
                        "Dirección: ${pedido.direccionEntrega}"
                }

                txtEstadoRepartidor.text =
                    "Estado: ${pedido.estadoPedido}"

                val ganancia =
                    calcularGananciaRepartidor(
                        pedido.precioEstimado
                    )

                txtGananciaRepartidor.text =
                    if (pedidoFinalizado) {
                        "Ganancia final: $${String.format("%.2f", ganancia)}"
                    } else {
                        "Ganancia estimada: $${String.format("%.2f", ganancia)}"
                    }

                // Actualiza los datos temporales
                FirebasePedidoHelper.aplicarPedidoADatosEntrega(
                    pedido
                )

                // Configura los botones
                actualizarBotonesSegunEstado(
                    pedido.estadoPedido,
                    pedidoFinalizado
                )

                // Detiene el GPS si el pedido terminó
                if (pedidoFinalizado) {
                    UbicacionRepartidorService.detener(this)
                } else {
                    reanudarSeguimientoSiCorresponde(
                        pedido.estadoPedido
                    )
                }
            },

            onError = { mensaje ->
                Toast.makeText(
                    this,
                    mensaje,
                    Toast.LENGTH_LONG
                ).show()

                mostrarSinPedido()
            }
        )
    }

    // Cambia el estado y controla el GPS
    private fun cambiarEstadoPedido(
        nuevoEstado: String,
        mensaje: String,
        finalizado: Boolean
    ) {
        if (codigoPedidoActual.isBlank()) {
            Toast.makeText(
                this,
                "Primero acepta una solicitud",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        DatosEntrega.estadoPedido =
            nuevoEstado

        DatosEntrega.ultimaActualizacion =
            mensaje

        txtEstadoRepartidor.text =
            "Estado: $nuevoEstado"

        FirebasePedidoHelper.actualizarEstadoPedido(
            codigoPedido = codigoPedidoActual,
            nuevoEstado = nuevoEstado,
            mensajeActualizacion = mensaje,
            finalizado = finalizado,

            onSuccess = {

                // Inicia el GPS al comenzar el recorrido
                if (
                    nuevoEstado == "En camino a recogida" ||
                    nuevoEstado == "En ruta al punto de entrega"
                ) {
                    solicitarOIniciarSeguimiento()
                }

                // Detiene el GPS al terminar
                if (
                    finalizado ||
                    nuevoEstado == "Entregado" ||
                    nuevoEstado == "Finalizado" ||
                    nuevoEstado == "Cancelado"
                ) {
                    UbicacionRepartidorService.detener(this)
                }

                Toast.makeText(
                    this,
                    mensaje,
                    Toast.LENGTH_SHORT
                ).show()

                cargarPedidoAsignado(
                    codigoPedidoActual
                )
            },

            onError = { error ->
                Toast.makeText(
                    this,
                    "Error Firebase: $error",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    // Comprueba permisos antes de iniciar el GPS
    private fun solicitarOIniciarSeguimiento() {
        val permisoPreciso =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val permisoAproximado =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (permisoPreciso || permisoAproximado) {
            iniciarServicioUbicacion()
        } else {
            solicitarPermisosUbicacionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Inicia el servicio de ubicación
    private fun iniciarServicioUbicacion() {
        if (codigoPedidoActual.isBlank()) {
            return
        }

        UbicacionRepartidorService.iniciar(
            context = this,
            codigoPedido = codigoPedidoActual
        )
    }

    // Reinicia el GPS si el pedido sigue activo
    private fun reanudarSeguimientoSiCorresponde(
        estado: String
    ) {
        val estadoConSeguimiento =
            estado == "En camino a recogida" ||
                    estado == "En punto de recogida" ||
                    estado == "Paquete recogido" ||
                    estado == "En ruta al punto de entrega" ||
                    estado == "Cerca del punto de entrega"

        if (!estadoConSeguimiento) {
            return
        }

        val permisoPreciso =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val permisoAproximado =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (permisoPreciso || permisoAproximado) {
            iniciarServicioUbicacion()
        }
    }

    // Muestra la pantalla sin pedido
    private fun mostrarSinPedido() {
        txtCodigoPedidoRepartidor.text =
            "Código: Sin pedido"

        txtClienteRepartidor.text =
            "Cliente: No asignado"

        txtDireccionRepartidor.text =
            "Dirección: No asignada"

        txtEstadoRepartidor.text =
            "Estado: Selecciona una solicitud disponible"

        txtGananciaRepartidor.text =
            "Ganancia estimada: $0.00"

        habilitarBoton(btnIrRecogerPaquete, false)
        habilitarBoton(btnLleguePuntoRecogida, false)
        habilitarBoton(btnConfirmarPaqueteRecogido, false)
        habilitarBoton(btnIniciarRutaEntrega, false)
        habilitarBoton(btnActualizarUbicacion, false)
        habilitarBoton(btnTomarEvidencia, false)
        habilitarBoton(btnFinalizarEntrega, false)
    }

    // Configura los botones según el estado
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
                habilitarBoton(
                    btnIrRecogerPaquete,
                    true
                )

                habilitarBoton(
                    btnActualizarUbicacion,
                    true
                )
            }

            "En camino a recogida" -> {
                habilitarBoton(
                    btnLleguePuntoRecogida,
                    true
                )

                habilitarBoton(
                    btnActualizarUbicacion,
                    true
                )
            }

            "En punto de recogida" -> {
                habilitarBoton(
                    btnConfirmarPaqueteRecogido,
                    true
                )

                habilitarBoton(
                    btnActualizarUbicacion,
                    true
                )
            }

            "Paquete recogido" -> {
                habilitarBoton(
                    btnIniciarRutaEntrega,
                    true
                )

                habilitarBoton(
                    btnActualizarUbicacion,
                    true
                )
            }

            "En ruta al punto de entrega",
            "Cerca del punto de entrega" -> {
                habilitarBoton(
                    btnActualizarUbicacion,
                    true
                )

                habilitarBoton(
                    btnTomarEvidencia,
                    true
                )

                habilitarBoton(
                    btnFinalizarEntrega,
                    true
                )
            }

            "Entregado" -> {
                habilitarBoton(
                    btnFinalizarEntrega,
                    true
                )
            }

            else -> {
                habilitarBoton(
                    btnIrRecogerPaquete,
                    true
                )

                habilitarBoton(
                    btnActualizarUbicacion,
                    true
                )
            }
        }
    }

    // Cambia el estado visual del botón
    private fun habilitarBoton(
        boton: Button,
        habilitado: Boolean
    ) {
        boton.isEnabled =
            habilitado

        boton.alpha =
            if (habilitado) 1.0f else 0.45f
    }

    // Calcula el 70 por ciento para el repartidor
    private fun calcularGananciaRepartidor(
        precioEstimado: Double
    ): Double {
        return precioEstimado * 0.70
    }

    override fun onResume() {
        super.onResume()

        // Actualiza el pedido al regresar
        if (
            ::txtEstadoRepartidor.isInitialized &&
            codigoPedidoActual.isNotBlank()
        ) {
            cargarPedidoAsignado(
                codigoPedidoActual
            )
        }
    }
}
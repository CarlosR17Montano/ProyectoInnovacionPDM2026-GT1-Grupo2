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

// Pantalla que muestra al cliente el seguimiento de un pedido seleccionado
class ClienteActivity : ComponentActivity() {

    // Controles que muestran la información del pedido
    private lateinit var txtCodigoPedidoCliente: TextView
    private lateinit var txtNombreCliente: TextView
    private lateinit var txtRepartidorCliente: TextView
    private lateinit var txtDireccionCliente: TextView
    private lateinit var txtEstadoCliente: TextView
    private lateinit var txtUltimaActualizacionCliente: TextView
    private lateinit var txtEvidenciaCliente: TextView

    // Botones disponibles para el cliente
    private lateinit var btnVerUbicacion: Button
    private lateinit var btnVerEvidencia: Button

    // Listener utilizado para observar cambios del pedido en Firebase
    private var listenerFirebase: ValueEventListener? = null

    // Código del pedido que está consultando el cliente
    private var codigoPedidoActual: String = ""

    // Indica si el pedido seleccionado ya terminó
    private var pedidoFinalizadoSeleccionado: Boolean = false

    // Configura la pantalla cuando se abre
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Define los colores de las barras del sistema
        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        // Carga el diseño de la pantalla
        setContentView(R.layout.activity_cliente)

        // Relaciona los botones del XML con Kotlin
        btnVerUbicacion = findViewById(R.id.btnVerUbicacion)
        btnVerEvidencia = findViewById(R.id.btnVerEvidencia)

        // Relaciona los textos del XML con Kotlin
        txtCodigoPedidoCliente = findViewById(R.id.txtCodigoPedidoCliente)
        txtNombreCliente = findViewById(R.id.txtNombreCliente)
        txtRepartidorCliente = findViewById(R.id.txtRepartidorCliente)
        txtDireccionCliente = findViewById(R.id.txtDireccionCliente)
        txtEstadoCliente = findViewById(R.id.txtEstadoCliente)
        txtUltimaActualizacionCliente =
            findViewById(R.id.txtUltimaActualizacionCliente)
        txtEvidenciaCliente = findViewById(R.id.txtEvidenciaCliente)

        // Recibe el código enviado desde el historial
        codigoPedidoActual =
            intent.getStringExtra("codigoPedido")
                ?: DatosEntrega.codigoPedido

        // Valida que se haya recibido un código
        if (codigoPedidoActual.isBlank()) {
            Toast.makeText(
                this,
                "No se recibió el código del pedido",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        // Consulta los datos reales del pedido
        cargarPedidoSeleccionado(codigoPedidoActual)

        // Abre el mapa para visualizar la ubicación del repartidor
        btnVerUbicacion.setOnClickListener {
            val intentMapa = Intent(
                this,
                MapaActivity::class.java
            )

            // Indica que el mapa se abre en modo cliente
            intentMapa.putExtra("modo", "cliente")

            // Envía el pedido que debe mostrarse en el mapa
            intentMapa.putExtra(
                "codigoPedido",
                codigoPedidoActual
            )

            startActivity(intentMapa)
        }

        // Abre la evidencia final en modo de solo lectura
        btnVerEvidencia.setOnClickListener {
            val intentEvidencia = Intent(
                this,
                EvidenciaActivity::class.java
            )

            // Indica que el usuario solamente visualizará la evidencia
            intentEvidencia.putExtra("modo", "cliente")

            // Envía el pedido al que pertenece la evidencia
            intentEvidencia.putExtra(
                "codigoPedido",
                codigoPedidoActual
            )

            // El cliente debe visualizar la evidencia de entrega
            intentEvidencia.putExtra(
                "tipoEvidencia",
                "entrega"
            )

            startActivity(intentEvidencia)
        }
    }

    // Se ejecuta cuando la pantalla vuelve a estar visible
    override fun onResume() {
        super.onResume()

        // Solicita permiso para mostrar notificaciones
        NotificacionesClienteHelper
            .solicitarPermisoNotificaciones(this)

        // Inicia la escucha de notificaciones nuevas
        NotificacionesClienteHelper
            .iniciarEscucha(this)

        // Refresca la información mostrada
        if (::txtEstadoCliente.isInitialized) {
            actualizarDatosPedido()
        }
    }

    // Se ejecuta cuando el cliente sale temporalmente de la pantalla
    override fun onPause() {
        super.onPause()

        // Detiene el listener de notificaciones para evitar duplicados
        NotificacionesClienteHelper.detenerEscucha()
    }

    // Carga el pedido seleccionado desde Firebase
    private fun cargarPedidoSeleccionado(codigoPedido: String) {
        FirebasePedidoHelper.cargarPedidoPorCodigo(
            codigoPedido = codigoPedido,

            onSuccess = { pedido ->

                // Copia los datos del pedido al objeto temporal
                FirebasePedidoHelper.aplicarPedidoADatosEntrega(
                    pedido
                )

                // Determina si el pedido ya terminó
                pedidoFinalizadoSeleccionado =
                    pedido.finalizado ||
                            pedido.estadoPedido.equals(
                                "Entregado",
                                ignoreCase = true
                            ) ||
                            pedido.estadoPedido.equals(
                                "Finalizado",
                                ignoreCase = true
                            )

                // Actualiza los textos de la pantalla
                actualizarDatosPedido()

                // Escucha cambios en tiempo real solamente si sigue activo
                if (!pedidoFinalizadoSeleccionado) {
                    iniciarEscuchaFirebase()
                }
            },

            onError = { mensaje ->

                // Muestra el error recibido desde Firebase
                Toast.makeText(
                    this,
                    mensaje,
                    Toast.LENGTH_LONG
                ).show()

                // Muestra los datos temporales disponibles
                actualizarDatosPedido()
            }
        )
    }

    // Escucha los cambios de ubicación y estado del pedido
    private fun iniciarEscuchaFirebase() {

        // Elimina cualquier listener anterior
        FirebaseEntregaHelper.detenerEscuchaPedido(
            listenerFirebase
        )

        // Inicia un nuevo listener para el pedido seleccionado
        listenerFirebase =
            FirebaseEntregaHelper.escucharUbicacionPedido(

                onChange = { datos ->

                    // Actualiza los datos temporales del pedido
                    DatosEntrega.estadoPedido =
                        datos.estadoPedido

                    DatosEntrega.latitud =
                        datos.latitud

                    DatosEntrega.longitud =
                        datos.longitud

                    DatosEntrega.ultimaActualizacion =
                        datos.ultimaActualizacion

                    DatosEntrega.evidenciaRegistrada =
                        datos.evidenciaRegistrada

                    DatosEntrega.rutaFotoEvidencia =
                        datos.rutaFotoEvidencia

                    // Comprueba si el pedido terminó
                    pedidoFinalizadoSeleccionado =
                        DatosEntrega.estadoPedido.equals(
                            "Entregado",
                            ignoreCase = true
                        ) ||
                                DatosEntrega.estadoPedido.equals(
                                    "Finalizado",
                                    ignoreCase = true
                                )

                    // Actualiza la información visible
                    actualizarDatosPedido()

                    // Detiene la ubicación en tiempo real si terminó
                    if (pedidoFinalizadoSeleccionado) {
                        FirebaseEntregaHelper
                            .detenerEscuchaPedido(
                                listenerFirebase
                            )

                        listenerFirebase = null
                    }
                },

                onError = { mensaje ->

                    // Informa si ocurre un error de lectura
                    Toast.makeText(
                        this,
                        "Error Firebase: $mensaje",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
    }

    // Coloca los datos actuales del pedido en los textos
    private fun actualizarDatosPedido() {

        // Muestra la información general del pedido
        txtCodigoPedidoCliente.text =
            "Pedido: ${DatosEntrega.codigoPedido}"

        txtNombreCliente.text =
            "Cliente: ${DatosEntrega.clientePedido}"

        txtRepartidorCliente.text =
            "Repartidor: ${DatosEntrega.repartidorPedido}"

        txtDireccionCliente.text =
            "Dirección: ${DatosEntrega.direccionPedido}"

        // Muestra el estado y la última actualización
        txtEstadoCliente.text =
            "Estado actual: ${DatosEntrega.estadoPedido}"

        txtUltimaActualizacionCliente.text =
            "Última actualización: ${DatosEntrega.ultimaActualizacion}"

        // Informa si ya existe evidencia final
        txtEvidenciaCliente.text =
            if (DatosEntrega.evidenciaRegistrada) {
                "Evidencia registrada: Sí"
            } else {
                "Evidencia registrada: No"
            }

        // Ajusta los botones según el estado
        configurarBotonesPorEstado()
    }

    // Muestra u oculta botones según el estado del pedido
    private fun configurarBotonesPorEstado() {

        // Comprueba si el pedido ya terminó
        val pedidoEntregado =
            pedidoFinalizadoSeleccionado ||
                    DatosEntrega.estadoPedido.equals(
                        "Entregado",
                        ignoreCase = true
                    ) ||
                    DatosEntrega.estadoPedido.equals(
                        "Finalizado",
                        ignoreCase = true
                    )

        if (pedidoEntregado) {

            // Oculta el mapa porque ya no necesita seguimiento
            btnVerUbicacion.visibility = View.GONE

            // Muestra el botón solamente si existe evidencia
            btnVerEvidencia.visibility =
                if (DatosEntrega.evidenciaRegistrada) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            // Cambia el texto del botón
            btnVerEvidencia.text =
                "Ver evidencia entregada"

        } else {

            // Permite ver la ubicación del pedido activo
            btnVerUbicacion.visibility = View.VISIBLE

            // Muestra evidencia solo cuando esté disponible
            btnVerEvidencia.visibility =
                if (DatosEntrega.evidenciaRegistrada) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            btnVerEvidencia.text =
                "Ver evidencia de entrega"
        }
    }

    // Se ejecuta cuando la pantalla se destruye
    override fun onDestroy() {
        super.onDestroy()

        // Elimina el listener del seguimiento del pedido
        FirebaseEntregaHelper.detenerEscuchaPedido(
            listenerFirebase
        )

        // Limpia la referencia local
        listenerFirebase = null
    }
}
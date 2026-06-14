package sv.edu.ues.entregatrack

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.bumptech.glide.Glide
import com.google.firebase.database.ValueEventListener
import java.util.Locale

// Muestra el detalle y seguimiento del pedido
class ClienteActivity : ComponentActivity() {

    // Datos principales del pedido
    private lateinit var txtCodigoPedidoCliente: TextView
    private lateinit var txtTipoServicioCliente: TextView
    private lateinit var txtNombreCliente: TextView
    private lateinit var txtTelefonoCliente: TextView
    private lateinit var txtRepartidorCliente: TextView

    // Direcciones del pedido
    private lateinit var txtDireccionRecogidaCliente: TextView
    private lateinit var txtDireccionCliente: TextView
    private lateinit var txtReferenciaCliente: TextView

    // Información del mandado
    private lateinit var txtDescripcionPedidoCliente: TextView
    private lateinit var txtIndicacionesCliente: TextView
    private lateinit var txtDistanciaPrecioCliente: TextView

    // Estado del pedido
    private lateinit var txtEstadoCliente: TextView
    private lateinit var txtUltimaActualizacionCliente: TextView
    private lateinit var txtEvidenciaCliente: TextView

    // Galería de imágenes
    private lateinit var layoutGaleriaEvidencias: LinearLayout

    // Anexo del cliente
    private lateinit var imgAnexoClienteDetalle: ImageView
    private lateinit var txtAnexoClienteEstado: TextView

    // Evidencia de recogida
    private lateinit var imgEvidenciaRecogidaDetalle: ImageView
    private lateinit var txtEvidenciaRecogidaEstado: TextView

    // Evidencia de entrega
    private lateinit var imgEvidenciaEntregaDetalle: ImageView
    private lateinit var txtEvidenciaEntregaEstado: TextView

    // Botones de la pantalla
    private lateinit var btnVerUbicacion: Button
    private lateinit var btnVerEvidencia: Button
    private lateinit var btnVolverHistorialCliente: Button

    // Listener del seguimiento
    private var listenerFirebase: ValueEventListener? = null

    // Pedido seleccionado
    private var codigoPedidoActual: String = ""
    private var pedidoActual: PedidoFirebase? = null

    // Estados de la pantalla
    private var pedidoFinalizadoSeleccionado = false
    private var galeriaVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        setContentView(R.layout.activity_cliente)

        inicializarControles()

        codigoPedidoActual =
            intent.getStringExtra("codigoPedido")
                ?: DatosEntrega.codigoPedido

        if (codigoPedidoActual.isBlank()) {
            Toast.makeText(
                this,
                "No se recibió el código del pedido",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        // Abre el mapa del repartidor
        btnVerUbicacion.setOnClickListener {
            abrirMapaPedido()
        }

        // Muestra u oculta las imágenes
        btnVerEvidencia.setOnClickListener {
            alternarGaleria()
        }

        // Regresa al historial
        btnVolverHistorialCliente.setOnClickListener {
            finish()
        }
    }

    // Relaciona los controles con el XML
    private fun inicializarControles() {
        txtCodigoPedidoCliente = findViewById(R.id.txtCodigoPedidoCliente)
        txtTipoServicioCliente = findViewById(R.id.txtTipoServicioCliente)
        txtNombreCliente = findViewById(R.id.txtNombreCliente)
        txtTelefonoCliente = findViewById(R.id.txtTelefonoCliente)
        txtRepartidorCliente = findViewById(R.id.txtRepartidorCliente)

        txtDireccionRecogidaCliente =
            findViewById(R.id.txtDireccionRecogidaCliente)

        txtDireccionCliente =
            findViewById(R.id.txtDireccionCliente)

        txtReferenciaCliente =
            findViewById(R.id.txtReferenciaCliente)

        txtDescripcionPedidoCliente =
            findViewById(R.id.txtDescripcionPedidoCliente)

        txtIndicacionesCliente =
            findViewById(R.id.txtIndicacionesCliente)

        txtDistanciaPrecioCliente =
            findViewById(R.id.txtDistanciaPrecioCliente)

        txtEstadoCliente =
            findViewById(R.id.txtEstadoCliente)

        txtUltimaActualizacionCliente =
            findViewById(R.id.txtUltimaActualizacionCliente)

        txtEvidenciaCliente =
            findViewById(R.id.txtEvidenciaCliente)

        layoutGaleriaEvidencias =
            findViewById(R.id.layoutGaleriaEvidencias)

        imgAnexoClienteDetalle =
            findViewById(R.id.imgAnexoClienteDetalle)

        txtAnexoClienteEstado =
            findViewById(R.id.txtAnexoClienteEstado)

        imgEvidenciaRecogidaDetalle =
            findViewById(R.id.imgEvidenciaRecogidaDetalle)

        txtEvidenciaRecogidaEstado =
            findViewById(R.id.txtEvidenciaRecogidaEstado)

        imgEvidenciaEntregaDetalle =
            findViewById(R.id.imgEvidenciaEntregaDetalle)

        txtEvidenciaEntregaEstado =
            findViewById(R.id.txtEvidenciaEntregaEstado)

        btnVerUbicacion =
            findViewById(R.id.btnVerUbicacion)

        btnVerEvidencia =
            findViewById(R.id.btnVerEvidencia)

        btnVolverHistorialCliente =
            findViewById(R.id.btnVolverHistorialCliente)
    }

    override fun onResume() {
        super.onResume()

        // Solicita permiso para notificaciones
        NotificacionesClienteHelper
            .solicitarPermisoNotificaciones(this)

        // Escucha las notificaciones nuevas
        NotificacionesClienteHelper
            .iniciarEscucha(this)

        // Recarga el pedido y sus imágenes
        if (codigoPedidoActual.isNotBlank()) {
            cargarPedidoSeleccionado()
        }
    }

    override fun onPause() {
        super.onPause()

        // Detiene las notificaciones temporales
        NotificacionesClienteHelper.detenerEscucha()
    }

    // Consulta el pedido completo
    private fun cargarPedidoSeleccionado() {
        FirebasePedidoHelper.cargarPedidoPorCodigo(
            codigoPedido = codigoPedidoActual,

            onSuccess = { pedido ->
                pedidoActual = pedido

                FirebasePedidoHelper.aplicarPedidoADatosEntrega(
                    pedido
                )

                pedidoFinalizadoSeleccionado =
                    pedido.finalizado ||
                            pedido.estadoPedido.equals(
                                "Entregado",
                                true
                            ) ||
                            pedido.estadoPedido.equals(
                                "Finalizado",
                                true
                            )

                mostrarDatosPedido(pedido)
                configurarImagenes(pedido)

                if (!pedidoFinalizadoSeleccionado) {
                    iniciarEscuchaFirebase()
                } else {
                    FirebaseEntregaHelper.detenerEscuchaPedido(
                        listenerFirebase
                    )

                    listenerFirebase = null
                }
            },

            onError = { mensaje ->
                Toast.makeText(
                    this,
                    mensaje,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    // Muestra los datos del pedido
    private fun mostrarDatosPedido(
        pedido: PedidoFirebase
    ) {
        txtCodigoPedidoCliente.text =
            "Pedido: ${pedido.codigoPedido}"

        txtTipoServicioCliente.text =
            "Servicio: ${pedido.tipoServicio}"

        txtNombreCliente.text =
            "Cliente: ${pedido.clienteNombre}"

        txtTelefonoCliente.text =
            "Teléfono: ${pedido.telefonoCliente}"

        txtRepartidorCliente.text =
            "Repartidor: ${pedido.repartidorNombre}"

        txtDireccionRecogidaCliente.text =
            "Recoger en: ${pedido.direccionRecogida}"

        txtDireccionCliente.text =
            "Entregar en: ${pedido.direccionEntrega}"

        txtReferenciaCliente.text =
            "Referencia: ${
                pedido.referenciaUbicacion.ifBlank {
                    "Sin referencia adicional"
                }
            }"

        txtDescripcionPedidoCliente.text =
            "Mandado: ${pedido.descripcionPedido}"

        txtIndicacionesCliente.text =
            "Indicaciones: ${
                pedido.indicacionesRepartidor.ifBlank {
                    "Sin indicaciones adicionales"
                }
            }"

        val distancia =
            String.format(
                Locale.getDefault(),
                "%.2f",
                pedido.distanciaKm
            )

        val precio =
            String.format(
                Locale.getDefault(),
                "%.2f",
                pedido.precioEstimado
            )

        txtDistanciaPrecioCliente.text =
            "Distancia: $distancia km\nPrecio estimado: $$precio"

        txtEstadoCliente.text =
            "Estado actual: ${pedido.estadoPedido}"

        txtUltimaActualizacionCliente.text =
            "Última actualización: ${pedido.ultimaActualizacion}"

        val cantidadImagenes =
            contarImagenes(pedido)

        txtEvidenciaCliente.text =
            "Imágenes registradas: $cantidadImagenes de 3"

        configurarBotones()
    }

    // Prepara las tres imágenes del pedido
    private fun configurarImagenes(
        pedido: PedidoFirebase
    ) {
        mostrarImagen(
            url = pedido.urlAnexoCliente,
            imageView = imgAnexoClienteDetalle,
            textView = txtAnexoClienteEstado,
            disponible = pedido.tipoAnexoCliente.ifBlank {
                "Anexo agregado por el cliente"
            },
            noDisponible = "El cliente no agregó ningún anexo"
        )

        mostrarImagen(
            url = pedido.urlEvidenciaRecogida,
            imageView = imgEvidenciaRecogidaDetalle,
            textView = txtEvidenciaRecogidaEstado,
            disponible = "Foto tomada al recoger el paquete",
            noDisponible = "La recogida todavía no tiene evidencia"
        )

        val urlEntrega =
            pedido.urlEvidenciaEntrega.ifBlank {
                pedido.rutaFotoEvidencia
            }

        mostrarImagen(
            url = urlEntrega,
            imageView = imgEvidenciaEntregaDetalle,
            textView = txtEvidenciaEntregaEstado,
            disponible = "Foto tomada al entregar el paquete",
            noDisponible = "La entrega todavía no tiene evidencia"
        )
    }

    // Carga una imagen usando Glide
    private fun mostrarImagen(
        url: String,
        imageView: ImageView,
        textView: TextView,
        disponible: String,
        noDisponible: String
    ) {
        if (url.isBlank()) {
            Glide.with(this).clear(imageView)

            imageView.setImageDrawable(null)
            imageView.visibility = View.GONE
            textView.text = noDisponible
            return
        }

        imageView.visibility = View.VISIBLE
        textView.text = disponible

        Glide.with(this)
            .load(url)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_report_image)
            .fitCenter()
            .into(imageView)
    }

    // Cuenta las imágenes disponibles
    private fun contarImagenes(
        pedido: PedidoFirebase
    ): Int {
        var cantidad = 0

        if (pedido.urlAnexoCliente.isNotBlank()) {
            cantidad++
        }

        if (pedido.urlEvidenciaRecogida.isNotBlank()) {
            cantidad++
        }

        if (
            pedido.urlEvidenciaEntrega.isNotBlank() ||
            pedido.rutaFotoEvidencia.isNotBlank()
        ) {
            cantidad++
        }

        return cantidad
    }

    // Muestra u oculta la galería
    private fun alternarGaleria() {
        if (pedidoActual == null) {
            Toast.makeText(
                this,
                "El pedido todavía se está cargando",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        galeriaVisible = !galeriaVisible

        layoutGaleriaEvidencias.visibility =
            if (galeriaVisible) {
                View.VISIBLE
            } else {
                View.GONE
            }

        btnVerEvidencia.text =
            if (galeriaVisible) {
                "Ocultar imágenes del pedido"
            } else {
                "Ver imágenes del pedido"
            }
    }

    // Abre el seguimiento en el mapa
    private fun abrirMapaPedido() {
        val intentMapa =
            Intent(
                this,
                MapaActivity::class.java
            )

        intentMapa.putExtra(
            "modo",
            "cliente"
        )

        intentMapa.putExtra(
            "codigoPedido",
            codigoPedidoActual
        )

        startActivity(intentMapa)
    }

    // Escucha cambios del pedido activo
    private fun iniciarEscuchaFirebase() {
        FirebaseEntregaHelper.detenerEscuchaPedido(
            listenerFirebase
        )

        listenerFirebase =
            FirebaseEntregaHelper.escucharUbicacionPedido(
                onChange = { datos ->
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

                    txtEstadoCliente.text =
                        "Estado actual: ${datos.estadoPedido}"

                    txtUltimaActualizacionCliente.text =
                        "Última actualización: ${datos.ultimaActualizacion}"

                    pedidoFinalizadoSeleccionado =
                        datos.estadoPedido.equals(
                            "Entregado",
                            true
                        ) ||
                                datos.estadoPedido.equals(
                                    "Finalizado",
                                    true
                                )

                    if (
                        datos.evidenciaRegistrada &&
                        pedidoActual
                            ?.urlEvidenciaEntrega
                            .isNullOrBlank()
                    ) {
                        cargarPedidoSeleccionado()
                    }

                    configurarBotones()

                    if (pedidoFinalizadoSeleccionado) {
                        FirebaseEntregaHelper.detenerEscuchaPedido(
                            listenerFirebase
                        )

                        listenerFirebase = null
                    }
                },

                onError = { mensaje ->
                    Toast.makeText(
                        this,
                        "Error Firebase: $mensaje",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
    }

    // Ajusta los botones por estado
    private fun configurarBotones() {
        btnVerUbicacion.visibility =
            if (pedidoFinalizadoSeleccionado) {
                View.GONE
            } else {
                View.VISIBLE
            }

        btnVerEvidencia.visibility =
            View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()

        // Libera el listener de Firebase
        FirebaseEntregaHelper.detenerEscuchaPedido(
            listenerFirebase
        )

        listenerFirebase = null
    }
}
package sv.edu.ues.entregatrack

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

// Pantalla que muestra el historial de pedidos del cliente
class HistorialPedidosActivity : ComponentActivity() {

    // Contenedor donde se agregan las tarjetas de los pedidos
    private lateinit var layoutListaPedidos: LinearLayout

    // Configura la pantalla cuando se abre
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Define los colores principales de la aplicación
        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        // Carga el diseño XML de la pantalla
        setContentView(R.layout.activity_historial_pedidos)

        // Relaciona el contenedor del XML con Kotlin
        layoutListaPedidos = findViewById(R.id.layoutListaPedidos)

        // Obtiene el botón para crear un nuevo pedido
        val btnNuevoPedido = findViewById<Button>(R.id.btnNuevoPedido)

        // Abre la pantalla de creación de pedidos
        btnNuevoPedido.setOnClickListener {
            val intentNuevoPedido = Intent(
                this,
                CrearPedidoActivity::class.java
            )

            startActivity(intentNuevoPedido)
        }
    }

    // Se ejecuta cuando la pantalla queda visible
    override fun onResume() {
        super.onResume()

        // Solicita permiso de notificaciones en Android 13 o superior
        NotificacionesClienteHelper
            .solicitarPermisoNotificaciones(this)

        // Inicia la escucha de notificaciones nuevas
        NotificacionesClienteHelper
            .iniciarEscucha(this)

        // Actualiza la lista de pedidos del cliente
        if (::layoutListaPedidos.isInitialized) {
            cargarHistorialPedidos()
        }
    }

    // Se ejecuta cuando el usuario cambia de pantalla
    override fun onPause() {
        super.onPause()

        // Detiene la escucha para evitar listeners duplicados
        NotificacionesClienteHelper.detenerEscucha()
    }

    // Consulta en Firebase los pedidos del cliente autenticado
    private fun cargarHistorialPedidos() {
        FirebasePedidoHelper.cargarPedidosCliente(
            onSuccess = { pedidos ->
                // Muestra los pedidos recuperados
                mostrarPedidos(pedidos)
            },
            onError = { mensaje ->
                // Muestra el error recibido desde Firebase
                Toast.makeText(
                    this,
                    mensaje,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    // Dibuja una tarjeta por cada pedido recuperado
    private fun mostrarPedidos(pedidos: List<PedidoFirebase>) {
        // Elimina las tarjetas anteriores
        layoutListaPedidos.removeAllViews()

        // Muestra un mensaje cuando no existen pedidos
        if (pedidos.isEmpty()) {
            val txtSinPedidos = TextView(this)

            txtSinPedidos.text = "Aún no tienes pedidos registrados"
            txtSinPedidos.textSize = 18f
            txtSinPedidos.setTextColor(Color.parseColor("#777777"))
            txtSinPedidos.textAlignment = TextView.TEXT_ALIGNMENT_CENTER

            layoutListaPedidos.addView(txtSinPedidos)
            return
        }

        // Recorre la lista y agrega una tarjeta por pedido
        pedidos.forEachIndexed { index, pedido ->
            val tarjeta = crearTarjetaPedido(
                pedido = pedido,
                posicion = index
            )

            layoutListaPedidos.addView(tarjeta)
        }
    }

    // Construye visualmente una tarjeta para un pedido
    private fun crearTarjetaPedido(
        pedido: PedidoFirebase,
        posicion: Int
    ): LinearLayout {

        // Crea el contenedor principal de la tarjeta
        val tarjeta = LinearLayout(this)

        tarjeta.orientation = LinearLayout.VERTICAL
        tarjeta.setBackgroundResource(R.drawable.bg_card_login)
        tarjeta.elevation = 8f
        tarjeta.setPadding(24, 22, 24, 22)

        // Define el tamaño y separación de la tarjeta
        val parametros = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        parametros.setMargins(0, 0, 0, 22)
        tarjeta.layoutParams = parametros

        // Obtiene la etiqueta visual del pedido
        val etiqueta = obtenerEtiquetaPedido(
            pedido = pedido,
            posicion = posicion
        )

        // Muestra el código y la etiqueta
        val txtCodigo = TextView(this)
        txtCodigo.text = "${pedido.codigoPedido}  •  $etiqueta"
        txtCodigo.textSize = 20f
        txtCodigo.setTextColor(Color.parseColor("#D81B60"))
        txtCodigo.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        // Muestra el tipo de servicio
        val txtTipoServicio = TextView(this)
        txtTipoServicio.text =
            "Servicio: ${obtenerTextoSeguro(pedido.tipoServicio)}"
        txtTipoServicio.textSize = 15f
        txtTipoServicio.setTextColor(Color.parseColor("#555555"))

        // Muestra la dirección de recogida
        val txtRecogida = TextView(this)
        txtRecogida.text =
            "Recoger en: ${obtenerTextoSeguro(pedido.direccionRecogida)}"
        txtRecogida.textSize = 15f
        txtRecogida.setTextColor(Color.parseColor("#555555"))

        // Muestra la dirección de entrega
        val txtEntrega = TextView(this)
        txtEntrega.text =
            "Entregar en: ${obtenerTextoSeguro(pedido.direccionEntrega)}"
        txtEntrega.textSize = 15f
        txtEntrega.setTextColor(Color.parseColor("#555555"))

        // Muestra la descripción del mandado
        val txtDescripcion = TextView(this)
        txtDescripcion.text =
            "Mandado: ${obtenerTextoSeguro(pedido.descripcionPedido)}"
        txtDescripcion.textSize = 15f
        txtDescripcion.setTextColor(Color.parseColor("#555555"))

        // Muestra el precio estimado
        val txtPrecio = TextView(this)
        txtPrecio.text =
            "Precio estimado: $${String.format("%.2f", pedido.precioEstimado)}"
        txtPrecio.textSize = 15f
        txtPrecio.setTextColor(Color.parseColor("#D81B60"))
        txtPrecio.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        // Muestra el estado actual
        val txtEstado = TextView(this)
        txtEstado.text =
            "Estado: ${obtenerTextoSeguro(pedido.estadoPedido)}"
        txtEstado.textSize = 16f
        txtEstado.setTextColor(Color.parseColor("#6A1B9A"))
        txtEstado.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        // Muestra la fecha del pedido
        val txtFecha = TextView(this)
        txtFecha.text =
            "Fecha: ${obtenerTextoSeguro(pedido.fechaCreacion)}"
        txtFecha.textSize = 14f
        txtFecha.setTextColor(Color.parseColor("#777777"))

        // Agrega los textos a la tarjeta
        tarjeta.addView(txtCodigo)
        tarjeta.addView(txtTipoServicio)
        tarjeta.addView(txtRecogida)
        tarjeta.addView(txtEntrega)
        tarjeta.addView(txtDescripcion)
        tarjeta.addView(txtPrecio)
        tarjeta.addView(txtEstado)
        tarjeta.addView(txtFecha)

        // Abre el detalle cuando el cliente toca la tarjeta
        tarjeta.setOnClickListener {
            // Copia los datos del pedido al objeto temporal
            FirebasePedidoHelper.aplicarPedidoADatosEntrega(pedido)

            // Prepara la pantalla de detalle
            val intentDetalle = Intent(
                this,
                ClienteActivity::class.java
            )

            // Envía el código del pedido seleccionado
            intentDetalle.putExtra(
                "codigoPedido",
                pedido.codigoPedido
            )

            startActivity(intentDetalle)
        }

        return tarjeta
    }

    // Devuelve una etiqueta según el estado del pedido
    private fun obtenerEtiquetaPedido(
        pedido: PedidoFirebase,
        posicion: Int
    ): String {
        return when {
            // Pedido cerrado completamente
            pedido.finalizado -> "Finalizado"

            // Pedido esperando repartidor
            pedido.estadoPedido.equals(
                "Solicitado",
                ignoreCase = true
            ) -> "Solicitado"

            // Pedido ya entregado
            pedido.estadoPedido.equals(
                "Entregado",
                ignoreCase = true
            ) -> "Entregado"

            // Pedido marcado como finalizado
            pedido.estadoPedido.equals(
                "Finalizado",
                ignoreCase = true
            ) -> "Finalizado"

            // Primer pedido activo de la lista
            posicion == 0 -> "Pedido activo"

            // Demás pedidos en proceso
            else -> "En proceso"
        }
    }

    // Evita mostrar valores vacíos
    private fun obtenerTextoSeguro(valor: String): String {
        return if (valor.isBlank()) {
            "No especificado"
        } else {
            valor
        }
    }
}

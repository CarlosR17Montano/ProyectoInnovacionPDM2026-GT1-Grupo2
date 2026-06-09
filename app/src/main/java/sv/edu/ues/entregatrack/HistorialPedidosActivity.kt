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

class HistorialPedidosActivity : ComponentActivity() {

    private lateinit var layoutListaPedidos: LinearLayout

    // Pantalla que muestra el historial de pedidos del cliente
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Colores principales de la app
        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        setContentView(R.layout.activity_historial_pedidos)

        layoutListaPedidos = findViewById(R.id.layoutListaPedidos)

        val btnNuevoPedido = findViewById<Button>(R.id.btnNuevoPedido)

        // Abre la pantalla para que el cliente solicite un mandado o entrega
        btnNuevoPedido.setOnClickListener {
            val intent = Intent(this, CrearPedidoActivity::class.java)
            startActivity(intent)
        }
    }

    // Refresca el historial cada vez que se entra o se regresa a esta pantalla
    override fun onResume() {
        super.onResume()

        if (::layoutListaPedidos.isInitialized) {
            cargarHistorialPedidos()
        }
    }

    // Consulta los pedidos del cliente en Firebase
    private fun cargarHistorialPedidos() {
        FirebasePedidoHelper.cargarPedidosCliente(
            onSuccess = { pedidos ->
                mostrarPedidos(pedidos)
            },
            onError = { mensaje ->
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            }
        )
    }

    // Dibuja las tarjetas de pedidos en pantalla
    private fun mostrarPedidos(pedidos: List<PedidoFirebase>) {
        layoutListaPedidos.removeAllViews()

        if (pedidos.isEmpty()) {
            val txtSinPedidos = TextView(this)
            txtSinPedidos.text = "Aún no tienes pedidos registrados"
            txtSinPedidos.textSize = 18f
            txtSinPedidos.setTextColor(Color.parseColor("#777777"))
            txtSinPedidos.textAlignment = TextView.TEXT_ALIGNMENT_CENTER

            layoutListaPedidos.addView(txtSinPedidos)
            return
        }

        pedidos.forEachIndexed { index, pedido ->
            val tarjeta = crearTarjetaPedido(pedido, index)
            layoutListaPedidos.addView(tarjeta)
        }
    }

    // Crea una tarjeta visual por cada pedido
    private fun crearTarjetaPedido(pedido: PedidoFirebase, posicion: Int): LinearLayout {
        val tarjeta = LinearLayout(this)
        tarjeta.orientation = LinearLayout.VERTICAL
        tarjeta.setBackgroundResource(R.drawable.bg_card_login)
        tarjeta.elevation = 8f
        tarjeta.setPadding(24, 22, 24, 22)

        val parametros = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        parametros.setMargins(0, 0, 0, 22)
        tarjeta.layoutParams = parametros

        val etiqueta = obtenerEtiquetaPedido(pedido, posicion)

        val txtCodigo = TextView(this)
        txtCodigo.text = "${pedido.codigoPedido}  •  $etiqueta"
        txtCodigo.textSize = 20f
        txtCodigo.setTextColor(Color.parseColor("#D81B60"))
        txtCodigo.setTypeface(null, android.graphics.Typeface.BOLD)

        val txtTipoServicio = TextView(this)
        txtTipoServicio.text = "Servicio: ${obtenerTextoSeguro(pedido.tipoServicio)}"
        txtTipoServicio.textSize = 15f
        txtTipoServicio.setTextColor(Color.parseColor("#555555"))

        val txtRecogida = TextView(this)
        txtRecogida.text = "Recoger en: ${obtenerTextoSeguro(pedido.direccionRecogida)}"
        txtRecogida.textSize = 15f
        txtRecogida.setTextColor(Color.parseColor("#555555"))

        val txtEntrega = TextView(this)
        txtEntrega.text = "Entregar en: ${obtenerTextoSeguro(pedido.direccionEntrega)}"
        txtEntrega.textSize = 15f
        txtEntrega.setTextColor(Color.parseColor("#555555"))

        val txtDescripcion = TextView(this)
        txtDescripcion.text = "Mandado: ${obtenerTextoSeguro(pedido.descripcionPedido)}"
        txtDescripcion.textSize = 15f
        txtDescripcion.setTextColor(Color.parseColor("#555555"))

        val txtPrecio = TextView(this)
        txtPrecio.text = "Precio estimado: $${String.format("%.2f", pedido.precioEstimado)}"
        txtPrecio.textSize = 15f
        txtPrecio.setTextColor(Color.parseColor("#D81B60"))
        txtPrecio.setTypeface(null, android.graphics.Typeface.BOLD)

        val txtEstado = TextView(this)
        txtEstado.text = "Estado: ${pedido.estadoPedido}"
        txtEstado.textSize = 16f
        txtEstado.setTextColor(Color.parseColor("#6A1B9A"))
        txtEstado.setTypeface(null, android.graphics.Typeface.BOLD)

        val txtFecha = TextView(this)
        txtFecha.text = "Fecha: ${pedido.fechaCreacion}"
        txtFecha.textSize = 14f
        txtFecha.setTextColor(Color.parseColor("#777777"))

        tarjeta.addView(txtCodigo)
        tarjeta.addView(txtTipoServicio)
        tarjeta.addView(txtRecogida)
        tarjeta.addView(txtEntrega)
        tarjeta.addView(txtDescripcion)
        tarjeta.addView(txtPrecio)
        tarjeta.addView(txtEstado)
        tarjeta.addView(txtFecha)

        // Al tocar el pedido se abre el detalle
        tarjeta.setOnClickListener {
            FirebasePedidoHelper.aplicarPedidoADatosEntrega(pedido)

            val intent = Intent(this, ClienteActivity::class.java)
            intent.putExtra("codigoPedido", pedido.codigoPedido)
            startActivity(intent)
        }

        return tarjeta
    }

    // Define la etiqueta visual del pedido
    private fun obtenerEtiquetaPedido(pedido: PedidoFirebase, posicion: Int): String {
        return when {
            pedido.finalizado -> "Finalizado"
            pedido.estadoPedido.equals("Solicitado", ignoreCase = true) -> "Solicitado"
            pedido.estadoPedido.equals("Entregado", ignoreCase = true) -> "Finalizado"
            posicion == 0 -> "Pedido activo"
            else -> "En proceso"
        }
    }

    // Evita mostrar campos vacíos en pedidos antiguos
    private fun obtenerTextoSeguro(valor: String): String {
        return if (valor.isBlank()) {
            "No especificado"
        } else {
            valor
        }
    }
}
package sv.edu.ues.entregatrack

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.bumptech.glide.Glide

class SolicitudesRepartidorActivity : ComponentActivity() {

    private lateinit var layoutSolicitudesDisponibles: LinearLayout

    // Pantalla donde el repartidor ve solicitudes creadas por clientes
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        setContentView(R.layout.activity_solicitudes_repartidor)

        layoutSolicitudesDisponibles = findViewById(R.id.layoutSolicitudesDisponibles)

        val btnRefrescarSolicitudes = findViewById<Button>(R.id.btnRefrescarSolicitudes)
        val btnVolverSolicitudes = findViewById<Button>(R.id.btnVolverSolicitudes)

        btnRefrescarSolicitudes.setOnClickListener {
            cargarSolicitudes()
        }

        btnVolverSolicitudes.setOnClickListener {
            finish()
        }

        cargarSolicitudes()
    }

    // Consulta solicitudes disponibles en Firebase
    private fun cargarSolicitudes() {
        FirebasePedidoHelper.cargarSolicitudesDisponibles(
            onSuccess = { solicitudes ->
                mostrarSolicitudes(solicitudes)
            },
            onError = { mensaje ->
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            }
        )
    }

    // Dibuja las tarjetas de solicitudes
    private fun mostrarSolicitudes(solicitudes: List<PedidoFirebase>) {
        layoutSolicitudesDisponibles.removeAllViews()

        if (solicitudes.isEmpty()) {
            val txtVacio = TextView(this)
            txtVacio.text = "No hay solicitudes disponibles por el momento"
            txtVacio.textSize = 17f
            txtVacio.setTextColor(Color.parseColor("#333333"))
            txtVacio.setTypeface(null, android.graphics.Typeface.BOLD)
            txtVacio.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            txtVacio.setPadding(12, 20, 12, 20)

            layoutSolicitudesDisponibles.addView(txtVacio)
            return
        }

        solicitudes.forEach { pedido ->
            val tarjeta = crearTarjetaSolicitud(pedido)
            layoutSolicitudesDisponibles.addView(tarjeta)
        }
    }

    // Crea tarjeta visual con datos completos del pedido
    private fun crearTarjetaSolicitud(pedido: PedidoFirebase): LinearLayout {
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

        val txtCodigo = crearTexto(
            "Pedido: ${pedido.codigoPedido}",
            20f,
            "#D81B60",
            true
        )

        val txtServicio = crearTexto(
            "Servicio: ${textoSeguro(pedido.tipoServicio)}",
            16f,
            "#333333",
            true
        )

        val txtCliente = crearTexto(
            "Cliente: ${textoSeguro(pedido.clienteNombre)}",
            16f,
            "#333333",
            false
        )

        val txtRecogida = crearTexto(
            "Recoger en: ${textoSeguro(pedido.direccionRecogida)}",
            16f,
            "#333333",
            false
        )

        val txtEntrega = crearTexto(
            "Entregar en: ${textoSeguro(pedido.direccionEntrega)}",
            16f,
            "#333333",
            false
        )

        val txtMandado = crearTexto(
            "Mandado: ${textoSeguro(pedido.descripcionPedido)}",
            16f,
            "#333333",
            false
        )

        val txtIndicaciones = crearTexto(
            "Indicaciones: ${textoSeguro(pedido.indicacionesRepartidor)}",
            16f,
            "#333333",
            false
        )

        val txtPrecio = crearTexto(
            "Distancia: ${String.format("%.2f", pedido.distanciaKm)} km\nTotal estimado: $${String.format("%.2f", pedido.precioEstimado)}",
            17f,
            "#D81B60",
            true
        )

        tarjeta.addView(txtCodigo)
        tarjeta.addView(txtServicio)
        tarjeta.addView(txtCliente)
        tarjeta.addView(txtRecogida)
        tarjeta.addView(txtEntrega)
        tarjeta.addView(txtMandado)
        tarjeta.addView(txtIndicaciones)
        tarjeta.addView(txtPrecio)

        // Muestra QR o anexo del cliente si existe
        if (pedido.anexoClienteRegistrado && pedido.urlAnexoCliente.isNotBlank()) {
            val txtAnexo = crearTexto(
                "Anexo del cliente: ${pedido.tipoAnexoCliente}",
                16f,
                "#4A148C",
                true
            )

            val imgAnexo = ImageView(this)
            imgAnexo.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                420
            )
            imgAnexo.scaleType = ImageView.ScaleType.CENTER_CROP
            imgAnexo.setBackgroundResource(R.drawable.bg_input_accesible)
            imgAnexo.setPadding(6, 6, 6, 6)

            Glide.with(this)
                .load(pedido.urlAnexoCliente)
                .into(imgAnexo)

            tarjeta.addView(txtAnexo)
            tarjeta.addView(imgAnexo)
        }

        val btnAceptar = Button(this)
        btnAceptar.text = "Aceptar solicitud"
        btnAceptar.setAllCaps(false)
        btnAceptar.textSize = 16f
        btnAceptar.setTextColor(Color.WHITE)
        btnAceptar.setTypeface(null, android.graphics.Typeface.BOLD)
        btnAceptar.setBackgroundResource(R.drawable.bg_button_pink)

        val paramsBoton = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            56.dp()
        )
        paramsBoton.setMargins(0, 18, 0, 0)
        btnAceptar.layoutParams = paramsBoton

        btnAceptar.setOnClickListener {
            aceptarSolicitud(pedido)
        }

        tarjeta.addView(btnAceptar)

        return tarjeta
    }

    // Acepta la solicitud seleccionada
    private fun aceptarSolicitud(pedido: PedidoFirebase) {
        FirebasePedidoHelper.aceptarSolicitudRepartidor(
            pedido = pedido,
            onSuccess = {
                Toast.makeText(this, "Solicitud aceptada correctamente", Toast.LENGTH_SHORT).show()

                // Actualiza datos temporales y abre panel del repartidor
                val pedidoActualizado = pedido.copy(
                    estadoPedido = "Aceptado por repartidor"
                )

                FirebasePedidoHelper.aplicarPedidoADatosEntrega(pedidoActualizado)

                val intent = Intent(this, RepartidorActivity::class.java)

                // Enviamos el código real del pedido aceptado
                intent.putExtra("codigoPedido", pedido.codigoPedido)

                startActivity(intent)

                finish()
            },
            onError = { mensaje ->
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            }
        )
    }

    // Crea TextView uniforme y legible
    private fun crearTexto(
        texto: String,
        size: Float,
        color: String,
        negrita: Boolean
    ): TextView {
        val txt = TextView(this)
        txt.text = texto
        txt.textSize = size
        txt.setTextColor(Color.parseColor(color))
        txt.setPadding(0, 6, 0, 6)

        if (negrita) {
            txt.setTypeface(null, android.graphics.Typeface.BOLD)
        }

        return txt
    }

    // Evita campos vacíos en pantalla
    private fun textoSeguro(valor: String): String {
        return if (valor.isBlank()) "No especificado" else valor
    }

    // Conversión simple de dp a px
    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
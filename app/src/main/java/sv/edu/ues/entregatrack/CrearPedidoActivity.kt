package sv.edu.ues.entregatrack

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import java.util.Locale

class CrearPedidoActivity : ComponentActivity() {

    private lateinit var spinnerTipoServicio: Spinner
    private lateinit var edtDireccionRecogida: EditText
    private lateinit var edtDireccionEntrega: EditText
    private lateinit var edtTelefonoPedido: EditText
    private lateinit var edtReferenciaPedido: EditText
    private lateinit var edtDescripcionPedido: EditText
    private lateinit var edtIndicacionesRepartidor: EditText
    private lateinit var edtDistanciaKm: EditText

    private lateinit var txtPrecioEstimado: TextView
    private lateinit var txtUbicacionActualCliente: TextView
    private lateinit var txtDistanciaCalculada: TextView
    private lateinit var txtEstadoAnexoCliente: TextView
    private lateinit var imgAnexoCliente: ImageView

    private var precioCalculado: Double = 0.0

    // Ubicación actual del cliente
    private var latitudCliente: Double = 0.0
    private var longitudCliente: Double = 0.0
    private var ubicacionActualCliente: String = ""

    // Coordenadas seleccionadas en el mapa
    private var latitudRecogida: Double = 0.0
    private var longitudRecogida: Double = 0.0
    private var latitudEntrega: Double = 0.0
    private var longitudEntrega: Double = 0.0

    // Datos del anexo del cliente
    private var uriAnexoCliente: Uri? = null
    private var bitmapAnexoCliente: Bitmap? = null
    private var tipoAnexoCliente: String = ""

    private val codigoPermisoUbicacion = 200
    private val codigoPermisoCamara = 201

    // Recibe la ubicación seleccionada desde SeleccionarUbicacionActivity
    private val seleccionarUbicacionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
            if (resultado.resultCode == RESULT_OK) {
                val data = resultado.data ?: return@registerForActivityResult

                val tipoUbicacion = data.getStringExtra("tipoUbicacion") ?: ""
                val direccion = data.getStringExtra("direccionSeleccionada") ?: ""
                val latitud = data.getDoubleExtra("latitudSeleccionada", 0.0)
                val longitud = data.getDoubleExtra("longitudSeleccionada", 0.0)

                if (tipoUbicacion == "recogida") {
                    edtDireccionRecogida.setText(direccion)
                    latitudRecogida = latitud
                    longitudRecogida = longitud
                } else {
                    edtDireccionEntrega.setText(direccion)
                    latitudEntrega = latitud
                    longitudEntrega = longitud
                }

                calcularDistanciaAutomatica()
            }
        }

    // Selecciona imagen o QR desde galería
    private val seleccionarAnexoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                uriAnexoCliente = uri
                bitmapAnexoCliente = null
                tipoAnexoCliente = "Imagen seleccionada"

                imgAnexoCliente.setImageURI(uri)
                txtEstadoAnexoCliente.text = "Anexo del pedido: imagen seleccionada"
            }
        }

    // Toma foto del anexo usando cámara
    private val tomarFotoAnexoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                bitmapAnexoCliente = bitmap
                uriAnexoCliente = null
                tipoAnexoCliente = "Foto tomada"

                imgAnexoCliente.setImageBitmap(bitmap)
                txtEstadoAnexoCliente.text = "Anexo del pedido: foto tomada"
            }
        }

    // Pantalla para que el cliente solicite un mandado o entrega
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        setContentView(R.layout.activity_crear_pedido)

        spinnerTipoServicio = findViewById(R.id.spinnerTipoServicio)
        edtDireccionRecogida = findViewById(R.id.edtDireccionRecogida)
        edtDireccionEntrega = findViewById(R.id.edtDireccionEntrega)
        edtTelefonoPedido = findViewById(R.id.edtTelefonoPedido)
        edtReferenciaPedido = findViewById(R.id.edtReferenciaPedido)
        edtDescripcionPedido = findViewById(R.id.edtDescripcionPedido)
        edtIndicacionesRepartidor = findViewById(R.id.edtIndicacionesRepartidor)
        edtDistanciaKm = findViewById(R.id.edtDistanciaKm)

        txtPrecioEstimado = findViewById(R.id.txtPrecioEstimado)
        txtUbicacionActualCliente = findViewById(R.id.txtUbicacionActualCliente)
        txtDistanciaCalculada = findViewById(R.id.txtDistanciaCalculada)
        txtEstadoAnexoCliente = findViewById(R.id.txtEstadoAnexoCliente)
        imgAnexoCliente = findViewById(R.id.imgAnexoCliente)

        val btnSeleccionarRecogidaMapa = findViewById<Button>(R.id.btnSeleccionarRecogidaMapa)
        val btnSeleccionarEntregaMapa = findViewById<Button>(R.id.btnSeleccionarEntregaMapa)
        val btnUsarUbicacionActual = findViewById<Button>(R.id.btnUsarUbicacionActual)
        val btnCalcularPrecio = findViewById<Button>(R.id.btnCalcularPrecio)
        val btnGuardarPedido = findViewById<Button>(R.id.btnGuardarPedido)
        val btnVolverCrearPedido = findViewById<Button>(R.id.btnVolverCrearPedido)
        val btnSeleccionarAnexoCliente = findViewById<Button>(R.id.btnSeleccionarAnexoCliente)
        val btnTomarFotoAnexoCliente = findViewById<Button>(R.id.btnTomarFotoAnexoCliente)

        configurarSpinnerServicios()
        aplicarFormatoTelefono()

        btnSeleccionarRecogidaMapa.setOnClickListener {
            abrirSelectorUbicacion(
                tipo = "recogida",
                direccionInicial = edtDireccionRecogida.text.toString().trim()
            )
        }

        btnSeleccionarEntregaMapa.setOnClickListener {
            abrirSelectorUbicacion(
                tipo = "entrega",
                direccionInicial = edtDireccionEntrega.text.toString().trim()
            )
        }

        btnUsarUbicacionActual.setOnClickListener {
            obtenerUbicacionActual()
        }

        btnCalcularPrecio.setOnClickListener {
            calcularPrecioEstimado()
        }

        btnSeleccionarAnexoCliente.setOnClickListener {
            seleccionarAnexoLauncher.launch("image/*")
        }

        btnTomarFotoAnexoCliente.setOnClickListener {
            solicitarCamaraParaAnexo()
        }

        btnGuardarPedido.setOnClickListener {
            guardarPedido()
        }

        btnVolverCrearPedido.setOnClickListener {
            finish()
        }
    }

    // Carga tipos de servicio reales para la app
    private fun configurarSpinnerServicios() {
        val servicios = listOf(
            "Retiro de paquete",
            "Compra en tienda",
            "Farmacia / medicamentos",
            "Entrega de documentos",
            "Pago de recibos",
            "Encomienda express",
            "Otro mandado"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            servicios
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipoServicio.adapter = adapter
    }

    // Abre la pantalla de selección de ubicación dentro de la app
    private fun abrirSelectorUbicacion(tipo: String, direccionInicial: String) {
        val intent = Intent(this, SeleccionarUbicacionActivity::class.java)
        intent.putExtra("tipoUbicacion", tipo)
        intent.putExtra("direccionInicial", direccionInicial)

        seleccionarUbicacionLauncher.launch(intent)
    }

    // Obtiene la ubicación actual del cliente
    private fun obtenerUbicacionActual() {
        val permiso = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (permiso != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                codigoPermisoUbicacion
            )
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location == null) {
                    Toast.makeText(
                        this,
                        "No se pudo obtener la ubicación. Activa el GPS.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                latitudCliente = location.latitude
                longitudCliente = location.longitude
                ubicacionActualCliente = obtenerDireccionDesdeCoordenadas(
                    latitudCliente,
                    longitudCliente
                )

                txtUbicacionActualCliente.text =
                    "Ubicación actual: $ubicacionActualCliente\nLat: $latitudCliente, Lng: $longitudCliente"

                edtDireccionEntrega.setText(ubicacionActualCliente)
                latitudEntrega = latitudCliente
                longitudEntrega = longitudCliente

                calcularDistanciaAutomatica()

                Toast.makeText(this, "Ubicación capturada correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener ubicación", Toast.LENGTH_LONG).show()
            }
    }

    // Convierte coordenadas a una dirección legible
    private fun obtenerDireccionDesdeCoordenadas(latitud: Double, longitud: Double): String {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val direcciones = geocoder.getFromLocation(latitud, longitud, 1)

            if (!direcciones.isNullOrEmpty()) {
                direcciones[0].getAddressLine(0) ?: "Ubicación capturada"
            } else {
                "Ubicación capturada"
            }
        } catch (e: Exception) {
            "Ubicación capturada"
        }
    }

    // Da formato automático al teléfono: 7696-3351
    private fun aplicarFormatoTelefono() {
        var editando = false

        edtTelefonoPedido.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {}

            override fun afterTextChanged(s: Editable?) {
                if (editando) return

                editando = true

                val soloNumeros = s.toString()
                    .replace("-", "")
                    .filter { it.isDigit() }
                    .take(8)

                val telefonoFormateado = if (soloNumeros.length > 4) {
                    soloNumeros.substring(0, 4) + "-" + soloNumeros.substring(4)
                } else {
                    soloNumeros
                }

                edtTelefonoPedido.setText(telefonoFormateado)
                edtTelefonoPedido.setSelection(telefonoFormateado.length)

                editando = false
            }
        })
    }

    // Calcula la distancia aproximada entre recogida y entrega
    private fun calcularDistanciaAutomatica() {
        if (
            latitudRecogida == 0.0 || longitudRecogida == 0.0 ||
            latitudEntrega == 0.0 || longitudEntrega == 0.0
        ) {
            return
        }

        val resultado = FloatArray(1)

        Location.distanceBetween(
            latitudRecogida,
            longitudRecogida,
            latitudEntrega,
            longitudEntrega,
            resultado
        )

        val distanciaKm = resultado[0] / 1000.0
        val distanciaFormateada = String.format(Locale.US, "%.2f", distanciaKm)

        edtDistanciaKm.setText(distanciaFormateada)

        txtDistanciaCalculada.text =
            "Distancia estimada: $distanciaFormateada kilómetros"

        calcularPrecioEstimado()
    }

    // Calcula precio con una fórmula simple
    private fun calcularPrecioEstimado(): Double {
        val distanciaTexto = edtDistanciaKm.text.toString().trim()

        if (distanciaTexto.isEmpty()) {
            Toast.makeText(this, "Ingresa la distancia estimada en km", Toast.LENGTH_SHORT).show()
            return 0.0
        }

        val distanciaKm = distanciaTexto.toDoubleOrNull()

        if (distanciaKm == null || distanciaKm <= 0.0) {
            Toast.makeText(this, "Distancia inválida", Toast.LENGTH_SHORT).show()
            return 0.0
        }

        val tipoServicio = spinnerTipoServicio.selectedItem.toString()

        val tarifaBase = 2.00
        val costoPorKm = 0.60
        val recargoServicio = obtenerRecargoServicio(tipoServicio)

        precioCalculado = tarifaBase + (distanciaKm * costoPorKm) + recargoServicio

        val formato = DecimalFormat("#0.00")
        txtPrecioEstimado.text =
            "Total estimado del servicio: $${formato.format(precioCalculado)}"

        return precioCalculado
    }

    // Recargo básico según el tipo de mandado
    private fun obtenerRecargoServicio(tipoServicio: String): Double {
        return when (tipoServicio) {
            "Compra en tienda" -> 1.00
            "Farmacia / medicamentos" -> 0.75
            "Entrega de documentos" -> 0.50
            "Pago de recibos" -> 0.75
            "Encomienda express" -> 0.50
            else -> 0.0
        }
    }

    // Solicita permiso de cámara para tomar foto del QR, receta o anexo
    private fun solicitarCamaraParaAnexo() {
        val permiso = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        )

        if (permiso != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                codigoPermisoCamara
            )
            return
        }

        tomarFotoAnexoLauncher.launch(null)
    }

    // Sube el anexo del cliente a Firebase Storage
    private fun subirAnexoCliente(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val uidCliente = FirebaseAuth.getInstance().currentUser?.uid

        if (uidCliente.isNullOrEmpty()) {
            onError("No hay usuario autenticado")
            return
        }

        val nombreArchivo = "anexos_pedidos/$uidCliente/anexo_${System.currentTimeMillis()}.jpg"
        val referenciaStorage = FirebaseStorage.getInstance().reference.child(nombreArchivo)

        if (uriAnexoCliente != null) {
            referenciaStorage.putFile(uriAnexoCliente!!)
                .addOnSuccessListener {
                    referenciaStorage.downloadUrl
                        .addOnSuccessListener { url ->
                            onSuccess(url.toString())
                        }
                        .addOnFailureListener { error ->
                            onError(error.message ?: "Error al obtener URL del anexo")
                        }
                }
                .addOnFailureListener { error ->
                    onError(error.message ?: "Error al subir anexo")
                }

            return
        }

        if (bitmapAnexoCliente != null) {
            val baos = ByteArrayOutputStream()
            bitmapAnexoCliente!!.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val datosImagen = baos.toByteArray()

            referenciaStorage.putBytes(datosImagen)
                .addOnSuccessListener {
                    referenciaStorage.downloadUrl
                        .addOnSuccessListener { url ->
                            onSuccess(url.toString())
                        }
                        .addOnFailureListener { error ->
                            onError(error.message ?: "Error al obtener URL del anexo")
                        }
                }
                .addOnFailureListener { error ->
                    onError(error.message ?: "Error al subir foto")
                }

            return
        }

        onSuccess("")
    }

    // Valida y guarda el pedido en Firebase
    private fun guardarPedido() {
        val tipoServicio = spinnerTipoServicio.selectedItem.toString()
        val direccionRecogida = edtDireccionRecogida.text.toString().trim()
        val direccionEntrega = edtDireccionEntrega.text.toString().trim()
        val telefono = edtTelefonoPedido.text.toString().trim()
        val referencia = edtReferenciaPedido.text.toString().trim()
        val descripcion = edtDescripcionPedido.text.toString().trim()
        val indicaciones = edtIndicacionesRepartidor.text.toString().trim()
        val distanciaKm = edtDistanciaKm.text.toString().trim().toDoubleOrNull() ?: 0.0

        if (telefono.length != 9) {
            Toast.makeText(this, "El teléfono debe tener formato 7696-3351", Toast.LENGTH_LONG).show()
            return
        }

        if (latitudRecogida == 0.0 || longitudRecogida == 0.0) {
            Toast.makeText(this, "Selecciona el lugar de recogida en el mapa", Toast.LENGTH_LONG).show()
            return
        }

        if (latitudEntrega == 0.0 || longitudEntrega == 0.0) {
            Toast.makeText(this, "Selecciona el lugar de entrega en el mapa", Toast.LENGTH_LONG).show()
            return
        }

        val precioFinal = calcularPrecioEstimado()

        if (precioFinal <= 0.0) {
            return
        }

        subirAnexoCliente(
            onSuccess = { urlAnexo ->
                val tieneAnexo = urlAnexo.isNotBlank()

                FirebasePedidoHelper.crearPedidoCliente(
                    tipoServicio = tipoServicio,
                    direccionRecogida = direccionRecogida,
                    direccionEntrega = direccionEntrega,
                    telefonoCliente = telefono,
                    referenciaUbicacion = referencia,
                    descripcionPedido = descripcion,
                    indicacionesRepartidor = indicaciones,

                    anexoClienteRegistrado = tieneAnexo,
                    urlAnexoCliente = urlAnexo,
                    tipoAnexoCliente = if (tieneAnexo) tipoAnexoCliente else "",

                    distanciaKm = distanciaKm,
                    precioEstimado = precioFinal,

                    ubicacionActualCliente = ubicacionActualCliente,
                    latitudCliente = latitudCliente,
                    longitudCliente = longitudCliente,

                    latitudRecogida = latitudRecogida,
                    longitudRecogida = longitudRecogida,
                    latitudEntrega = latitudEntrega,
                    longitudEntrega = longitudEntrega,

                    onSuccess = {
                        Toast.makeText(this, "Solicitud creada correctamente", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this, HistorialPedidosActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(intent)

                        finish()
                    },
                    onError = { mensaje ->
                        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
                    }
                )
            },
            onError = { mensaje ->
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            }
        )
    }

    // Maneja permisos de ubicación y cámara
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (
            requestCode == codigoPermisoUbicacion &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            obtenerUbicacionActual()
            return
        }

        if (
            requestCode == codigoPermisoCamara &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            tomarFotoAnexoLauncher.launch(null)
            return
        }

        Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
    }
}
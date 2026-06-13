package sv.edu.ues.entregatrack

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

// Pantalla que permite registrar evidencias de recogida o entrega
class EvidenciaActivity : ComponentActivity() {

    // Controles visuales de la pantalla
    private lateinit var imgEvidencia: ImageView
    private lateinit var txtEstadoEvidencia: TextView
    private lateinit var txtRutaFoto: TextView
    private lateinit var btnTomarFoto: Button
    private lateinit var btnGuardarEvidencia: Button
    private lateinit var btnVolverEvidencia: Button

    // Imagen capturada temporalmente
    private var bitmapEvidencia: Bitmap? = null

    // Código del pedido seleccionado
    private var codigoPedidoActual: String = ""

    // Define si la evidencia es de recogida o entrega
    private var tipoEvidencia: String = "entrega"

    // Abre la aplicación de cámara y recibe una vista previa
    private val tomarFotoLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicturePreview()
        ) { bitmap ->

            // Comprueba si se obtuvo una fotografía
            if (bitmap != null) {

                // Guarda temporalmente la fotografía
                bitmapEvidencia = bitmap

                // Muestra la fotografía en pantalla
                imgEvidencia.setImageBitmap(bitmap)

                // Cambia el mensaje según el tipo de evidencia
                txtEstadoEvidencia.text =
                    if (tipoEvidencia == "recogida") {
                        "Evidencia de recogida capturada"
                    } else {
                        "Evidencia de entrega capturada"
                    }

                // Informa que la foto está lista para guardarse
                txtRutaFoto.text =
                    "Foto lista para guardar en Firebase Storage"

            } else {

                // Informa cuando el usuario cancela la fotografía
                Toast.makeText(
                    this,
                    "No se capturó ninguna foto",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // Solicita el permiso de cámara durante la ejecución
    private val solicitarPermisoCamaraLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { permisoConcedido ->

            if (permisoConcedido) {

                // Abre la cámara después de recibir autorización
                tomarFotoLauncher.launch(null)

            } else {

                // Informa que la cámara necesita autorización
                Toast.makeText(
                    this,
                    "Debes permitir el acceso a la cámara para registrar la evidencia",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    // Configura la pantalla cuando se abre
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Define los colores principales de la aplicación
        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        // Carga el diseño XML
        setContentView(R.layout.activity_evidencia)

        // Relaciona los controles XML con Kotlin
        imgEvidencia = findViewById(R.id.imgEvidencia)
        txtEstadoEvidencia = findViewById(R.id.txtEstadoEvidencia)
        txtRutaFoto = findViewById(R.id.txtRutaFoto)
        btnTomarFoto = findViewById(R.id.btnTomarFoto)
        btnGuardarEvidencia = findViewById(R.id.btnGuardarEvidencia)
        btnVolverEvidencia = findViewById(R.id.btnVolverEvidencia)

        // Recibe el código del pedido desde la pantalla anterior
        codigoPedidoActual =
            intent.getStringExtra("codigoPedido")
                ?: DatosEntrega.codigoPedido

        // Recibe el tipo de evidencia
        tipoEvidencia =
            intent.getStringExtra("tipoEvidencia")
                ?: "entrega"

        // Configura los textos de la pantalla
        configurarPantalla()

        // Comprueba el permiso antes de abrir la cámara
        btnTomarFoto.setOnClickListener {
            abrirCamaraConPermiso()
        }

        // Guarda la evidencia en Firebase
        btnGuardarEvidencia.setOnClickListener {
            guardarEvidencia()
        }

        // Regresa a la pantalla anterior
        btnVolverEvidencia.setOnClickListener {
            finish()
        }
    }

    // Comprueba si la aplicación tiene permiso para usar la cámara
    private fun abrirCamaraConPermiso() {

        // Consulta el estado actual del permiso
        val permisoCamaraConcedido =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        if (permisoCamaraConcedido) {

            // Abre la cámara si el permiso ya fue concedido
            tomarFotoLauncher.launch(null)

        } else {

            // Solicita el permiso si todavía no fue concedido
            solicitarPermisoCamaraLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    // Ajusta los textos según el tipo de evidencia
    private fun configurarPantalla() {

        if (tipoEvidencia == "recogida") {

            // Configuración para evidencia de recogida
            txtEstadoEvidencia.text =
                "Registra evidencia de recogida del paquete"

            txtRutaFoto.text =
                "Toma una foto que confirme que el paquete fue recibido"

            btnGuardarEvidencia.text =
                "Guardar evidencia de recogida"

        } else {

            // Configuración para evidencia de entrega
            txtEstadoEvidencia.text =
                "Registra evidencia de entrega del paquete"

            txtRutaFoto.text =
                "Toma una foto que confirme la entrega del paquete"

            btnGuardarEvidencia.text =
                "Guardar evidencia de entrega"
        }
    }

    // Valida la fotografía antes de subirla
    private fun guardarEvidencia() {

        // Comprueba que exista un pedido seleccionado
        if (codigoPedidoActual.isBlank()) {
            Toast.makeText(
                this,
                "No hay pedido seleccionado",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Obtiene la fotografía capturada
        val bitmap = bitmapEvidencia

        // Evita guardar sin tomar una fotografía
        if (bitmap == null) {
            Toast.makeText(
                this,
                "Primero toma una foto de evidencia",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Informa que inició el proceso
        txtEstadoEvidencia.text =
            "Subiendo evidencia..."

        // Evita presionar varias veces mientras se guarda
        btnGuardarEvidencia.isEnabled = false
        btnTomarFoto.isEnabled = false

        // Envía la fotografía a Firebase Storage
        subirEvidenciaStorage(
            bitmap = bitmap,

            onSuccess = { urlEvidencia ->
                guardarUrlEvidenciaPedido(urlEvidencia)
            },

            onError = { mensaje ->

                // Habilita nuevamente los botones
                btnGuardarEvidencia.isEnabled = true
                btnTomarFoto.isEnabled = true

                txtEstadoEvidencia.text =
                    "Error al subir evidencia"

                Toast.makeText(
                    this,
                    mensaje,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    // Sube la fotografía a Firebase Storage
    private fun subirEvidenciaStorage(
        bitmap: Bitmap,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        // Obtiene el identificador del usuario autenticado
        val uid =
            FirebaseAuth.getInstance()
                .currentUser
                ?.uid

        // Valida que exista una sesión activa
        if (uid.isNullOrBlank()) {
            onError("No hay usuario autenticado")
            return
        }

        // Obtiene la referencia principal de Firebase Storage
        val storageRef =
            FirebaseStorage.getInstance().reference

        // Crea un nombre diferente según el tipo de evidencia
        val nombreArchivo =
            if (tipoEvidencia == "recogida") {
                "evidencia_recogida_${System.currentTimeMillis()}.jpg"
            } else {
                "evidencia_entrega_${System.currentTimeMillis()}.jpg"
            }

        // Define la carpeta donde se guardará la fotografía
        val rutaStorage = storageRef.child(
            "evidencias_pedidos/$codigoPedidoActual/$uid/$nombreArchivo"
        )

        // Convierte el Bitmap a un arreglo de bytes
        val flujoBytes = ByteArrayOutputStream()

        // Comprime la fotografía en formato JPEG
        bitmap.compress(
            Bitmap.CompressFormat.JPEG,
            85,
            flujoBytes
        )

        // Obtiene los bytes que se subirán
        val datosFoto = flujoBytes.toByteArray()

        // Libera el flujo utilizado
        flujoBytes.close()

        // Sube la fotografía a Firebase Storage
        rutaStorage.putBytes(datosFoto)
            .addOnSuccessListener {

                // Obtiene la URL pública de descarga
                rutaStorage.downloadUrl
                    .addOnSuccessListener { uri ->
                        onSuccess(uri.toString())
                    }
                    .addOnFailureListener { error ->
                        onError(
                            error.message
                                ?: "No se pudo obtener la URL de evidencia"
                        )
                    }
            }
            .addOnFailureListener { error ->
                onError(
                    error.message
                        ?: "No se pudo subir la evidencia"
                )
            }
    }

    // Guarda la URL de la evidencia en Realtime Database
    private fun guardarUrlEvidenciaPedido(
        urlEvidencia: String
    ) {

        FirebasePedidoHelper.guardarEvidenciaPedido(
            codigoPedido = codigoPedidoActual,
            tipoEvidencia = tipoEvidencia,
            urlEvidencia = urlEvidencia,

            onSuccess = {

                if (tipoEvidencia == "recogida") {

                    // Actualiza los datos temporales de recogida
                    DatosEntrega.estadoPedido =
                        "Paquete recogido"

                    DatosEntrega.ultimaActualizacion =
                        "El repartidor ya recibió el paquete y registró evidencia"

                    txtEstadoEvidencia.text =
                        "Evidencia de recogida guardada correctamente"

                } else {

                    // Actualiza los datos temporales de entrega
                    DatosEntrega.estadoPedido =
                        "Entregado"

                    DatosEntrega.ultimaActualizacion =
                        "El repartidor entregó el paquete y registró evidencia"

                    DatosEntrega.evidenciaRegistrada =
                        true

                    DatosEntrega.rutaFotoEvidencia =
                        urlEvidencia

                    txtEstadoEvidencia.text =
                        "Evidencia de entrega guardada correctamente"
                }

                // Muestra la URL almacenada
                txtRutaFoto.text = urlEvidencia

                // Mantiene bloqueado el botón para evitar duplicados
                btnGuardarEvidencia.isEnabled = false

                // Permite tomar otra foto solamente si fuera necesario
                btnTomarFoto.isEnabled = true

                // Informa que el proceso terminó correctamente
                Toast.makeText(
                    this,
                    "Evidencia guardada correctamente",
                    Toast.LENGTH_SHORT
                ).show()
            },

            onError = { mensaje ->

                // Habilita nuevamente los botones
                btnGuardarEvidencia.isEnabled = true
                btnTomarFoto.isEnabled = true

                txtEstadoEvidencia.text =
                    "Error al guardar evidencia"

                Toast.makeText(
                    this,
                    mensaje,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
}

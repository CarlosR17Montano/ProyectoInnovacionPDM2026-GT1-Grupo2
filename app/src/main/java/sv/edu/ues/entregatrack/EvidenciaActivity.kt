
package sv.edu.ues.entregatrack

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.io.File

// Registra evidencia de recogida o entrega con CameraX
class EvidenciaActivity : ComponentActivity() {

    // Controles de la pantalla
    private lateinit var previewCamara: PreviewView
    private lateinit var imgEvidencia: ImageView
    private lateinit var txtEstadoEvidencia: TextView
    private lateinit var txtRutaFoto: TextView
    private lateinit var btnTomarFoto: Button
    private lateinit var btnGuardarEvidencia: Button
    private lateinit var btnVolverEvidencia: Button

    // Componentes de CameraX
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null

    // Archivo temporal de la fotografía
    private var archivoEvidencia: File? = null

    // Datos del pedido
    private var codigoPedidoActual: String = ""
    private var tipoEvidencia: String = "entrega"

    // Solicita el permiso de cámara
    private val solicitarPermisoCamara =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { permisoConcedido ->

            if (permisoConcedido) {
                iniciarCamara()
            } else {
                Toast.makeText(
                    this,
                    "Debes permitir el uso de la cámara",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        setContentView(R.layout.activity_evidencia)

        // Relaciona los controles del XML
        previewCamara = findViewById(R.id.previewCamara)
        imgEvidencia = findViewById(R.id.imgEvidencia)
        txtEstadoEvidencia = findViewById(R.id.txtEstadoEvidencia)
        txtRutaFoto = findViewById(R.id.txtRutaFoto)
        btnTomarFoto = findViewById(R.id.btnTomarFoto)
        btnGuardarEvidencia = findViewById(R.id.btnGuardarEvidencia)
        btnVolverEvidencia = findViewById(R.id.btnVolverEvidencia)

        // Recibe los datos del pedido
        codigoPedidoActual =
            intent.getStringExtra("codigoPedido")
                ?: DatosEntrega.codigoPedido

        tipoEvidencia =
            intent.getStringExtra("tipoEvidencia")
                ?: "entrega"

        configurarPantalla()

        // Deshabilita guardar hasta tomar una foto
        btnGuardarEvidencia.isEnabled = false

        // Captura o repite la fotografía
        btnTomarFoto.setOnClickListener {
            if (archivoEvidencia == null) {
                tomarFotografia()
            } else {
                prepararNuevaFotografia()
            }
        }

        // Guarda la evidencia en Firebase
        btnGuardarEvidencia.setOnClickListener {
            guardarEvidencia()
        }

        // Regresa a la pantalla anterior
        btnVolverEvidencia.setOnClickListener {
            finish()
        }

        comprobarPermisoCamara()
    }

    // Configura los textos según la evidencia
    private fun configurarPantalla() {
        if (tipoEvidencia == "recogida") {
            txtEstadoEvidencia.text =
                "Registra evidencia de recogida del paquete"

            txtRutaFoto.text =
                "Toma una foto cuando recibas el paquete"

            btnGuardarEvidencia.text =
                "Guardar evidencia de recogida"
        } else {
            txtEstadoEvidencia.text =
                "Registra evidencia de entrega del paquete"

            txtRutaFoto.text =
                "Toma una foto cuando entregues el paquete"

            btnGuardarEvidencia.text =
                "Guardar evidencia de entrega"
        }
    }

    // Comprueba el permiso de cámara
    private fun comprobarPermisoCamara() {
        val permisoConcedido =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        if (permisoConcedido) {
            iniciarCamara()
        } else {
            solicitarPermisoCamara.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    // Inicia la vista previa de CameraX
    private fun iniciarCamara() {
        val futuroProveedor =
            ProcessCameraProvider.getInstance(this)

        futuroProveedor.addListener({

            try {
                cameraProvider =
                    futuroProveedor.get()

                val preview =
                    Preview.Builder()
                        .build()

                preview.setSurfaceProvider(
                    previewCamara.surfaceProvider
                )

                imageCapture =
                    ImageCapture.Builder()
                        .setCaptureMode(
                            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                        )
                        .build()

                cameraProvider?.unbindAll()

                cameraProvider?.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )

                previewCamara.visibility = View.VISIBLE
                imgEvidencia.visibility = View.GONE

                txtRutaFoto.text =
                    "Cámara lista para tomar la fotografía"

            } catch (error: Exception) {
                Toast.makeText(
                    this,
                    "No se pudo iniciar la cámara: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // Captura la fotografía en un archivo
    private fun tomarFotografia() {
        val captura =
            imageCapture

        if (captura == null) {
            Toast.makeText(
                this,
                "La cámara todavía no está lista",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val nombreArchivo =
            if (tipoEvidencia == "recogida") {
                "recogida_${System.currentTimeMillis()}.jpg"
            } else {
                "entrega_${System.currentTimeMillis()}.jpg"
            }

        val archivo =
            File(
                cacheDir,
                nombreArchivo
            )

        val opcionesSalida =
            ImageCapture.OutputFileOptions.Builder(
                archivo
            ).build()

        btnTomarFoto.isEnabled = false
        txtEstadoEvidencia.text = "Tomando fotografía..."

        captura.takePicture(
            opcionesSalida,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    resultado: ImageCapture.OutputFileResults
                ) {
                    archivoEvidencia = archivo

                    previewCamara.visibility = View.GONE
                    imgEvidencia.visibility = View.VISIBLE

                    imgEvidencia.setImageURI(
                        Uri.fromFile(archivo)
                    )

                    btnTomarFoto.isEnabled = true
                    btnTomarFoto.text = "Tomar otra foto"
                    btnGuardarEvidencia.isEnabled = true

                    txtEstadoEvidencia.text =
                        if (tipoEvidencia == "recogida") {
                            "Evidencia de recogida capturada"
                        } else {
                            "Evidencia de entrega capturada"
                        }

                    txtRutaFoto.text =
                        "Foto lista para guardar"
                }

                override fun onError(
                    error: ImageCaptureException
                ) {
                    btnTomarFoto.isEnabled = true

                    txtEstadoEvidencia.text =
                        "No se pudo tomar la fotografía"

                    Toast.makeText(
                        this@EvidenciaActivity,
                        error.message ?: "Error de cámara",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    // Permite tomar otra fotografía
    private fun prepararNuevaFotografia() {
        archivoEvidencia?.let { archivo ->
            if (archivo.exists()) {
                archivo.delete()
            }
        }

        archivoEvidencia = null

        imgEvidencia.setImageDrawable(null)
        imgEvidencia.visibility = View.GONE
        previewCamara.visibility = View.VISIBLE

        btnTomarFoto.text = "Tomar foto"
        btnGuardarEvidencia.isEnabled = false

        txtEstadoEvidencia.text =
            "Cámara lista para una nueva fotografía"

        txtRutaFoto.text =
            "Presiona tomar foto"
    }

    // Valida y sube la fotografía
    private fun guardarEvidencia() {
        if (codigoPedidoActual.isBlank()) {
            Toast.makeText(
                this,
                "No hay pedido seleccionado",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val archivo =
            archivoEvidencia

        if (archivo == null || !archivo.exists()) {
            Toast.makeText(
                this,
                "Primero toma una fotografía",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val uid =
            FirebaseAuth.getInstance()
                .currentUser
                ?.uid

        if (uid.isNullOrBlank()) {
            Toast.makeText(
                this,
                "No hay usuario autenticado",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        btnGuardarEvidencia.isEnabled = false
        btnTomarFoto.isEnabled = false
        txtEstadoEvidencia.text = "Subiendo evidencia..."

        subirEvidenciaStorage(
            archivo = archivo,
            uid = uid
        )
    }

    // Sube el archivo a Firebase Storage
    private fun subirEvidenciaStorage(
        archivo: File,
        uid: String
    ) {
        val nombreArchivo =
            if (tipoEvidencia == "recogida") {
                "evidencia_recogida_${System.currentTimeMillis()}.jpg"
            } else {
                "evidencia_entrega_${System.currentTimeMillis()}.jpg"
            }

        val referenciaStorage =
            FirebaseStorage.getInstance()
                .reference
                .child(
                    "evidencias_pedidos/" +
                            "$codigoPedidoActual/" +
                            "$uid/" +
                            nombreArchivo
                )

        val uriArchivo =
            Uri.fromFile(archivo)

        referenciaStorage.putFile(uriArchivo)
            .addOnSuccessListener {

                referenciaStorage.downloadUrl
                    .addOnSuccessListener { uri ->
                        guardarUrlEvidenciaPedido(
                            uri.toString()
                        )
                    }
                    .addOnFailureListener { error ->
                        mostrarError(
                            error.message
                                ?: "No se pudo obtener la URL"
                        )
                    }
            }
            .addOnFailureListener { error ->
                mostrarError(
                    error.message
                        ?: "No se pudo subir la evidencia"
                )
            }
    }

    // Guarda la URL en Realtime Database
    private fun guardarUrlEvidenciaPedido(
        urlEvidencia: String
    ) {
        FirebasePedidoHelper.guardarEvidenciaPedido(
            codigoPedido = codigoPedidoActual,
            tipoEvidencia = tipoEvidencia,
            urlEvidencia = urlEvidencia,

            onSuccess = {
                if (tipoEvidencia == "recogida") {
                    DatosEntrega.estadoPedido =
                        "Paquete recogido"

                    DatosEntrega.ultimaActualizacion =
                        "El repartidor registró la recogida"

                    txtEstadoEvidencia.text =
                        "Evidencia de recogida guardada"
                } else {
                    DatosEntrega.estadoPedido =
                        "Entregado"

                    DatosEntrega.ultimaActualizacion =
                        "El repartidor registró la entrega"

                    DatosEntrega.evidenciaRegistrada = true
                    DatosEntrega.rutaFotoEvidencia = urlEvidencia

                    txtEstadoEvidencia.text =
                        "Evidencia de entrega guardada"
                }

                txtRutaFoto.text =
                    "Fotografía guardada en Firebase"

                btnGuardarEvidencia.isEnabled = false
                btnTomarFoto.isEnabled = false

                Toast.makeText(
                    this,
                    "Evidencia guardada correctamente",
                    Toast.LENGTH_SHORT
                ).show()
            },

            onError = { mensaje ->
                mostrarError(mensaje)
            }
        )
    }

    // Muestra errores de captura o subida
    private fun mostrarError(
        mensaje: String
    ) {
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

    override fun onDestroy() {
        super.onDestroy()

        // Libera la cámara
        cameraProvider?.unbindAll()

        // Elimina el archivo temporal
        archivoEvidencia?.let { archivo ->
            if (archivo.exists()) {
                archivo.delete()
            }
        }
    }
}

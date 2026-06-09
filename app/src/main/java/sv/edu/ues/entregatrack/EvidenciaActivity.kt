package sv.edu.ues.entregatrack

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
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
import com.bumptech.glide.Glide
import java.io.File

class EvidenciaActivity : ComponentActivity() {

    private lateinit var previewCamara: PreviewView
    private lateinit var imgEvidencia: ImageView
    private lateinit var txtEstadoEvidencia: TextView
    private lateinit var txtRutaFoto: TextView
    private lateinit var btnTomarFoto: Button
    private lateinit var btnGuardarEvidencia: Button

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var fotoMostrada: Boolean = false

    // Controla si la pantalla se abre como repartidor o cliente
    private var modoPantalla: String = "repartidor"

    // Solicita permiso de camara al usuario
    private val solicitarPermisoCamara =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permisoConcedido ->
            if (permisoConcedido) {
                iniciarCamara()
            } else {
                Toast.makeText(this, "Permiso de camara denegado", Toast.LENGTH_SHORT).show()
            }
        }

    // Pantalla para tomar o visualizar evidencia fotografica
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Colores principales de la app
        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        setContentView(R.layout.activity_evidencia)

        previewCamara = findViewById(R.id.previewCamara)
        imgEvidencia = findViewById(R.id.imgEvidencia)
        txtEstadoEvidencia = findViewById(R.id.txtEstadoEvidencia)
        txtRutaFoto = findViewById(R.id.txtRutaFoto)
        btnTomarFoto = findViewById(R.id.btnTomarFoto)
        btnGuardarEvidencia = findViewById(R.id.btnGuardarEvidencia)

        val btnVolverEvidencia = findViewById<Button>(R.id.btnVolverEvidencia)

        // Recibe si la pantalla se abre como cliente o repartidor
        modoPantalla = intent.getStringExtra("modo") ?: "repartidor"

        // Configura la pantalla segun el rol
        if (modoPantalla == "cliente") {
            configurarModoCliente()
        } else {
            configurarModoRepartidor()
        }

        // Regresa a la pantalla anterior
        btnVolverEvidencia.setOnClickListener {
            finish()
        }

        actualizarTextoEvidencia()
        mostrarFotoSiExiste()
    }

    // Configura la pantalla para que el cliente solo visualice la evidencia
    // Configura la pantalla para que el cliente solo visualice la evidencia
    private fun configurarModoCliente() {
        previewCamara.visibility = View.GONE
        btnTomarFoto.visibility = View.GONE
        btnGuardarEvidencia.visibility = View.GONE

        if (DatosEntrega.evidenciaRegistrada && DatosEntrega.rutaFotoEvidencia.isNotEmpty()) {
            txtRutaFoto.text = "Evidencia registrada correctamente"
            mostrarFotoConGlide(DatosEntrega.rutaFotoEvidencia)
        } else if (DatosEntrega.evidenciaRegistrada) {
            txtRutaFoto.text = "Evidencia registrada, imagen no disponible en este dispositivo"
            imgEvidencia.visibility = View.GONE
        } else {
            txtRutaFoto.text = "Aún no hay evidencia registrada"
            imgEvidencia.visibility = View.GONE
        }
    }

    // Configura la pantalla para que el repartidor pueda tomar y guardar evidencia
    private fun configurarModoRepartidor() {
        verificarPermisoCamara()

        // Solo el repartidor puede tomar fotografias
        btnTomarFoto.setOnClickListener {
            if (fotoMostrada) {
                prepararNuevaFoto()
            } else {
                tomarFoto()
            }
        }

        // Solo el repartidor puede guardar evidencia
        btnGuardarEvidencia.setOnClickListener {
            guardarEvidencia()
        }
    }

    // Verifica si la app ya tiene permiso de camara
    private fun verificarPermisoCamara() {
        val permiso = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)

        if (permiso == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara()
        } else {
            solicitarPermisoCamara.launch(Manifest.permission.CAMERA)
        }
    }

    // Inicia la vista previa de CameraX
    private fun iniciarCamara() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider

            // Vista previa de la camara
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewCamara.surfaceProvider)
            }

            // Objeto para capturar fotografias
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                provider.unbindAll()

                // Une la camara al ciclo de vida de la pantalla
                provider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(this, "No se pudo iniciar la camara", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // Captura una foto y la guarda en almacenamiento de la app
    private fun tomarFoto() {
        val captura = imageCapture

        if (captura == null) {
            Toast.makeText(this, "La camara aun no esta lista", Toast.LENGTH_SHORT).show()
            return
        }

        val carpetaFotos = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: filesDir
        val archivoFoto = File(carpetaFotos, "evidencia_${System.currentTimeMillis()}.jpg")

        val opcionesSalida = ImageCapture.OutputFileOptions.Builder(archivoFoto).build()

        captura.takePicture(
            opcionesSalida,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                // Se ejecuta cuando la foto se guarda correctamente
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    DatosEntrega.registrarEvidencia(archivoFoto.absolutePath)

                    actualizarTextoEvidencia()
                    mostrarFotoConGlide(archivoFoto.absolutePath)

                    Toast.makeText(
                        this@EvidenciaActivity,
                        "Foto tomada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Se ejecuta si ocurre un error al tomar la foto
                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        this@EvidenciaActivity,
                        "Error al tomar la foto",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    // Permite volver a mostrar la camara para capturar otra foto
    private fun prepararNuevaFoto() {
        fotoMostrada = false

        imgEvidencia.visibility = View.GONE
        previewCamara.visibility = View.VISIBLE

        btnTomarFoto.text = "Tomar foto"
        txtRutaFoto.text = "Foto: lista para nueva captura"
    }

    // Valida que exista foto y confirma la evidencia en Firebase
    private fun guardarEvidencia() {
        if (DatosEntrega.rutaFotoEvidencia.isEmpty()) {
            Toast.makeText(this, "Primero toma una fotografia", Toast.LENGTH_SHORT).show()
            return
        }

        // Registra evidencia localmente
        DatosEntrega.registrarEvidencia(DatosEntrega.rutaFotoEvidencia)
        actualizarTextoEvidencia()
        mostrarFotoConGlide(DatosEntrega.rutaFotoEvidencia)

        // Sincroniza evidencia con Firebase
        FirebaseEntregaHelper.guardarUbicacionPedido(
            onSuccess = {
                Toast.makeText(this, "Evidencia guardada en Firebase", Toast.LENGTH_SHORT).show()
            },
            onError = { mensaje ->
                Toast.makeText(this, "Error Firebase: $mensaje", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Muestra la foto guardada usando Glide
    private fun mostrarFotoConGlide(rutaFoto: String) {
        fotoMostrada = true

        previewCamara.visibility = View.GONE
        imgEvidencia.visibility = View.VISIBLE

        if (modoPantalla == "repartidor") {
            btnTomarFoto.text = "Tomar otra foto"
        }

        Glide.with(this)
            .load(File(rutaFoto))
            .centerCrop()
            .into(imgEvidencia)
    }

    // Si ya existe una foto, la muestra al abrir la pantalla
    private fun mostrarFotoSiExiste() {
        if (DatosEntrega.rutaFotoEvidencia.isNotEmpty()) {
            mostrarFotoConGlide(DatosEntrega.rutaFotoEvidencia)
        } else if (modoPantalla == "cliente") {
            imgEvidencia.visibility = View.GONE
        }
    }

    // Actualiza los textos visibles en pantalla
    private fun actualizarTextoEvidencia() {
        txtEstadoEvidencia.text = "Estado: ${DatosEntrega.estadoPedido}"

        txtRutaFoto.text = if (DatosEntrega.evidenciaRegistrada) {
            "Foto registrada correctamente"
        } else {
            "Foto: No registrada"
        }
    }

    // Libera la camara al salir de la pantalla
    override fun onDestroy() {
        super.onDestroy()
        cameraProvider?.unbindAll()
    }
}
package sv.edu.ues.entregatrack

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity

class EvidenciaActivity : ComponentActivity() {

    // Pantalla temporal para preparar CameraX y Glide
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Colores principales de la app
        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        // Carga el diseño de evidencia
        setContentView(R.layout.activity_evidencia)

        val btnTomarFoto = findViewById<Button>(R.id.btnTomarFoto)
        val btnGuardarEvidencia = findViewById<Button>(R.id.btnGuardarEvidencia)
        val btnVolverEvidencia = findViewById<Button>(R.id.btnVolverEvidencia)

        // Luego este boton abrira la camara usando CameraX
        btnTomarFoto.setOnClickListener {
            Toast.makeText(this, "Aqui se abrira CameraX para tomar la foto", Toast.LENGTH_SHORT).show()
        }

        // Luego este boton guardara la evidencia en la base de datos o Firebase
        btnGuardarEvidencia.setOnClickListener {
            Toast.makeText(this, "Evidencia registrada correctamente", Toast.LENGTH_SHORT).show()
        }

        // Regresa a la pantalla anterior
        btnVolverEvidencia.setOnClickListener {
            finish()
        }
    }
}
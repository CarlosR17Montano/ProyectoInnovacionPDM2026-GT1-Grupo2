package sv.edu.ues.entregatrack

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity

class MapaActivity : ComponentActivity() {

    // Pantalla temporal para preparar Google Maps y Firebase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Colores principales de la app
        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        setContentView(R.layout.activity_mapa)

        val btnSimularUbicacion = findViewById<Button>(R.id.btnSimularUbicacion)
        val btnVolverMapa = findViewById<Button>(R.id.btnVolverMapa)

        // Simula una actualización de ubicación GPS
        btnSimularUbicacion.setOnClickListener {
            DatosEntrega.actualizarUbicacion()
            Toast.makeText(this, "Ubicación GPS simulada correctamente", Toast.LENGTH_SHORT).show()
        }

        // Regresa a la pantalla anterior
        btnVolverMapa.setOnClickListener {
            finish()
        }
    }
}
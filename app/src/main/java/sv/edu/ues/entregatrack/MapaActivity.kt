package sv.edu.ues.entregatrack

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity

class MapaActivity : ComponentActivity() {

    // Pantalla temporal para preparar la integracion con Google Maps
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Colores de la barra superior e inferior
        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        // Carga el diseño de la pantalla de mapa
        setContentView(R.layout.activity_mapa)

        val btnSimularUbicacion = findViewById<Button>(R.id.btnSimularUbicacion)
        val btnVolverMapa = findViewById<Button>(R.id.btnVolverMapa)

        // Luego este boton enviara coordenadas reales a Firebase
        btnSimularUbicacion.setOnClickListener {
            Toast.makeText(this, "Ubicacion GPS simulada correctamente", Toast.LENGTH_SHORT).show()
        }

        // Regresa a la pantalla anterior
        btnVolverMapa.setOnClickListener {
            finish()
        }
    }
}
package sv.edu.ues.entregatrack

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.dynamic.IFragmentWrapper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapaActivity : FragmentActivity(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null

    private lateinit var txtEstadoMapa: TextView
    private lateinit var txtLatitudMapa: TextView
    private lateinit var txtLongitudMapa: TextView

    // Pantalla de seguimiento GPS con Google Maps
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Colores principales de la app
        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        setContentView(R.layout.activity_mapa)

        txtEstadoMapa = findViewById(R.id.txtEstadoMapa)
        txtLatitudMapa = findViewById(R.id.txtLatitudMapa)
        txtLongitudMapa = findViewById(R.id.txtLongitudMapa)

        val btnSimularUbicacion = findViewById<Button>(R.id.btnSimularUbicacion)
        val btnVolverMapa = findViewById<Button>(R.id.btnVolverMapa)

        // Carga el fragmento de Google Maps
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapaEntrega) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Simula una nueva ubicacion del repartidor
        btnSimularUbicacion.setOnClickListener {
            DatosEntrega.actualizarUbicacion()
            actualizarDatosMapa()
            moverMarcadorRepartidor()

            Toast.makeText(this, "Ubicacion GPS actualizada", Toast.LENGTH_SHORT).show()
        }

        // Regresa a la pantalla anterior
        btnVolverMapa.setOnClickListener {
            finish()
        }

        actualizarDatosMapa()
    }

    // Se ejecuta cuando Google Maps ya esta listo
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Muestra el marcador inicial del repartidor
        moverMarcadorRepartidor()
    }

    // Actualiza los textos de ubicacion
    private fun actualizarDatosMapa() {
        txtEstadoMapa.text = "Estado: ${DatosEntrega.estadoPedido}"
        txtLatitudMapa.text = "Latitud: ${DatosEntrega.latitud}"
        txtLongitudMapa.text = "Longitud: ${DatosEntrega.longitud}"
    }

    // Mueve el marcador del repartidor en el mapa
    private fun moverMarcadorRepartidor() {
        val mapa = googleMap ?: return

        val ubicacionRepartidor = LatLng(DatosEntrega.latitud, DatosEntrega.longitud)

        mapa.clear()

        mapa.addMarker(
            MarkerOptions()
                .position(ubicacionRepartidor)
                .title("Repartidor en ruta")
                .snippet("Pedido ${DatosEntrega.codigoPedido}")
        )

        mapa.moveCamera(
            CameraUpdateFactory.newLatLngZoom(ubicacionRepartidor, 16f)
        )
    }
}
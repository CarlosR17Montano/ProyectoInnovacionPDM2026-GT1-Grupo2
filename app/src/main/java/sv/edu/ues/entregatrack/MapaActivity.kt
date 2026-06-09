package sv.edu.ues.entregatrack

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.ValueEventListener

class MapaActivity : FragmentActivity(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private var listenerFirebase: ValueEventListener? = null

    private lateinit var txtEstadoMapa: TextView
    private lateinit var txtLatitudMapa: TextView
    private lateinit var txtLongitudMapa: TextView
    private lateinit var btnSimularUbicacion: Button

    private var modoPantalla: String = "cliente"

    // Pantalla de seguimiento GPS con Google Maps y Firebase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Colores principales de la app
        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        setContentView(R.layout.activity_mapa)

        txtEstadoMapa = findViewById(R.id.txtEstadoMapa)
        txtLatitudMapa = findViewById(R.id.txtLatitudMapa)
        txtLongitudMapa = findViewById(R.id.txtLongitudMapa)
        btnSimularUbicacion = findViewById(R.id.btnSimularUbicacion)

        val btnVolverMapa = findViewById<Button>(R.id.btnVolverMapa)

        // Recibe si la pantalla se abre como cliente o repartidor
        modoPantalla = intent.getStringExtra("modo") ?: "cliente"

        // El cliente solo visualiza; el repartidor actualiza
        if (modoPantalla == "cliente") {
            btnSimularUbicacion.visibility = View.GONE
        } else {
            btnSimularUbicacion.visibility = View.VISIBLE
        }

        // Carga el fragmento de Google Maps
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapaEntrega) as SupportMapFragment

        mapFragment.getMapAsync(this)

        // Simula nueva ubicacion y la guarda en Firebase
        // Simula nueva ubicacion y la guarda en Firebase
        btnSimularUbicacion.setOnClickListener {
            Toast.makeText(this, "Intentando guardar en Firebase...", Toast.LENGTH_SHORT).show()

            DatosEntrega.actualizarUbicacion()

            FirebaseEntregaHelper.guardarUbicacionPedido(
                onSuccess = {
                    actualizarDatosMapa()
                    moverMarcadorRepartidor()
                    Toast.makeText(this, "Ubicacion guardada en Firebase", Toast.LENGTH_SHORT).show()
                },
                onError = { mensaje ->
                    Toast.makeText(this, "Error Firebase: $mensaje", Toast.LENGTH_LONG).show()
                }
            )
        }

        // Regresa a la pantalla anterior
        btnVolverMapa.setOnClickListener {
            finish()
        }

        actualizarDatosMapa()
        iniciarEscuchaFirebase()
    }

    // Se ejecuta cuando Google Maps ya esta listo
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Muestra el marcador inicial del repartidor
        moverMarcadorRepartidor()
    }

    // Escucha cambios de Firebase en tiempo real
    private fun iniciarEscuchaFirebase() {
        listenerFirebase = FirebaseEntregaHelper.escucharUbicacionPedido(
            onChange = { datos ->
                DatosEntrega.estadoPedido = datos.estadoPedido
                DatosEntrega.latitud = datos.latitud
                DatosEntrega.longitud = datos.longitud
                DatosEntrega.ultimaActualizacion = datos.ultimaActualizacion
                DatosEntrega.evidenciaRegistrada = datos.evidenciaRegistrada
                DatosEntrega.rutaFotoEvidencia = datos.rutaFotoEvidencia

                actualizarDatosMapa()
                moverMarcadorRepartidor()
            },
            onError = { mensaje ->
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
            }
        )
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

    // Detiene la escucha al cerrar la pantalla
    override fun onDestroy() {
        super.onDestroy()
        FirebaseEntregaHelper.detenerEscuchaPedido(listenerFirebase)
    }
}
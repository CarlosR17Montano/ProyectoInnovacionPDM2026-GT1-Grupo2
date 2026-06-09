package sv.edu.ues.entregatrack

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

class SeleccionarUbicacionActivity : FragmentActivity(), OnMapReadyCallback {

    private lateinit var mapa: GoogleMap
    private lateinit var edtBuscarDireccionMapa: EditText
    private lateinit var txtDireccionSeleccionada: TextView
    private lateinit var txtTituloSelectorUbicacion: TextView

    private var tipoUbicacion: String = "recogida"
    private var latitudSeleccionada: Double = 0.0
    private var longitudSeleccionada: Double = 0.0
    private var direccionSeleccionada: String = ""

    private val codigoPermisoUbicacion = 300

    // Pantalla para seleccionar una ubicación dentro de la app
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        setContentView(R.layout.activity_seleccionar_ubicacion)

        tipoUbicacion = intent.getStringExtra("tipoUbicacion") ?: "recogida"
        val direccionInicial = intent.getStringExtra("direccionInicial") ?: ""

        txtTituloSelectorUbicacion = findViewById(R.id.txtTituloSelectorUbicacion)
        edtBuscarDireccionMapa = findViewById(R.id.edtBuscarDireccionMapa)
        txtDireccionSeleccionada = findViewById(R.id.txtDireccionSeleccionada)

        val btnBuscarDireccionMapa = findViewById<Button>(R.id.btnBuscarDireccionMapa)
        val btnUsarMiUbicacionMapa = findViewById<Button>(R.id.btnUsarMiUbicacionMapa)
        val btnConfirmarUbicacion = findViewById<Button>(R.id.btnConfirmarUbicacion)

        txtTituloSelectorUbicacion.text = if (tipoUbicacion == "recogida") {
            "Seleccionar lugar de recogida"
        } else {
            "Seleccionar lugar de entrega"
        }

        edtBuscarDireccionMapa.setText(direccionInicial)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapSeleccionUbicacion) as SupportMapFragment

        mapFragment.getMapAsync(this)

        // Busca la dirección escrita dentro del mapa
        btnBuscarDireccionMapa.setOnClickListener {
            buscarDireccionEnMapa()
        }

        // Usa la ubicación actual del teléfono
        btnUsarMiUbicacionMapa.setOnClickListener {
            obtenerUbicacionActual()
        }

        // Devuelve la ubicación seleccionada al formulario
        btnConfirmarUbicacion.setOnClickListener {
            confirmarUbicacion()
        }
    }

    // Configura el mapa al cargar
    override fun onMapReady(googleMap: GoogleMap) {
        mapa = googleMap

        // Punto inicial: San Salvador
        val sanSalvador = LatLng(13.6929, -89.2182)

        mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(sanSalvador, 12f))

        // Permite seleccionar tocando el mapa
        mapa.setOnMapClickListener { punto ->
            seleccionarPunto(punto)
        }

        // Si ya venía texto escrito, intenta buscarlo
        if (edtBuscarDireccionMapa.text.toString().trim().isNotBlank()) {
            buscarDireccionEnMapa()
        }
    }

    // Busca una dirección escrita usando Geocoder
    private fun buscarDireccionEnMapa() {
        val textoBusqueda = edtBuscarDireccionMapa.text.toString().trim()

        if (textoBusqueda.isBlank()) {
            Toast.makeText(this, "Escribe un lugar o dirección", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val resultados = geocoder.getFromLocationName(textoBusqueda, 1)

            if (resultados.isNullOrEmpty()) {
                Toast.makeText(this, "No se encontró la dirección", Toast.LENGTH_LONG).show()
                return
            }

            val resultado = resultados[0]
            val punto = LatLng(resultado.latitude, resultado.longitude)

            seleccionarPunto(punto)

        } catch (e: Exception) {
            Toast.makeText(this, "Error al buscar dirección", Toast.LENGTH_LONG).show()
        }
    }

    // Selecciona un punto en el mapa
    private fun seleccionarPunto(punto: LatLng) {
        latitudSeleccionada = punto.latitude
        longitudSeleccionada = punto.longitude

        direccionSeleccionada = obtenerDireccionDesdeCoordenadas(
            latitudSeleccionada,
            longitudSeleccionada
        )

        mapa.clear()
        mapa.addMarker(
            MarkerOptions()
                .position(punto)
                .title("Ubicación seleccionada")
        )

        mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(punto, 16f))

        txtDireccionSeleccionada.text =
            "Seleccionado: $direccionSeleccionada\nLat: $latitudSeleccionada, Lng: $longitudSeleccionada"
    }

    // Obtiene la ubicación actual del dispositivo
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
                    Toast.makeText(this, "Activa el GPS e intenta de nuevo", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val punto = LatLng(location.latitude, location.longitude)
                seleccionarPunto(punto)
            }
            .addOnFailureListener {
                Toast.makeText(this, "No se pudo obtener ubicación actual", Toast.LENGTH_LONG).show()
            }
    }

    // Convierte coordenadas a texto
    private fun obtenerDireccionDesdeCoordenadas(latitud: Double, longitud: Double): String {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val direcciones = geocoder.getFromLocation(latitud, longitud, 1)

            if (!direcciones.isNullOrEmpty()) {
                direcciones[0].getAddressLine(0) ?: "Ubicación seleccionada"
            } else {
                "Ubicación seleccionada"
            }
        } catch (e: Exception) {
            "Ubicación seleccionada"
        }
    }

    // Envía el resultado al formulario
    private fun confirmarUbicacion() {
        if (latitudSeleccionada == 0.0 && longitudSeleccionada == 0.0) {
            Toast.makeText(this, "Selecciona una ubicación primero", Toast.LENGTH_SHORT).show()
            return
        }

        val resultado = Intent()
        resultado.putExtra("tipoUbicacion", tipoUbicacion)
        resultado.putExtra("direccionSeleccionada", direccionSeleccionada)
        resultado.putExtra("latitudSeleccionada", latitudSeleccionada)
        resultado.putExtra("longitudSeleccionada", longitudSeleccionada)

        setResult(RESULT_OK, resultado)
        finish()
    }

    // Si acepta permisos, vuelve a buscar ubicación actual
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == codigoPermisoUbicacion &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            obtenerUbicacionActual()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }
}
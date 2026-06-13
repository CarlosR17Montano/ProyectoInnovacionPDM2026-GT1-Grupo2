package sv.edu.ues.entregatrack

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MapaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var txtEstadoMapa: TextView
    private lateinit var txtLatitudMapa: TextView
    private lateinit var txtLongitudMapa: TextView
    private lateinit var btnActualizarUbicacion: Button
    private lateinit var btnVolverMapa: Button

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var googleMap: GoogleMap? = null
    private var codigoPedidoActual: String = ""
    private var modoPantalla: String = "cliente"

    private val database = FirebaseDatabase
        .getInstance("https://entregatrackpdm-1792b-default-rtdb.firebaseio.com/")
        .reference

    private var pedidoListener: ValueEventListener? = null

    // Solicita permiso de ubicación cuando el usuario lo permite desde Android
    private val permisoUbicacionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permitido ->
            if (permitido) {
                actualizarUbicacionRealRepartidor()
            } else {
                Toast.makeText(
                    this,
                    "Permiso de ubicación denegado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // Pantalla de mapa para cliente y repartidor
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#D81B60")
        window.navigationBarColor = Color.parseColor("#FFF3B0")

        setContentView(R.layout.activity_mapa)

        txtEstadoMapa = findViewById(R.id.txtEstadoMapa)
        txtLatitudMapa = findViewById(R.id.txtLatitudMapa)
        txtLongitudMapa = findViewById(R.id.txtLongitudMapa)
        btnActualizarUbicacion = findViewById(R.id.btnSimularUbicacion)
        btnVolverMapa = findViewById(R.id.btnVolverMapa)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Recibe datos enviados desde ClienteActivity o RepartidorActivity
        modoPantalla = intent.getStringExtra("modo") ?: "cliente"
        codigoPedidoActual = intent.getStringExtra("codigoPedido") ?: DatosEntrega.codigoPedido

        if (codigoPedidoActual.isBlank()) {
            codigoPedidoActual = DatosEntrega.codigoPedido
        }

        configurarTextoInicial()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapaEntrega) as SupportMapFragment

        mapFragment.getMapAsync(this)

        btnActualizarUbicacion.setOnClickListener {
            if (modoPantalla == "repartidor") {
                validarPermisoYActualizarUbicacion()
            } else {
                cargarUbicacionPedidoUnaVez()
            }
        }

        btnVolverMapa.setOnClickListener {
            finish()
        }
    }

    // Se ejecuta cuando Google Maps está listo
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        val ubicacionInicial = LatLng(13.6420, -88.7853)

        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                ubicacionInicial,
                14f
            )
        )

        if (modoPantalla == "repartidor") {
            validarPermisoYActualizarUbicacion()
        } else {
            escucharUbicacionRepartidor()
        }
    }

    // Configura textos según modo cliente o repartidor
    private fun configurarTextoInicial() {
        if (modoPantalla == "repartidor") {
            txtEstadoMapa.text = "Modo repartidor: actualiza tu ubicación GPS"
            btnActualizarUbicacion.text = "Actualizar ubicación GPS"
        } else {
            txtEstadoMapa.text = "Modo cliente: ubicación actual del repartidor"
            btnActualizarUbicacion.text = "Actualizar mapa"
        }

        txtLatitudMapa.text = "Latitud: pendiente"
        txtLongitudMapa.text = "Longitud: pendiente"
    }

    // Valida permiso antes de usar GPS
    private fun validarPermisoYActualizarUbicacion() {
        val permiso = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (permiso == PackageManager.PERMISSION_GRANTED) {
            actualizarUbicacionRealRepartidor()
        } else {
            permisoUbicacionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Obtiene ubicación real del teléfono del repartidor
    private fun actualizarUbicacionRealRepartidor() {
        if (codigoPedidoActual.isBlank()) {
            Toast.makeText(
                this,
                "No hay pedido seleccionado",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val permiso = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (permiso != PackageManager.PERMISSION_GRANTED) {
            permisoUbicacionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        try {
            val token = CancellationTokenSource()

            fusedLocationClient
                .getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    token.token
                )
                .addOnSuccessListener { location ->
                    if (location != null) {
                        procesarUbicacionRepartidor(location)
                    } else {
                        obtenerUltimaUbicacionDisponible()
                    }
                }
                .addOnFailureListener {
                    obtenerUltimaUbicacionDisponible()
                }

        } catch (e: SecurityException) {
            Toast.makeText(
                this,
                "No se pudo acceder a la ubicación",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Respaldo por si getCurrentLocation devuelve null
    private fun obtenerUltimaUbicacionDisponible() {
        val permiso = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (permiso != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        procesarUbicacionRepartidor(location)
                    } else {
                        Toast.makeText(
                            this,
                            "No se pudo obtener ubicación. Activa GPS e inténtalo nuevamente.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        this,
                        "Error al obtener ubicación",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } catch (e: SecurityException) {
            Toast.makeText(
                this,
                "Permiso de ubicación no disponible",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Procesa y guarda la ubicación real del repartidor
    private fun procesarUbicacionRepartidor(location: Location) {
        val latitud = location.latitude
        val longitud = location.longitude

        DatosEntrega.latitud = latitud
        DatosEntrega.longitud = longitud

        txtLatitudMapa.text = "Latitud: $latitud"
        txtLongitudMapa.text = "Longitud: $longitud"
        txtEstadoMapa.text = "Ubicación GPS actualizada"

        moverMarcador(
            latitud = latitud,
            longitud = longitud,
            titulo = "Ubicación actual del repartidor"
        )

        FirebasePedidoHelper.actualizarUbicacionRepartidor(
            codigoPedido = codigoPedidoActual,
            latitud = latitud,
            longitud = longitud,
            onSuccess = {
                Toast.makeText(
                    this,
                    "Ubicación guardada en Firebase",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onError = { mensaje ->
                Toast.makeText(
                    this,
                    "Error Firebase: $mensaje",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    // Cliente escucha en tiempo real la ubicación del repartidor
    private fun escucharUbicacionRepartidor() {
        if (codigoPedidoActual.isBlank()) {
            txtEstadoMapa.text = "No hay pedido seleccionado"
            return
        }

        pedidoListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pedido = snapshot.getValue(PedidoFirebase::class.java)

                if (pedido == null) {
                    txtEstadoMapa.text = "No se encontró el pedido"
                    return
                }

                if (pedido.latitud == 0.0 && pedido.longitud == 0.0) {
                    txtEstadoMapa.text = "El repartidor aún no ha actualizado su ubicación"
                    txtLatitudMapa.text = "Latitud: pendiente"
                    txtLongitudMapa.text = "Longitud: pendiente"
                    return
                }

                txtEstadoMapa.text = pedido.ultimaActualizacion
                txtLatitudMapa.text = "Latitud: ${pedido.latitud}"
                txtLongitudMapa.text = "Longitud: ${pedido.longitud}"

                moverMarcador(
                    latitud = pedido.latitud,
                    longitud = pedido.longitud,
                    titulo = "Ubicación actual del repartidor"
                )
            }

            override fun onCancelled(error: DatabaseError) {
                txtEstadoMapa.text = "Error al escuchar ubicación"
            }
        }

        database.child("pedidos")
            .child(codigoPedidoActual)
            .addValueEventListener(pedidoListener!!)
    }

    // Cliente puede actualizar manualmente el mapa
    private fun cargarUbicacionPedidoUnaVez() {
        if (codigoPedidoActual.isBlank()) {
            txtEstadoMapa.text = "No hay pedido seleccionado"
            return
        }

        database.child("pedidos")
            .child(codigoPedidoActual)
            .get()
            .addOnSuccessListener { snapshot ->
                val pedido = snapshot.getValue(PedidoFirebase::class.java)

                if (pedido == null) {
                    txtEstadoMapa.text = "No se encontró el pedido"
                    return@addOnSuccessListener
                }

                if (pedido.latitud == 0.0 && pedido.longitud == 0.0) {
                    txtEstadoMapa.text = "El repartidor aún no ha actualizado su ubicación"
                    return@addOnSuccessListener
                }

                txtEstadoMapa.text = pedido.ultimaActualizacion
                txtLatitudMapa.text = "Latitud: ${pedido.latitud}"
                txtLongitudMapa.text = "Longitud: ${pedido.longitud}"

                moverMarcador(
                    latitud = pedido.latitud,
                    longitud = pedido.longitud,
                    titulo = "Ubicación actual del repartidor"
                )
            }
            .addOnFailureListener {
                txtEstadoMapa.text = "Error al cargar ubicación"
            }
    }

    // Dibuja marcador en el mapa
    private fun moverMarcador(
        latitud: Double,
        longitud: Double,
        titulo: String
    ) {
        val mapa = googleMap ?: return

        val ubicacion = LatLng(latitud, longitud)

        mapa.clear()
        mapa.addMarker(
            MarkerOptions()
                .position(ubicacion)
                .title(titulo)
        )

        mapa.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                ubicacion,
                17f
            )
        )
    }

    // Limpia listener para evitar fugas de memoria
    override fun onDestroy() {
        super.onDestroy()

        if (pedidoListener != null && codigoPedidoActual.isNotBlank()) {
            database.child("pedidos")
                .child(codigoPedidoActual)
                .removeEventListener(pedidoListener!!)
        }
    }
}
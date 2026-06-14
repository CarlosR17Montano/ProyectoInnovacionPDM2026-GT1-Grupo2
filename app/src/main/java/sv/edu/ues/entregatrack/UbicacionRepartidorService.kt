
package sv.edu.ues.entregatrack

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// Comparte automáticamente la ubicación del repartidor
class UbicacionRepartidorService : Service() {

    // Cliente para obtener coordenadas
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Recibe las nuevas ubicaciones
    private lateinit var locationCallback: LocationCallback

    // Código del pedido activo
    private var codigoPedidoActual: String = ""

    // Evita iniciar el seguimiento varias veces
    private var seguimientoIniciado = false

    // Evita envíos simultáneos a Firebase
    private var enviandoUbicacion = false

    // Referencia del pedido en Firebase
    private var referenciaPedido: DatabaseReference? = null

    // Listener del estado del pedido
    private var listenerPedido: ValueEventListener? = null

    override fun onCreate() {
        super.onCreate()

        // Inicializa el proveedor de ubicación
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        // Crea el canal de la notificación
        crearCanalNotificacion()

        // Prepara el receptor de ubicaciones
        locationCallback =
            object : LocationCallback() {

                override fun onLocationResult(
                    resultado: LocationResult
                ) {
                    super.onLocationResult(resultado)

                    val ubicacion =
                        resultado.lastLocation ?: return

                    enviarUbicacionFirebase(
                        latitud = ubicacion.latitude,
                        longitud = ubicacion.longitude
                    )
                }
            }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        // Recibe el código enviado por RepartidorActivity
        val codigoRecibido =
            intent?.getStringExtra(EXTRA_CODIGO_PEDIDO)
                .orEmpty()

        if (codigoRecibido.isNotBlank()) {
            codigoPedidoActual = codigoRecibido

            guardarCodigoPedido(
                codigoPedidoActual
            )
        }

        // Recupera el pedido si Android recreó el servicio
        if (codigoPedidoActual.isBlank()) {
            codigoPedidoActual =
                recuperarCodigoPedido()
        }

        // Detiene el servicio si no hay pedido
        if (codigoPedidoActual.isBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Muestra la notificación permanente
        iniciarServicioPrimerPlano()

        // Inicia el GPS una sola vez
        if (!seguimientoIniciado) {
            seguimientoIniciado = true

            escucharEstadoPedido()
            iniciarActualizacionesUbicacion()
        }

        return START_STICKY
    }

    // Inicia la notificación del servicio
    private fun iniciarServicioPrimerPlano() {

        val intentPantalla =
            Intent(
                this,
                RepartidorActivity::class.java
            ).apply {
                putExtra(
                    "codigoPedido",
                    codigoPedidoActual
                )
            }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                501,
                intentPantalla,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val notificacion =
            NotificationCompat.Builder(
                this,
                CANAL_UBICACION
            )
                .setSmallIcon(
                    android.R.drawable.ic_menu_mylocation
                )
                .setContentTitle(
                    "EntregaTrack"
                )
                .setContentText(
                    "Compartiendo ubicación del pedido $codigoPedidoActual"
                )
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(
                    NotificationCompat.PRIORITY_LOW
                )
                .build()

        // Usa el tipo location desde Android 10
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                ID_NOTIFICACION,
                notificacion,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(
                ID_NOTIFICACION,
                notificacion
            )
        }
    }

    // Solicita actualizaciones periódicas
    private fun iniciarActualizacionesUbicacion() {

        val permisoPreciso =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val permisoAproximado =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        // Detiene el servicio sin permisos
        if (!permisoPreciso && !permisoAproximado) {
            detenerServicioCompleto()
            return
        }

        val solicitudUbicacion =
            LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                5_000L
            )
                .setMinUpdateIntervalMillis(
                    3_000L
                )
                .setMinUpdateDistanceMeters(
                    10f
                )
                .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                solicitudUbicacion,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (error: SecurityException) {
            detenerServicioCompleto()
        }
    }

    // Envía la coordenada a Firebase
    private fun enviarUbicacionFirebase(
        latitud: Double,
        longitud: Double
    ) {

        if (enviandoUbicacion) {
            return
        }

        if (latitud == 0.0 || longitud == 0.0) {
            return
        }

        if (codigoPedidoActual.isBlank()) {
            return
        }

        enviandoUbicacion = true

        FirebasePedidoHelper.actualizarUbicacionRepartidor(
            codigoPedido = codigoPedidoActual,
            latitud = latitud,
            longitud = longitud,

            onSuccess = {
                enviandoUbicacion = false
            },

            onError = {
                enviandoUbicacion = false
            }
        )
    }

    // Escucha cuando el pedido termina
    private fun escucharEstadoPedido() {

        referenciaPedido =
            FirebaseDatabase.getInstance(
                "https://entregatrackpdm-1792b-default-rtdb.firebaseio.com/"
            )
                .reference
                .child("pedidos")
                .child(codigoPedidoActual)

        listenerPedido =
            object : ValueEventListener {

                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {

                    val estado =
                        snapshot.child("estadoPedido")
                            .getValue(String::class.java)
                            .orEmpty()

                    val finalizado =
                        snapshot.child("finalizado")
                            .getValue(Boolean::class.java)
                            ?: false

                    val debeDetenerse =
                        finalizado ||
                                estado.equals(
                                    "Entregado",
                                    ignoreCase = true
                                ) ||
                                estado.equals(
                                    "Finalizado",
                                    ignoreCase = true
                                ) ||
                                estado.equals(
                                    "Cancelado",
                                    ignoreCase = true
                                )

                    if (debeDetenerse) {
                        detenerServicioCompleto()
                    }
                }

                override fun onCancelled(
                    error: DatabaseError
                ) {
                    // Mantiene el servicio ante un fallo temporal
                }
            }

        listenerPedido?.let { listener ->
            referenciaPedido?.addValueEventListener(
                listener
            )
        }
    }

    // Crea el canal de seguimiento
    private fun crearCanalNotificacion() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val canal =
                NotificationChannel(
                    CANAL_UBICACION,
                    "Ubicación del repartidor",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description =
                        "Muestra que el repartidor comparte su ubicación"
                }

            val administrador =
                getSystemService(
                    NotificationManager::class.java
                )

            administrador.createNotificationChannel(
                canal
            )
        }
    }

    // Guarda el pedido para recuperar el servicio
    private fun guardarCodigoPedido(
        codigoPedido: String
    ) {
        getSharedPreferences(
            PREFERENCIAS_SERVICIO,
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                EXTRA_CODIGO_PEDIDO,
                codigoPedido
            )
            .apply()
    }

    // Recupera el pedido guardado
    private fun recuperarCodigoPedido(): String {
        return getSharedPreferences(
            PREFERENCIAS_SERVICIO,
            Context.MODE_PRIVATE
        )
            .getString(
                EXTRA_CODIGO_PEDIDO,
                ""
            )
            .orEmpty()
    }

    // Detiene el GPS y limpia recursos
    private fun detenerServicioCompleto() {

        getSharedPreferences(
            PREFERENCIAS_SERVICIO,
            Context.MODE_PRIVATE
        )
            .edit()
            .remove(EXTRA_CODIGO_PEDIDO)
            .apply()

        if (
            ::fusedLocationClient.isInitialized &&
            ::locationCallback.isInitialized
        ) {
            fusedLocationClient.removeLocationUpdates(
                locationCallback
            )
        }

        listenerPedido?.let { listener ->
            referenciaPedido?.removeEventListener(
                listener
            )
        }

        listenerPedido = null
        referenciaPedido = null
        seguimientoIniciado = false
        enviandoUbicacion = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(
                STOP_FOREGROUND_REMOVE
            )
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (
            ::fusedLocationClient.isInitialized &&
            ::locationCallback.isInitialized
        ) {
            fusedLocationClient.removeLocationUpdates(
                locationCallback
            )
        }

        listenerPedido?.let { listener ->
            referenciaPedido?.removeEventListener(
                listener
            )
        }

        listenerPedido = null
        referenciaPedido = null
    }

    // Este servicio no utiliza conexión directa
    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }

    companion object {

        // Clave utilizada para enviar el código
        const val EXTRA_CODIGO_PEDIDO =
            "codigoPedidoSeguimiento"

        // Canal de la notificación
        private const val CANAL_UBICACION =
            "canal_ubicacion_repartidor"

        // Identificador de la notificación
        private const val ID_NOTIFICACION =
            501

        // Nombre de las preferencias
        private const val PREFERENCIAS_SERVICIO =
            "preferencias_ubicacion_repartidor"

        // Inicia el servicio desde una Activity
        fun iniciar(
            context: Context,
            codigoPedido: String
        ) {
            if (codigoPedido.isBlank()) {
                return
            }

            val intentServicio =
                Intent(
                    context,
                    UbicacionRepartidorService::class.java
                ).apply {
                    putExtra(
                        EXTRA_CODIGO_PEDIDO,
                        codigoPedido
                    )
                }

            ContextCompat.startForegroundService(
                context,
                intentServicio
            )
        }

        // Detiene el servicio
        fun detener(
            context: Context
        ) {
            val intentServicio =
                Intent(
                    context,
                    UbicacionRepartidorService::class.java
                )

            context.stopService(
                intentServicio
            )

            context.getSharedPreferences(
                PREFERENCIAS_SERVICIO,
                Context.MODE_PRIVATE
            )
                .edit()
                .remove(EXTRA_CODIGO_PEDIDO)
                .apply()
        }
    }
}


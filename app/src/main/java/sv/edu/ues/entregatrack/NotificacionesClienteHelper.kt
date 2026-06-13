package sv.edu.ues.entregatrack

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

// Modelo de las notificaciones guardadas en Firebase
data class NotificacionClienteFirebase(
    var titulo: String = "",
    var mensaje: String = "",
    var codigoPedido: String = "",
    var estadoPedido: String = "",
    var leida: Boolean = false,
    var fecha: Long = 0L
)

object NotificacionesClienteHelper {

    private const val CANAL_ID = "entregatrack_pedidos"
    private const val CANAL_NOMBRE = "Seguimiento de pedidos"
    private const val SOLICITUD_NOTIFICACIONES = 3001

    private val auth = FirebaseAuth.getInstance()

    private val database = FirebaseDatabase
        .getInstance(
            "https://entregatrackpdm-1792b-default-rtdb.firebaseio.com/"
        )
        .reference

    private var listenerNotificaciones: ChildEventListener? = null
    private var referenciaNotificaciones: DatabaseReference? = null

    // Momento desde el cual se escucharán notificaciones nuevas
    private var momentoInicioEscucha: Long = 0L

    // Solicita permiso para mostrar notificaciones en Android 13 o superior
    fun solicitarPermisoNotificaciones(activity: ComponentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            val permisoConcedido = ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!permisoConcedido) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    SOLICITUD_NOTIFICACIONES
                )
            }
        }
    }

    // Empieza a escuchar las notificaciones del cliente autenticado
    fun iniciarEscucha(activity: ComponentActivity) {
        val uidCliente = auth.currentUser?.uid

        if (uidCliente.isNullOrBlank()) {
            return
        }

        // Evita crear dos listeners al mismo tiempo
        detenerEscucha()

        crearCanalNotificaciones(activity)

        momentoInicioEscucha = System.currentTimeMillis()

        val referencia = database
            .child("notificaciones")
            .child(uidCliente)

        referenciaNotificaciones = referencia

        listenerNotificaciones = object : ChildEventListener {

            override fun onChildAdded(
                snapshot: DataSnapshot,
                previousChildName: String?
            ) {
                val notificacion = snapshot.getValue(
                    NotificacionClienteFirebase::class.java
                ) ?: return

                /*
                 * Solo muestra notificaciones nuevas.
                 * Esto evita que al abrir la app aparezcan todas las antiguas.
                 */
                val esNotificacionNueva =
                    notificacion.fecha >= momentoInicioEscucha - 3000L

                if (!notificacion.leida && esNotificacionNueva) {
                    mostrarNotificacionLocal(
                        activity = activity,
                        notificacion = notificacion
                    )

                    // Marca la notificación como leída
                    snapshot.ref.child("leida").setValue(true)
                    snapshot.ref.child("fechaLectura")
                        .setValue(System.currentTimeMillis())
                }
            }

            override fun onChildChanged(
                snapshot: DataSnapshot,
                previousChildName: String?
            ) {
                // No se necesita por ahora
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // No se necesita por ahora
            }

            override fun onChildMoved(
                snapshot: DataSnapshot,
                previousChildName: String?
            ) {
                // No se necesita por ahora
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    activity,
                    "Error al recibir notificaciones: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        referencia.addChildEventListener(listenerNotificaciones!!)
    }

    // Elimina el listener para evitar duplicados
    fun detenerEscucha() {
        val referencia = referenciaNotificaciones
        val listener = listenerNotificaciones

        if (referencia != null && listener != null) {
            referencia.removeEventListener(listener)
        }

        referenciaNotificaciones = null
        listenerNotificaciones = null
    }

    // Crea el canal requerido desde Android 8
    private fun crearCanalNotificaciones(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val canal = NotificationChannel(
                CANAL_ID,
                CANAL_NOMBRE,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    "Notificaciones sobre el estado de los pedidos de EntregaTrack"
                enableVibration(true)
            }

            val administrador = context.getSystemService(
                NotificationManager::class.java
            )

            administrador.createNotificationChannel(canal)
        }
    }

    // Muestra la notificación en el teléfono del cliente
    private fun mostrarNotificacionLocal(
        activity: ComponentActivity,
        notificacion: NotificacionClienteFirebase
    ) {
        // Muestra también un mensaje dentro de la app
        Toast.makeText(
            activity,
            "${notificacion.titulo}: ${notificacion.mensaje}",
            Toast.LENGTH_LONG
        ).show()

        val intent = Intent(
            activity,
            HistorialPedidosActivity::class.java
        ).apply {
            putExtra("codigoPedido", notificacion.codigoPedido)

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val solicitudIntent =
            notificacion.codigoPedido.hashCode()

        val pendingIntent = PendingIntent.getActivity(
            activity,
            solicitudIntent,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notificacionAndroid = NotificationCompat.Builder(
            activity,
            CANAL_ID
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(
                notificacion.titulo.ifBlank {
                    "EntregaTrack"
                }
            )
            .setContentText(notificacion.mensaje)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notificacion.mensaje)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Android 13 requiere permiso explícito
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val idNotificacion =
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

        NotificationManagerCompat
            .from(activity)
            .notify(idNotificacion, notificacionAndroid)
    }
}
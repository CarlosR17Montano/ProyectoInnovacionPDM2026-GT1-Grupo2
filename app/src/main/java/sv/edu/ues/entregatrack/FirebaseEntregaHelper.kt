package sv.edu.ues.entregatrack

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// Modelo temporal para guardar seguimiento en Firebase
data class UbicacionPedidoFirebase(
    var codigoPedido: String = "",
    var estadoPedido: String = "",
    var latitud: Double = 0.0,
    var longitud: Double = 0.0,
    var ultimaActualizacion: String = "",
    var evidenciaRegistrada: Boolean = false,
    var rutaFotoEvidencia: String = ""
)

object FirebaseEntregaHelper {

    // Referencia principal de la base de datos
    private val database = FirebaseDatabase.getInstance("https://entregatrackpdm-1792b-default-rtdb.firebaseio.com/").reference

    // Guarda la ubicacion actual del pedido en Firebase
    fun guardarUbicacionPedido(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val datos = UbicacionPedidoFirebase(
            codigoPedido = DatosEntrega.codigoPedido,
            estadoPedido = DatosEntrega.estadoPedido,
            latitud = DatosEntrega.latitud,
            longitud = DatosEntrega.longitud,
            ultimaActualizacion = DatosEntrega.ultimaActualizacion,
            evidenciaRegistrada = DatosEntrega.evidenciaRegistrada,
            rutaFotoEvidencia = DatosEntrega.rutaFotoEvidencia
        )

        database
            .child("entregas")
            .child(DatosEntrega.codigoPedido)
            .child("seguimiento")
            .setValue(datos)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Error al guardar en Firebase")
            }
    }

    // Escucha cambios de ubicacion del pedido en tiempo real
    fun escucharUbicacionPedido(
        onChange: (UbicacionPedidoFirebase) -> Unit,
        onError: (String) -> Unit
    ): ValueEventListener {
        val referencia = database
            .child("entregas")
            .child(DatosEntrega.codigoPedido)
            .child("seguimiento")

        val listener = object : ValueEventListener {

            // Se ejecuta cuando Firebase detecta cambios
            override fun onDataChange(snapshot: DataSnapshot) {
                val datos = snapshot.getValue(UbicacionPedidoFirebase::class.java)

                if (datos != null) {
                    onChange(datos)
                }
            }

            // Se ejecuta si Firebase devuelve error
            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }

        referencia.addValueEventListener(listener)
        return listener
    }

    // Detiene la escucha para evitar consumo innecesario
    fun detenerEscuchaPedido(listener: ValueEventListener?) {
        if (listener == null) return

        database
            .child("entregas")
            .child(DatosEntrega.codigoPedido)
            .child("seguimiento")
            .removeEventListener(listener)
    }
}
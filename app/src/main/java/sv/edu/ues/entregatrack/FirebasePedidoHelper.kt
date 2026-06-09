package sv.edu.ues.entregatrack

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Modelo de pedido usado para historial, detalle y solicitud de mandados
data class PedidoFirebase(
    var codigoPedido: String = "",
    var clienteId: String = "",
    var clienteNombre: String = "",
    var telefonoCliente: String = "",

    // Datos del servicio solicitado
    var tipoServicio: String = "",
    var descripcionPedido: String = "",
    var indicacionesRepartidor: String = "",

    // Anexo enviado por el cliente: QR, receta, captura o referencia
    var anexoClienteRegistrado: Boolean = false,
    var urlAnexoCliente: String = "",
    var tipoAnexoCliente: String = "",

    // Direcciones principales
    var direccionRecogida: String = "",
    var direccionEntrega: String = "",
    var referenciaUbicacion: String = "",

    // Ubicación actual del cliente
    var ubicacionActualCliente: String = "",
    var latitudCliente: Double = 0.0,
    var longitudCliente: Double = 0.0,

    // Coordenadas seleccionadas en el mapa
    var latitudRecogida: Double = 0.0,
    var longitudRecogida: Double = 0.0,
    var latitudEntrega: Double = 0.0,
    var longitudEntrega: Double = 0.0,

    // Datos de costo estimado
    var distanciaKm: Double = 0.0,
    var precioEstimado: Double = 0.0,

    // Datos del repartidor
    var repartidorId: String = "",
    var repartidorNombre: String = "",

    // Seguimiento del repartidor
    var estadoPedido: String = "",
    var ultimaActualizacion: String = "",
    var evidenciaRegistrada: Boolean = false,
    var rutaFotoEvidencia: String = "",
    var latitud: Double = 0.0,
    var longitud: Double = 0.0,

    // Control
    var fechaCreacion: String = "",
    var finalizado: Boolean = false,
    var orden: Long = 0
)

object FirebasePedidoHelper {

    private val auth = FirebaseAuth.getInstance()

    // Referencia principal de Firebase Realtime Database
    private val database = FirebaseDatabase
        .getInstance("https://entregatrackpdm-1792b-default-rtdb.firebaseio.com/")
        .reference

    // Crea una solicitud de entrega realizada por el cliente
    fun crearPedidoCliente(
        tipoServicio: String,
        direccionRecogida: String,
        direccionEntrega: String,
        telefonoCliente: String,
        referenciaUbicacion: String,
        descripcionPedido: String,
        indicacionesRepartidor: String,

        // Datos del anexo del cliente
        anexoClienteRegistrado: Boolean,
        urlAnexoCliente: String,
        tipoAnexoCliente: String,

        distanciaKm: Double,
        precioEstimado: Double,

        ubicacionActualCliente: String,
        latitudCliente: Double,
        longitudCliente: Double,

        latitudRecogida: Double,
        longitudRecogida: Double,
        latitudEntrega: Double,
        longitudEntrega: Double,

        onSuccess: (PedidoFirebase) -> Unit,
        onError: (String) -> Unit
    ) {
        val uidCliente = auth.currentUser?.uid

        if (uidCliente.isNullOrEmpty()) {
            onError("No hay usuario autenticado")
            return
        }

        if (
            tipoServicio.isBlank() ||
            direccionRecogida.isBlank() ||
            direccionEntrega.isBlank() ||
            telefonoCliente.isBlank() ||
            descripcionPedido.isBlank()
        ) {
            onError("Completa tipo de servicio, direcciones, teléfono y descripción")
            return
        }

        if (distanciaKm <= 0.0) {
            onError("Ingresa una distancia estimada válida")
            return
        }

        // Busca los datos del cliente autenticado
        database.child("usuarios")
            .child(uidCliente)
            .get()
            .addOnSuccessListener { snapshot ->
                val usuario = snapshot.getValue(UsuarioFirebase::class.java)

                if (usuario == null) {
                    onError("No se encontró el perfil del cliente")
                    return@addOnSuccessListener
                }

                val fechaCodigo = SimpleDateFormat(
                    "yyyyMMddHHmmss",
                    Locale.getDefault()
                ).format(Date())

                val fechaVisible = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm",
                    Locale.getDefault()
                ).format(Date())

                val codigoPedido = "PED-$fechaCodigo"
                val ordenPedido = System.currentTimeMillis()

                val pedido = PedidoFirebase(
                    codigoPedido = codigoPedido,
                    clienteId = uidCliente,
                    clienteNombre = usuario.nombre,
                    telefonoCliente = telefonoCliente,

                    tipoServicio = tipoServicio,
                    descripcionPedido = descripcionPedido,
                    indicacionesRepartidor = indicacionesRepartidor,

                    anexoClienteRegistrado = anexoClienteRegistrado,
                    urlAnexoCliente = urlAnexoCliente,
                    tipoAnexoCliente = tipoAnexoCliente,

                    direccionRecogida = direccionRecogida,
                    direccionEntrega = direccionEntrega,
                    referenciaUbicacion = referenciaUbicacion,

                    ubicacionActualCliente = ubicacionActualCliente,
                    latitudCliente = latitudCliente,
                    longitudCliente = longitudCliente,

                    latitudRecogida = latitudRecogida,
                    longitudRecogida = longitudRecogida,
                    latitudEntrega = latitudEntrega,
                    longitudEntrega = longitudEntrega,

                    distanciaKm = distanciaKm,
                    precioEstimado = precioEstimado,

                    repartidorId = "",
                    repartidorNombre = "Pendiente de asignación",

                    estadoPedido = "Solicitado",
                    ultimaActualizacion = "Pedido solicitado por el cliente",
                    evidenciaRegistrada = false,
                    rutaFotoEvidencia = "",

                    // Estas coordenadas serán del repartidor cuando acepte y actualice ubicación
                    latitud = 0.0,
                    longitud = 0.0,

                    fechaCreacion = fechaVisible,
                    finalizado = false,
                    orden = ordenPedido
                )

                database.child("pedidos")
                    .child(codigoPedido)
                    .setValue(pedido)
                    .addOnSuccessListener {
                        onSuccess(pedido)
                    }
                    .addOnFailureListener { error ->
                        onError(error.message ?: "Error al guardar el pedido")
                    }
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Error al consultar datos del cliente")
            }
    }

    // Carga los pedidos del cliente autenticado
    fun cargarPedidosCliente(
        onSuccess: (List<PedidoFirebase>) -> Unit,
        onError: (String) -> Unit
    ) {
        val uidCliente = auth.currentUser?.uid

        if (uidCliente.isNullOrEmpty()) {
            onError("No hay usuario autenticado")
            return
        }

        database.child("pedidos")
            .orderByChild("clienteId")
            .equalTo(uidCliente)
            .get()
            .addOnSuccessListener { snapshot ->

                val pedidos = mutableListOf<PedidoFirebase>()

                for (item in snapshot.children) {
                    val pedido = item.getValue(PedidoFirebase::class.java)

                    if (pedido != null) {
                        pedidos.add(pedido)
                    }
                }

                // Ordena: primero activos, luego los más recientes
                val pedidosOrdenados = pedidos.sortedWith(
                    compareBy<PedidoFirebase> { it.finalizado }
                        .thenByDescending { it.orden }
                )

                onSuccess(pedidosOrdenados)
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Error al cargar pedidos")
            }
    }

    // Carga un pedido específico por código
    fun cargarPedidoPorCodigo(
        codigoPedido: String,
        onSuccess: (PedidoFirebase) -> Unit,
        onError: (String) -> Unit
    ) {
        database.child("pedidos")
            .child(codigoPedido)
            .get()
            .addOnSuccessListener { snapshot ->
                val pedido = snapshot.getValue(PedidoFirebase::class.java)

                if (pedido != null) {
                    onSuccess(pedido)
                } else {
                    onError("No se encontró el pedido")
                }
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Error al consultar pedido")
            }
    }

    // Actualiza el objeto temporal DatosEntrega con el pedido seleccionado
    fun aplicarPedidoADatosEntrega(pedido: PedidoFirebase) {
        DatosEntrega.codigoPedido = pedido.codigoPedido
        DatosEntrega.clientePedido = pedido.clienteNombre
        DatosEntrega.repartidorPedido = pedido.repartidorNombre
        DatosEntrega.direccionPedido = pedido.direccionEntrega
        DatosEntrega.estadoPedido = pedido.estadoPedido
        DatosEntrega.ultimaActualizacion = pedido.ultimaActualizacion
        DatosEntrega.evidenciaRegistrada = pedido.evidenciaRegistrada
        DatosEntrega.rutaFotoEvidencia = pedido.rutaFotoEvidencia

        // Estas coordenadas son del repartidor cuando ya esté en ruta
        DatosEntrega.latitud = pedido.latitud
        DatosEntrega.longitud = pedido.longitud
    }
}
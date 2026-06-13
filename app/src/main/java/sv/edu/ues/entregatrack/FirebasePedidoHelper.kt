package sv.edu.ues.entregatrack

import android.location.Location
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

    // Seguimiento del pedido
    var estadoPedido: String = "",
    var ultimaActualizacion: String = "",
    var latitud: Double = 0.0,
    var longitud: Double = 0.0,

    // Evidencia final compatible con ClienteActivity
    var evidenciaRegistrada: Boolean = false,
    var rutaFotoEvidencia: String = "",

    // Evidencia de recogida
    var evidenciaRecogidaRegistrada: Boolean = false,
    var urlEvidenciaRecogida: String = "",

    // Evidencia de entrega
    var evidenciaEntregaRegistrada: Boolean = false,
    var urlEvidenciaEntrega: String = "",

    // Control de cercanía al punto de entrega
    var notificacionCercaniaEnviada: Boolean = false,
    var distanciaRestanteEntregaMetros: Double = 0.0,

    // Control general
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

                    latitud = 0.0,
                    longitud = 0.0,

                    evidenciaRegistrada = false,
                    rutaFotoEvidencia = "",

                    evidenciaRecogidaRegistrada = false,
                    urlEvidenciaRecogida = "",

                    evidenciaEntregaRegistrada = false,
                    urlEvidenciaEntrega = "",

                    notificacionCercaniaEnviada = false,
                    distanciaRestanteEntregaMetros = 0.0,

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

    // Carga pedidos disponibles para que el repartidor pueda aceptarlos
    fun cargarSolicitudesDisponibles(
        onSuccess: (List<PedidoFirebase>) -> Unit,
        onError: (String) -> Unit
    ) {
        database.child("pedidos")
            .orderByChild("estadoPedido")
            .equalTo("Solicitado")
            .get()
            .addOnSuccessListener { snapshot ->

                val solicitudes = mutableListOf<PedidoFirebase>()

                for (item in snapshot.children) {
                    val pedido = item.getValue(PedidoFirebase::class.java)

                    if (pedido != null) {
                        solicitudes.add(pedido)
                    }
                }

                val solicitudesOrdenadas = solicitudes.sortedByDescending { it.orden }
                onSuccess(solicitudesOrdenadas)
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Error al cargar solicitudes")
            }
    }

    // Permite que el repartidor autenticado acepte una solicitud
    fun aceptarSolicitudRepartidor(
        pedido: PedidoFirebase,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uidRepartidor = auth.currentUser?.uid

        if (uidRepartidor.isNullOrEmpty()) {
            onError("No hay repartidor autenticado")
            return
        }

        database.child("pedidos")
            .child(pedido.codigoPedido)
            .get()
            .addOnSuccessListener { pedidoSnapshot ->

                val pedidoActual = pedidoSnapshot.getValue(PedidoFirebase::class.java)

                if (pedidoActual == null) {
                    onError("No se encontró el pedido")
                    return@addOnSuccessListener
                }

                if (pedidoActual.estadoPedido != "Solicitado") {
                    onError("Esta solicitud ya fue tomada por otro repartidor")
                    return@addOnSuccessListener
                }

                database.child("usuarios")
                    .child(uidRepartidor)
                    .get()
                    .addOnSuccessListener { usuarioSnapshot ->
                        val usuario = usuarioSnapshot.getValue(UsuarioFirebase::class.java)

                        if (usuario == null) {
                            onError("No se encontró el perfil del repartidor")
                            return@addOnSuccessListener
                        }

                        val codigoPedido = pedido.codigoPedido

                        val actualizaciones = hashMapOf<String, Any>(
                            "/pedidos/$codigoPedido/repartidorId" to uidRepartidor,
                            "/pedidos/$codigoPedido/repartidorNombre" to usuario.nombre,
                            "/pedidos/$codigoPedido/estadoPedido" to "Aceptado por repartidor",
                            "/pedidos/$codigoPedido/ultimaActualizacion" to
                                    "El repartidor ${usuario.nombre} aceptó la solicitud"
                        )

                        val notificacionId = database.child("notificaciones")
                            .child(pedido.clienteId)
                            .push()
                            .key ?: "notificacion_${System.currentTimeMillis()}"

                        actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/titulo"] =
                            "Solicitud aceptada"

                        actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/mensaje"] =
                            "Un repartidor aceptó tu solicitud ${pedido.codigoPedido}"

                        actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/codigoPedido"] =
                            pedido.codigoPedido

                        actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/estadoPedido"] =
                            "Aceptado por repartidor"

                        actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/leida"] =
                            false

                        actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/fecha"] =
                            System.currentTimeMillis()

                        database.updateChildren(actualizaciones)
                            .addOnSuccessListener {
                                onSuccess()
                            }
                            .addOnFailureListener { error ->
                                onError(error.message ?: "Error al aceptar solicitud")
                            }
                    }
                    .addOnFailureListener { error ->
                        onError(error.message ?: "Error al consultar repartidor")
                    }
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Error al consultar pedido")
            }
    }

    // Actualiza el estado real del pedido y crea notificación interna para el cliente
    fun actualizarEstadoPedido(
        codigoPedido: String,
        nuevoEstado: String,
        mensajeActualizacion: String,
        finalizado: Boolean = false,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (codigoPedido.isBlank()) {
            onError("Código de pedido vacío")
            return
        }

        database.child("pedidos")
            .child(codigoPedido)
            .get()
            .addOnSuccessListener { snapshot ->

                val pedido = snapshot.getValue(PedidoFirebase::class.java)

                if (pedido == null) {
                    onError("No se encontró el pedido")
                    return@addOnSuccessListener
                }

                val actualizaciones = hashMapOf<String, Any>(
                    "/pedidos/$codigoPedido/estadoPedido" to nuevoEstado,
                    "/pedidos/$codigoPedido/ultimaActualizacion" to mensajeActualizacion,
                    "/pedidos/$codigoPedido/finalizado" to finalizado
                )

                val notificacionId = database.child("notificaciones")
                    .child(pedido.clienteId)
                    .push()
                    .key ?: "notificacion_${System.currentTimeMillis()}"

                actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/titulo"] =
                    obtenerTituloNotificacion(nuevoEstado)

                actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/mensaje"] =
                    mensajeActualizacion

                actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/codigoPedido"] =
                    codigoPedido

                actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/estadoPedido"] =
                    nuevoEstado

                actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/leida"] =
                    false

                actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/fecha"] =
                    System.currentTimeMillis()

                database.updateChildren(actualizaciones)
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { error ->
                        onError(error.message ?: "Error al actualizar estado del pedido")
                    }
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Error al consultar pedido")
            }
    }

    // Actualiza la ubicación real del repartidor y notifica si está cerca de la entrega
    fun actualizarUbicacionRepartidor(
        codigoPedido: String,
        latitud: Double,
        longitud: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (codigoPedido.isBlank()) {
            onError("Código de pedido vacío")
            return
        }

        database.child("pedidos")
            .child(codigoPedido)
            .get()
            .addOnSuccessListener { snapshot ->

                val pedido = snapshot.getValue(PedidoFirebase::class.java)

                if (pedido == null) {
                    onError("No se encontró el pedido")
                    return@addOnSuccessListener
                }

                var estadoActual = if (DatosEntrega.estadoPedido.isBlank()) {
                    pedido.estadoPedido
                } else {
                    DatosEntrega.estadoPedido
                }

                var mensajeActualizacion = "Ubicación del repartidor actualizada"

                val actualizaciones = hashMapOf<String, Any>(
                    "/pedidos/$codigoPedido/latitud" to latitud,
                    "/pedidos/$codigoPedido/longitud" to longitud,
                    "/pedidos/$codigoPedido/estadoPedido" to estadoActual,
                    "/pedidos/$codigoPedido/ultimaActualizacion" to mensajeActualizacion,

                    "/entregas/$codigoPedido/seguimiento/codigoPedido" to codigoPedido,
                    "/entregas/$codigoPedido/seguimiento/latitud" to latitud,
                    "/entregas/$codigoPedido/seguimiento/longitud" to longitud,
                    "/entregas/$codigoPedido/seguimiento/estadoPedido" to estadoActual,
                    "/entregas/$codigoPedido/seguimiento/ultimaActualizacion" to mensajeActualizacion,
                    "/entregas/$codigoPedido/seguimiento/evidenciaRegistrada" to DatosEntrega.evidenciaRegistrada
                )

                val vaEnRutaEntrega =
                    estadoActual == "En ruta al punto de entrega" ||
                            estadoActual == "Cerca del punto de entrega"

                val tieneCoordenadasEntrega =
                    pedido.latitudEntrega != 0.0 &&
                            pedido.longitudEntrega != 0.0

                if (vaEnRutaEntrega && tieneCoordenadasEntrega) {
                    val resultadoDistancia = FloatArray(1)

                    Location.distanceBetween(
                        latitud,
                        longitud,
                        pedido.latitudEntrega,
                        pedido.longitudEntrega,
                        resultadoDistancia
                    )

                    val distanciaMetros = resultadoDistancia[0].toDouble()

                    actualizaciones["/pedidos/$codigoPedido/distanciaRestanteEntregaMetros"] =
                        distanciaMetros

                    if (distanciaMetros <= 500.0 && !pedido.notificacionCercaniaEnviada) {
                        estadoActual = "Cerca del punto de entrega"
                        mensajeActualizacion =
                            "El repartidor está próximo a llegar. Distancia aproximada: ${distanciaMetros.toInt()} metros"

                        actualizaciones["/pedidos/$codigoPedido/estadoPedido"] = estadoActual
                        actualizaciones["/pedidos/$codigoPedido/ultimaActualizacion"] = mensajeActualizacion
                        actualizaciones["/pedidos/$codigoPedido/notificacionCercaniaEnviada"] = true

                        actualizaciones["/entregas/$codigoPedido/seguimiento/estadoPedido"] = estadoActual
                        actualizaciones["/entregas/$codigoPedido/seguimiento/ultimaActualizacion"] =
                            mensajeActualizacion

                        val notificacionId = database.child("notificaciones")
                            .child(pedido.clienteId)
                            .push()
                            .key ?: "notificacion_${System.currentTimeMillis()}"

                        actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/titulo"] =
                            "Repartidor próximo a llegar"

                        actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/mensaje"] =
                            mensajeActualizacion

                        actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/codigoPedido"] =
                            codigoPedido

                        actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/estadoPedido"] =
                            estadoActual

                        actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/leida"] =
                            false

                        actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/fecha"] =
                            System.currentTimeMillis()
                    }
                }

                database.updateChildren(actualizaciones)
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { error ->
                        onError(error.message ?: "Error al actualizar ubicación")
                    }
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Error al consultar pedido")
            }
    }

    // Guarda la evidencia de recogida o entrega en el pedido real
    fun guardarEvidenciaPedido(
        codigoPedido: String,
        tipoEvidencia: String,
        urlEvidencia: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (codigoPedido.isBlank()) {
            onError("Código de pedido vacío")
            return
        }

        if (urlEvidencia.isBlank()) {
            onError("No se recibió la URL de la evidencia")
            return
        }

        database.child("pedidos")
            .child(codigoPedido)
            .get()
            .addOnSuccessListener { snapshot ->

                val pedido = snapshot.getValue(PedidoFirebase::class.java)

                if (pedido == null) {
                    onError("No se encontró el pedido")
                    return@addOnSuccessListener
                }

                val actualizaciones = hashMapOf<String, Any>()

                if (tipoEvidencia == "recogida") {
                    actualizaciones["/pedidos/$codigoPedido/evidenciaRecogidaRegistrada"] = true
                    actualizaciones["/pedidos/$codigoPedido/urlEvidenciaRecogida"] = urlEvidencia
                    actualizaciones["/pedidos/$codigoPedido/estadoPedido"] = "Paquete recogido"
                    actualizaciones["/pedidos/$codigoPedido/ultimaActualizacion"] =
                        "El repartidor ya recibió el paquete y registró evidencia"
                } else {
                    actualizaciones["/pedidos/$codigoPedido/evidenciaEntregaRegistrada"] = true
                    actualizaciones["/pedidos/$codigoPedido/urlEvidenciaEntrega"] = urlEvidencia
                    actualizaciones["/pedidos/$codigoPedido/evidenciaRegistrada"] = true
                    actualizaciones["/pedidos/$codigoPedido/rutaFotoEvidencia"] = urlEvidencia
                    actualizaciones["/pedidos/$codigoPedido/estadoPedido"] = "Entregado"
                    actualizaciones["/pedidos/$codigoPedido/ultimaActualizacion"] =
                        "El repartidor entregó el paquete y registró evidencia"
                }

                val estadoNotificacion = if (tipoEvidencia == "recogida") {
                    "Paquete recogido"
                } else {
                    "Entregado"
                }

                val mensajeNotificacion = if (tipoEvidencia == "recogida") {
                    "El repartidor ya tiene el paquete en su poder"
                } else {
                    "Tu paquete fue entregado correctamente"
                }

                val notificacionId = database.child("notificaciones")
                    .child(pedido.clienteId)
                    .push()
                    .key ?: "notificacion_${System.currentTimeMillis()}"

                actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/titulo"] =
                    if (tipoEvidencia == "recogida") "Paquete recogido" else "Paquete entregado"

                actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/mensaje"] =
                    mensajeNotificacion

                actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/codigoPedido"] =
                    codigoPedido

                actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/estadoPedido"] =
                    estadoNotificacion

                actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/leida"] =
                    false

                actualizaciones["/notificaciones/${pedido.clienteId}/$notificacionId/fecha"] =
                    System.currentTimeMillis()

                database.updateChildren(actualizaciones)
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { error ->
                        onError(error.message ?: "Error al guardar evidencia")
                    }
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Error al consultar pedido")
            }
    }

    // Devuelve títulos entendibles para las notificaciones internas
    private fun obtenerTituloNotificacion(estado: String): String {
        return when (estado) {
            "Aceptado por repartidor" -> "Solicitud aceptada"
            "En camino a recogida" -> "Repartidor en camino"
            "En punto de recogida" -> "Repartidor en punto de recogida"
            "Paquete recogido" -> "Paquete recogido"
            "En ruta al punto de entrega" -> "Repartidor en ruta"
            "Cerca del punto de entrega" -> "Repartidor próximo a llegar"
            "Entregado" -> "Paquete entregado"
            "Finalizado" -> "Solicitud finalizada"
            else -> "Actualización del pedido"
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
        DatosEntrega.latitud = pedido.latitud
        DatosEntrega.longitud = pedido.longitud
    }
}
package sv.edu.ues.entregatrack

object DatosEntrega {

    // Datos temporales del pedido de prueba
    var codigoPedido: String = "PED-001"
    var clientePedido: String = "Carlos Montano"
    var repartidorPedido: String = "Juan Pérez"
    var direccionPedido: String = "San Vicente, El Salvador"

    // Estado actual de la entrega
    var estadoPedido: String = "Pendiente de iniciar"

    // Coordenadas de prueba para simular ubicación GPS
    var latitud: Double = 13.6420
    var longitud: Double = -88.7853

    // Control temporal de evidencia fotográfica
    var evidenciaRegistrada: Boolean = false
    var rutaFotoEvidencia: String = ""

    // Texto de última actualización
    var ultimaActualizacion: String = "Sin actualización reciente"

    // Cambia el estado cuando el repartidor inicia la entrega
    fun iniciarEntrega() {
        estadoPedido = "En ruta"
        ultimaActualizacion = "Entrega iniciada por el repartidor"
    }

    // Simula una actualización de ubicación GPS
    fun actualizarUbicacion() {
        latitud += 0.0005
        longitud += 0.0005
        estadoPedido = "Repartidor en ruta"
        ultimaActualizacion = "Ubicación GPS actualizada"
    }

    // Marca que la evidencia fue registrada y guarda la ruta de la foto
    fun registrarEvidencia(rutaFoto: String = "") {
        evidenciaRegistrada = true

        // Guarda la ruta solo si viene una foto válida
        if (rutaFoto.isNotEmpty()) {
            rutaFotoEvidencia = rutaFoto
        }

        estadoPedido = "Evidencia registrada"
        ultimaActualizacion = "Fotografía de entrega registrada"
    }

    // Finaliza la entrega
    fun finalizarEntrega() {
        estadoPedido = "Entregado"
        ultimaActualizacion = "Pedido entregado correctamente"
    }
}
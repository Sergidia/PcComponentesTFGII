package ucm.tfg.pccomponentes.utilities

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import ucm.tfg.pccomponentes.notifications.MyFirebaseMessagingService
import ucm.tfg.pccomponentes.notifications.NotificationDocumentObject

object CheckData {

    /**
     * Se comprueba si el texto introducido es de tipo email
     */
    fun checkEmail(email: String): Boolean {
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return true;
        return false;
    }

    /**
     * Se comprueba si la contraseña cumple los requisitos mínimos
     */
    fun checkPassword(password: String): Boolean {
        if (password.length >= 6) return true;
        return false
    }

    /**
     * Actualizamos el registro del token de la base de datos
     */
    fun actualizarToken(email: String) {

        // Recuperamos el token del móvil del usuario
        val token: String = MyFirebaseMessagingService.getInstanceToken()

        // Obtenemos la instancia de Firebase
        val db = FirebaseFirestore.getInstance()

        // Recuperamos el documento del usuario
        val notificacionesUsuario: DocumentReference = db.collection("usuarios").document(email)

        // Convertimos el documento recuperado en un objeto de tipo NotificationDocumentObject
        notificacionesUsuario.get().addOnSuccessListener { documento ->
            val notificaciones = documento.toObject(NotificationDocumentObject::class.java)

            if (notificaciones != null && token != "") {

                // Si el token recuperado del documento no es igual al token actual lo actualizamos y subimos el documento a la base de datos
                if (notificaciones.getToken() != token) {

                    notificaciones.setToken(token)
                    notificacionesUsuario.set(notificaciones)
                }

            }
        }
    }
}
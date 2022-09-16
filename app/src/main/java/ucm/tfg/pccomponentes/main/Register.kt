package ucm.tfg.pccomponentes.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_register.*
import ucm.tfg.pccomponentes.Main
import ucm.tfg.pccomponentes.R
import ucm.tfg.pccomponentes.notifications.MyFirebaseMessagingService
import ucm.tfg.pccomponentes.notifications.NotificationDocumentObject
import ucm.tfg.pccomponentes.utilities.CheckData

class Register : AppCompatActivity()  {

    /**
     * Abrimos la ventana activity_register para permitir
     * al usuario registrarse introduciendo todos los datos
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        Thread.sleep(2000)
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        iniciarRegistro()
    }

    /**
     * Se configuran los botones y el título de la ventana. El botón de Registro intentará registrarse
     * con las datos proporcionados, llevándonos a activity_profile en caso de éxito o
     * mostrando un error si no se ha podido registrar el usuario
     */
    private fun iniciarRegistro() {

        title = "Registro"

        registerButton.setOnClickListener {
            if (userText.text.isNotEmpty() && passwordText.text.isNotEmpty()) {

                // Si el usuario no es de tipo email se muestra un error
                if (CheckData.checkEmail(userText.text.toString())) {

                    // Si la contraseña no cumple los requisitos mínimos se muestra un error
                    if (CheckData.checkPassword(passwordText.text.toString())) {

                        // Se usa el tipo de registro (ProviderType) BASIC, que es el de usuario + contraseña
                        // Habría otras opciones a configurar, como el inicio de sesión usando la cuenta de Google, de Facebook...
                        FirebaseAuth.getInstance()
                                .createUserWithEmailAndPassword(userText.text.toString(),
                                        passwordText.text.toString()).addOnCompleteListener {
                                    if (it.isSuccessful){

                                        // Si los datos son correctos se inicia una instancia en Firestore para almacenar las preferencias de notificación del usuario
                                        val db = FirebaseFirestore.getInstance()

                                        // Recuperamos el token del móvil del usuario
                                        val token: String = MyFirebaseMessagingService.getInstanceToken()

                                        // Creamos un documento para la colección de usuarios, que contendrá los datos del objeto Notificacion creado posteriormente
                                        val notificacionesUsuario: DocumentReference = db.collection("usuarios").document(userText.text.toString())

                                        // Creamos un objeto de tipo Notification, que contiene los booleanos de los dos tipos de notificaciones según la elección del usuario y el token del dispositivo móvil
                                        val notif: NotificationDocumentObject = NotificationDocumentObject(notifPush.isChecked, notifEmail.isChecked, token)

                                        // Insertamos el documento en la base de datos
                                        notificacionesUsuario.set(notif).addOnCompleteListener{
                                            if (it.isSuccessful){

                                                // Enviamos el email de verificación al correo del usuario
                                                FirebaseAuth.getInstance().currentUser?.sendEmailVerification()

                                                Toast.makeText(applicationContext,
                                                        "Debe verificar el registro desde el email enviado a su cuenta de correo", Toast.LENGTH_LONG).show()
                                                // Si el registro ha sido satisfactorio redirigimos a la vista del login
                                                showMain()
                                            } else {
                                                Log.d("Register", "Error: $it")
                                                showAlert("No se ha podido registrar el usuario, compruebe las opciones de notificación seleccionadas")
                                            }

                                        }

                                    } else {
                                        Log.d("Register", "Error: $it")
                                        showAlert("No se ha podido registrar el usuario, compruebe el usuario y la contraseña introducidos")
                                    }
                                }
                    } else showAlert("La contraseña debe tener mínimo 6 caracteres")
                } else showAlert("Se debe introducir una cuenta de correo electrónico")
            } else showAlert("Los campos de usuario y contraseña no pueden estar vacíos")
        }
    }

    /**
     * Redirección a la página de inicio de sesión
     */
    private fun showMain() {

        finish()
    }

    /**
     * Método para mostrar errores con una ventana emergente
     */
    private fun showAlert(mensajeError: String) {

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(mensajeError)
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
}
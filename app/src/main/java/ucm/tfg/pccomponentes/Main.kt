package ucm.tfg.pccomponentes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginBehavior
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.passwordText
import kotlinx.android.synthetic.main.activity_main.registerButton
import kotlinx.android.synthetic.main.activity_main.userText
import kotlinx.android.synthetic.main.activity_register.*
import ucm.tfg.pccomponentes.list.MainActivity
import ucm.tfg.pccomponentes.main.ProviderType
import ucm.tfg.pccomponentes.main.Register
import ucm.tfg.pccomponentes.notifications.MyFirebaseMessagingService
import ucm.tfg.pccomponentes.notifications.NotificationDocumentObject
import ucm.tfg.pccomponentes.utilities.CheckData

class Main : AppCompatActivity() {

    // Código de salida esperado para la autenticación exitosa de Google
    private val GOOGLE_SIGN_IN_CODE = 100;

    // CallbackManager para la autenticación con Facebook
    private val callBackManager = CallbackManager.Factory.create()

    /**
     * Al iniciar la aplicación abrimos la ventana activity_main para permitir
    al usuario iniciar sesión o registrarse
     */
    override fun onCreate(savedInstanceState: Bundle?) {

        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Comprobamos si el usuario está autenticado
        val email: String = FirebaseAuth.getInstance().currentUser?.email.toString()

        // Si lo está recuperamos el token y comprobamos si debemos actualizarlo. Luego redirigimos al listado de componentes
        if (email != "" && email != "null") {
            var provider = ProviderType.BASIC // Establecemos que provider va a tener el valor de inicio de sesión básico

            // Comprobamos el token y lo actualizamos de hacer falta
            CheckData.actualizarToken(email)

            // Comprobamos el tipo de proveedor, por si no fuera de tipo BASIC, ya que de no serlo no hace falta que comprobemos si verificó su registro
            val p = FirebaseAuth.getInstance().currentUser?.let{
                for (profile in it.providerData) {
                    when(profile.providerId){
                        GoogleAuthProvider.PROVIDER_ID -> {
                            provider = ProviderType.GOOGLE // Ahora que sabemos que es de Google, modificamos el valor
                        }
                        FacebookAuthProvider.PROVIDER_ID -> {
                            provider = ProviderType.FACEBOOK // Ahora que sabemos que es de Facebook, modificamos el valor
                        }
                    }
                }
            }

            // Independientemente de que hayamos o no actualizado el token, redirigimos a la vista del listado de componentes si verificó su registro o lo hizo mediante Google o Facebook
            if (FirebaseAuth.getInstance().currentUser?.isEmailVerified == true || provider == ProviderType.GOOGLE || provider == ProviderType.FACEBOOK)
                showList()
        }
        // Si no está autenticado o verificado generamos la vista del login
         else iniciarMain()
    }

    /**
     * Se configuran los botones y el título de la ventana. El botón de Registro nos llevará a activity_register
     * y el botón de Iniciar Sesión intentará iniciar sesión con las credenciales proporcionadas, llevándonos a
     * activity_list en caso de éxito o mostrando un error si no se ha podido iniciar sesión. El botón
     * Has olvidado tu contraseña enviará al usuario un email para poder recuperarla
     */
    private fun iniciarMain() {

        title = "Autenticación"

        /**
         * Este Listener implementa el click del botón 'Iniciar Sesión', que sólo sirve para iniciar sesión introduciendo correo electrónico
         * y contraseña
         */
        signInButton.setOnClickListener {

            // Si alguno de los datos está vacío se muestra un error
            if (userText.text.isNotEmpty() && passwordText.text.isNotEmpty()) {

                // Si el usuario no es de tipo email se muestra un error
                if (CheckData.checkEmail(userText.text.toString())) {

                    // Si la contraseña no cumple los requisitos mínimos se muestra un error
                    if (CheckData.checkPassword(passwordText.text.toString())) {

                        // Se usa el tipo de inicio de sesión (ProviderType) BASIC, que es el de usuario + contraseña
                        // Los otros tipos de inicio de sesión estarán en sus setOnClickListener particulares
                        FirebaseAuth.getInstance()
                                .signInWithEmailAndPassword(userText.text.toString(),
                                        passwordText.text.toString()).addOnCompleteListener {

                                    if (it.isSuccessful){

                                        // Comprobamos si hace falta actualizar el token almacenado previamente
                                        CheckData.actualizarToken(it.result?.user?.email ?: "")

                                        // Comprobamos si el usuario ha verificado su registro por email
                                        if (FirebaseAuth.getInstance().currentUser?.isEmailVerified == true) {

                                            // Si el usuario verificó su registro redirigimos al listado de componentes
                                            showList()
                                        }

                                        // Si no verificó su registro se muestra un pop up con un recordatorio y se le vuelve a enviar el email
                                        else {
                                            showAlert("No se ha podido iniciar sesión, debe confirmar el registro desde el email recibido en su cuenta de correo")
                                            FirebaseAuth.getInstance().currentUser?.sendEmailVerification()
                                        }

                                    } else {
                                        Log.d("Login", "Error: $it")
                                        showAlert("No se ha podido iniciar sesión, compruebe los datos introducidos")
                                    }
                                }
                    } else showAlert("La contraseña debe tener mínimo 6 caracteres")
                } else showAlert("Se debe introducir una cuenta de correo electrónico")
            } else showAlert("Los campos de usuario y contraseña no pueden estar vacíos")
        }

        /**
         * Este Listener implementa el botón 'Registrarse', que redirige a la ventana de registro mediante correo electrónico y contraseña
         */
        registerButton.setOnClickListener {
            showRegister()
        }

        /**
         * Este Listener implementa el click al texto '¿Has olvidado la contraseña?', que envía un email al usuario al correo que indique
         * en el campo 'Usuario (email)'
         */
        forgotPassword.setOnClickListener {
            if (userText.text.isNotEmpty()) {
                FirebaseAuth.getInstance().sendPasswordResetEmail(userText.text.toString())
                Toast.makeText(this, "Se ha enviado un email para reestablecer su contraseña", Toast.LENGTH_LONG).show()
            }
            else showAlert("Introduzca su email y pulse " + getString(R.string.forgotPassword_hint))

        }

        /**
         * Este Listener implementa el botón de 'Login mediante Google'
         */
        googleButton.setOnClickListener {

            // Creamos una constante de configuración para almacenar datos del inicio de sesión
            val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id_v2)) // Token asociado a nuestra app. Está almacenado en google-services.json
                .requestEmail()
                .build()

            // Creamos el cliente de autenticación de Google con la configuración anterior
            val googleClient = GoogleSignIn.getClient(this, googleConf)

            googleClient.signOut() // Este logout es por si tenemos más de una cuenta de Google asociada a nuestro dispositivo Android

            // Mostramos la pantalla de autenticación de Google. GOOGLE_SIGN_IN_CODE es un código que hemos implementado manualmente y que esperaremos
            // como salida del proceso de autenticación para confirmar su éxito. Si devuelve algo que no sea GOOGLE_SIGN_IN_CODE ha ocurrido un error
            startActivityForResult(googleClient.signInIntent, GOOGLE_SIGN_IN_CODE)
        }

        /**
         * Este Listener implementa el botón de 'Login mediante Facebook'
         */
        facebookButton.setOnClickListener {

            // Damos permisos de lectura para leer exclusivamente el correo electrónico del usuario
            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email"))

            LoginManager.getInstance().registerCallback(callBackManager,
            object : FacebookCallback<LoginResult> {

                // Implementamos el login con Firebase si la autenticación con Facebook ha sido correcta
                override fun onSuccess(result: LoginResult?) {

                    // Desempaquetamos el resultado si es distinto de nulo
                    result?.let {

                        // Recuperamos el token de autenticación de Facebook
                        val token = it.accessToken

                        // Y con ello obtenemos la credencial
                        val credential = FacebookAuthProvider.getCredential(token.token)

                        // Enviamos a Firebase esta credencial para iniciar sesión
                        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener {
                            if (it.isSuccessful){

                                // Si los datos son correctos se inicia una instancia en Firestore para almacenar las preferencias de notificación del usuario
                                val db = FirebaseFirestore.getInstance()

                                // Recuperamos el token del móvil del usuario (no es el mismo que el token de autenticación de Facebook ni el de la app)
                                val tokenMovil: String = MyFirebaseMessagingService.getInstanceToken()

                                // Creamos un documento para la colección de usuarios, que contendrá los datos del objeto Notificacion creado posteriormente
                                val notificacionesUsuario: DocumentReference = db.collection("usuarios").document(it.result.user?.email.toString())

                                // Creamos un objeto de tipo Notification, que contiene los booleanos de los dos tipos de notificaciones, forzado inicialmente a push para Facebook
                                val notif: NotificationDocumentObject = NotificationDocumentObject(true, false, tokenMovil)

                                // Insertamos el documento en la base de datos
                                notificacionesUsuario.set(notif).addOnCompleteListener {
                                    if (it.isSuccessful) {
                                        // Mostramos la ventana del listado de componentes
                                        showList()
                                    } else {
                                        Log.d("Register", "Error: $it")
                                        showAlert("No se ha podido registrar el usuario por un error al almacenar las opciones de notificación predeterminadas")
                                    }
                                }
                            } else {
                                Log.d("Login", "Error: $it")
                                showAlert("No se ha podido iniciar sesión, compruebe los datos introducidos")
                            }
                        }
                    }
                }

                // Si se cancela la autenticación no hacemos nada
                override fun onCancel() {

                }

                // Si ha habido un error devolvemos un mensaje
                override fun onError(error: FacebookException?) {
                    showAlert("No se ha podido iniciar sesión, compruebe los datos introducidos")
                }
            })
        }
    }

    /**
     * Redirección a la página de registro
     */
    private fun showRegister() {

        val signUpIntent = Intent(this, Register::class.java)
        startActivity(signUpIntent)
    }

    /**
     * Redirección a la página del listado de componentes
     */
    private fun showList() {

        val listIntent = Intent(this, MainActivity::class.java)
        startActivity(listIntent)
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

    /**
     * onActivityResult para controlar los login vía Google y Facebook
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        // Hay que llamar al callBackManager para comprobar si se ha hecho login con Facebook y desencadenar una de las llamadas "on"
        callBackManager.onActivityResult(requestCode, resultCode, data)

        super.onActivityResult(requestCode, resultCode, data)

        // Si la respuesta de esta Activity es igual a GOOGLE_SIGN_IN_CODE significa que estamos intentando el login mediante Google
        if (requestCode == GOOGLE_SIGN_IN_CODE) {

            // Recuperamos la respuesta
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                // Recuperamos la cuenta de Google accediendo al Result de esa respuesta
                val account = task.getResult(ApiException::class.java)

                // Si la cuenta no está vacía es que la hemos recuperado correctamente
                if(account != null) {

                    // Recuperamos la credencial de la cuenta
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                    // Enviamos a Firebase esta credencial para iniciar sesión
                    FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener {
                        if (it.isSuccessful){

                            // Si los datos son correctos se inicia una instancia en Firestore para almacenar las preferencias de notificación del usuario
                            val db = FirebaseFirestore.getInstance()

                            // Recuperamos el token del móvil del usuario
                            val token: String = MyFirebaseMessagingService.getInstanceToken()

                            // Creamos un documento para la colección de usuarios, que contendrá los datos del objeto Notificacion creado posteriormente
                            val notificacionesUsuario: DocumentReference = db.collection("usuarios").document(it.result.user?.email.toString())

                            // Creamos un objeto de tipo Notification, que contiene los booleanos de los dos tipos de notificaciones, forzado inicialmente a push para Google
                            val notif: NotificationDocumentObject = NotificationDocumentObject(true, false, token)

                            // Insertamos el documento en la base de datos
                            notificacionesUsuario.set(notif).addOnCompleteListener {
                                if (it.isSuccessful) {
                                    // Mostramos la ventana del listado de componentes
                                    showList()
                                } else {
                                    Log.d("Register", "Error: $it")
                                    showAlert("No se ha podido registrar el usuario por un error al almacenar las opciones de notificación predeterminadas")
                                }
                            }
                        } else {
                            Log.d("Login", "Error: $it")
                            showAlert("No se ha podido iniciar sesión, compruebe los datos introducidos")
                        }
                    }
                }

            } catch(e: ApiException) {
                Log.d("Login", "Error: $e")
                showAlert("No se ha podido recuperar la cuenta via API. Si el problema persiste intente otro tipo de inicio de sesión")
            }
        }
    }
}
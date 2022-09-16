package ucm.tfg.pccomponentes

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ucm.tfg.pccomponentes.list.MainActivity

class LaunchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Se redirige al usuario a la ventana principal en cuanto termina de cargar
        val intent = Intent(this, Main::class.java)
        startActivity(intent)

        finish()
    }
}
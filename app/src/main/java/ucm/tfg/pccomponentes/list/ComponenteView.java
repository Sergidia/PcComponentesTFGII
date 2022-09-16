package ucm.tfg.pccomponentes.list;

import ucm.tfg.pccomponentes.Main;
import ucm.tfg.pccomponentes.R;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

public class ComponenteView extends AppCompatActivity {

    private String email;

    private Item item;
    private boolean seguido;

    private CheckBox seguir;
    private EditText precioNoti;

    private Interes seguimiento;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle("Ficha del componente");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_componente_view);

        try {
            email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

            // Se comprueba que el usuario está correctamente autenticado, de lo contrario se vuelve a la vista del login
            if (email == null || email.equals("") || email.equals("null")) {
                showAlert("Error de autenticación");
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), Main.class));

            } else {
                mostrarArticulo();
            }
        }
        catch (Exception e) {
                showAlert("Error interno");
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), Main.class));
            }
    }

    /**
     * Se muestra la ficha de un artículo, incluidos los campo de precio relleno y el checkbox activado si el componente está seguido por el usuario
     */
    public void mostrarArticulo() {

        TextView nombre = findViewById(R.id.nombreComponente);
        TextView precio = findViewById(R.id.precioComponente);
        TextView componente = findViewById(R.id.tipoComponente);
        Button detalles = findViewById(R.id.buttonNavDetails);
        ImageView imagen = findViewById(R.id.imagenComponente);

        seguir = findViewById(R.id.checkBoxComponente);
        precioNoti = findViewById(R.id.precioNoti);
        Button btnActualiza = findViewById(R.id.btnActualizar);

        db = FirebaseFirestore.getInstance();
        Bundle recepcion = getIntent().getExtras();

        if(recepcion != null){

            item = (Item) recepcion.getSerializable("componente");
            nombre.setText(item.getNombre());
            precio.setText(String.valueOf(item.getPrecio()));
            componente.setText(item.getCategoria());
            detalles.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Uri uri = Uri.parse(item.getUrl());
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(uri);

                    Intent chooser = Intent.createChooser(intent, "Open with");

                    v.getContext().startActivity(chooser);
                }
            });
            Picasso.get()
                    .load(item.getImagen())
                    .resize(300,300)
                    .into(imagen);

            seguido = false;
            seguimiento = new Interes(item.getCodigo(), (double) 0);

            try {
                db.collection("usuarios").document(email).collection("interes")
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {

                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        if(document.getId().equals(item.getCodigo())){

                                            Map<String,Object> interes;
                                            interes = document.getData();
                                            Double precioD = (Double) interes.get("precio");
                                            seguimiento = new Interes(document.getId(), precioD);

                                            seguido = true;
                                            seguir.setChecked(true);

                                            //LinearLayout layout = findViewById(R.id.linearLayoutComponentView);
                                            //layout.setBackgroundResource(R.drawable.component_interest_rounded_border);

                                            precioNoti.setText(String.valueOf(seguimiento.getPrecioMax()));
                                        }

                                    }
                                } else {
                                    Log.d("Componente", "Error al obtener el documento: ", task.getException());
                                }
                            }
                        });

            } catch (NumberFormatException nE) {
                showAlert("Debe introducir un número en el campo del precio");
            }
        }

        btnActualiza.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actualizaNotificacion();
            }
        });
    }

    /**
     * Se actualiza el seguimiento del artículo
     */
    public void actualizaNotificacion(){

        if(seguir.isChecked()){

            if (precioNoti.getText().toString().isEmpty()) {
                showAlert("Debe indicar el precio a partir del cual ser notificado para seguir un componente");
            }
            else {
                try {

                    // Si no estaba seguido se crea un seguimiento y si lo estaba pero el precio de seguimiento ha cambiado se actualiza
                    if(!seguido || seguimiento.getPrecioMax() != Double.parseDouble(precioNoti.getText().toString())){

                        Map<String,Object> aux = new HashMap<>();
                        aux.put("precio",Double.parseDouble(precioNoti.getText().toString()));

                        db.collection("usuarios").document(email)
                                .collection("interes").document(seguimiento.getCodigo())
                                .set(aux)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        seguimiento.setPrecioMax(Double.parseDouble(precioNoti.getText().toString()));
                                        if(!seguido){
                                            seguido = true;
                                            Toast.makeText(getApplicationContext(),"Ahora sigues este componente",Toast.LENGTH_SHORT).show();
                                        }
                                        else{
                                            Toast.makeText(getApplicationContext(),"Precio de seguimiento actualizado",Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        showAlert("Error al seguir el componente");
                                    }
                                });
                    }
                }
                catch (NumberFormatException nE) {
                    showAlert("Debe introducir un número en el campo del precio");
                }
            }
        }
        else{

            // Si ya estaba seguido se deja de seguir
            if(seguido){
                db.collection("usuarios").document(email)
                        .collection("interes").document(seguimiento.getCodigo())
                        .delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                seguido = false;
                                Toast.makeText(getApplicationContext(),"Ya no sigues este componente",Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                showAlert("Error al dejar de seguir el componente");
                            }
                        });
            }
        }
    }

    /*
        Método para mostrar errores con una ventana emergente
    */
    private void showAlert(String mensajeError) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error");
        builder.setMessage(mensajeError);
        builder.setPositiveButton("Aceptar", null);
        AlertDialog dialog  = builder.create();
        dialog.show();
    }
}
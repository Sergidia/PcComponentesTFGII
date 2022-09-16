package ucm.tfg.pccomponentes.list;

import ucm.tfg.pccomponentes.Main;
import ucm.tfg.pccomponentes.R;
import ucm.tfg.pccomponentes.main.Profile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;


public class MainActivity extends AppCompatActivity  implements SearchView.OnQueryTextListener{

    private RecyclerView recycler;
    private ArrayList<Item> listadoComponentes;
    String filtro = "";

    private Adapter rva;
    private boolean isLoading;

    private FirebaseFirestore db;
    private DocumentSnapshot lastVisible;

    // Menú inferior
    private BottomNavigationView bottomNavigationView;

    // Navigation Drawer
    private NavigationView navigationView;

    // Modo de búsqueda para saber cómo ampliar la lista de componentes en scroll
    /**
     * 0 = estándar
     * 1 = nombre AZ
     * 2 = nombre ZA
     * 3 = categoria AZ
     * 4 = categoria ZA
     * 5 = precio mayor
     * 6 = precio menor
     */
    private int modoBusqueda = 0;

    // Switch de buscado por nombre/categoría del menú superior. False = nombre, True = categoría
    private Switch switchBusqueda;
    // Este booleano es para evitar que una vez se filtre por nombre, se cambie el switch y se haga scroll, lo que ampliaría la lista
    // buscando por categoría si sólo usáramos el switchBusqueda.isChecked(). Las comprobaciones se harán con este booleano que sólo se actualizará
    // al hacer click al "Enter" para confirmar el texto introducido (lo que reinicia la lista). False = nombre, True = categoría
    private boolean modoSwitchBusqueda = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTitle("Listado de componentes");
        super.onCreate(savedInstanceState);

        try {
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

            // Se comprueba que el usuario está correctamente autenticado, de lo contrario se vuelve a la vista del login
            if (email == null || email.equals("") || email.equals("null")) {
                showAlert("Error de autenticación");
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), Main.class));
            }

            else {
                setContentView(R.layout.nav_list_activity); // Esto abre el Navigation Drawer también. Si no se quiere -> activity_list

                // Generamos el listener para el Recycler
                recycler = findViewById(R.id.listItem);
                addListenerRecycler();

                // Generamos el listener para el menú inferior
                bottomNavigationView = findViewById(R.id.bottom_navigation);
                bottomNavigationView.setSelectedItemId(R.id.principal);
                setListenerBottomMenu();

                LinearLayoutManager llm = new LinearLayoutManager(this);
                llm.setOrientation(LinearLayoutManager.VERTICAL);
                recycler.setLayoutManager(llm);

                recycler.setHasFixedSize(true);

                listadoComponentes = new ArrayList<>();

                db = FirebaseFirestore.getInstance();

                // Generamos el listener para los botones del Navigation Drawer
                navigationView = findViewById(R.id.nav_view);
                navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                        Intent i;

                        switch (item.getItemId()) {

                            case R.id.mainActivity:
                                // Si vuelve a pulsar listado de componentes se reinicia la ventana sin filtros
                                modoBusqueda = 0;
                                recreate();
                                overridePendingTransition(0,0);
                                return true;

                            case R.id.nombreAZ:
                                modoBusqueda = 1;
                                listadoComponentes.clear();
                                iniciarListaComponentesNombreAZ();
                                return true;

                            case R.id.nombreZA:
                                modoBusqueda = 2;
                                listadoComponentes.clear();
                                iniciarListaComponentesNombreZA();
                                return true;

                            case R.id.categoriaAZ:
                                modoBusqueda = 3;
                                listadoComponentes.clear();
                                iniciarListaComponentesCategoriaAZ();
                                return true;

                            case R.id.categoriaZA:
                                modoBusqueda = 4;
                                listadoComponentes.clear();
                                iniciarListaComponentesCategoriaZA();
                                return true;

                            case R.id.precioMayor:
                                modoBusqueda = 5;
                                listadoComponentes.clear();
                                iniciarListaComponentesPrecioMayor();
                                return true;

                            case R.id.precioMenor:
                                modoBusqueda = 6;
                                listadoComponentes.clear();
                                iniciarListaComponentesPrecioMenor();
                                return true;

                            case R.id.seguidosView:
                                i = new Intent(getApplicationContext(), SeguidosView.class);
                                startActivity(i);
                                overridePendingTransition(0,0);
                                return true;

                            case R.id.profile:
                                i = new Intent(getApplicationContext(), Profile.class);
                                startActivity(i);
                                finish();
                                overridePendingTransition(0,0);
                                return true;

                            case R.id.cerrarSesion:
                                FirebaseAuth.getInstance().signOut();
                                i = new Intent(getApplicationContext(), Main.class);
                                startActivity(i);
                                finish();
                                overridePendingTransition(0,0);
                                return true;

                            default: return false;
                        }
                    }
                });

                // Escribimos el email en el Navigation Drawer
                View headerView = navigationView.getHeaderView(0);
                TextView navUsername = (TextView) headerView.findViewById(R.id.navigationDrawerEmail);
                navUsername.setText(email);

                iniciarListaComponentes();
            }
        }
        catch (Exception e) {
            showAlert("Error interno");
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getApplicationContext(), Main.class));
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // ----------------------------------------- INICIO LISTA COMPONENTES -----------------------------------------
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Inicialización de la lista de componentes recuperando los 10 primeros de la base de datos
     */
    private void iniciarListaComponentes() {

        Log.d("Listado Main", "Se inicializa la lista de componentes");

        db.collection("componentes")
                .limit(10)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {

                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                            addComponente(document);
                        }
                        cargarRecyclerView();
                        isLoading = false;
                    }
                });
    }

    // ------------------------------------------------------------------------------------------------------------
    // -------------------------------------- INICIO LISTA NAVIGATION DRAWER --------------------------------------
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Inicialización de la lista de componentes recuperando los 10 primeros de la base de datos ordenándolos por nombre de A a Z
     */
    private void iniciarListaComponentesNombreAZ() {

        Log.d("Listado Main", "Se inicializa la lista de componentes por nombre AZ");

        db.collection("componentes")
                .orderBy("nombre", Query.Direction.ASCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {

                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                            addComponente(document);
                        }
                        cargarRecyclerView();
                        isLoading = false;
                    }
                });
    }

    /**
     * Inicialización de la lista de componentes recuperando los 10 primeros de la base de datos ordenándolos por nombre de Z a A
     */
    private void iniciarListaComponentesNombreZA() {

        Log.d("Listado Main", "Se inicializa la lista de componentes por nombre ZA");

        db.collection("componentes")
                .orderBy("nombre", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {

                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                            addComponente(document);
                        }
                        cargarRecyclerView();
                        isLoading = false;
                    }
                });
    }

    /**
     * Inicialización de la lista de componentes recuperando los 10 primeros de la base de datos ordenándolos por categoría de A a Z
     */
    private void iniciarListaComponentesCategoriaAZ() {

        Log.d("Listado Main", "Se inicializa la lista de componentes por categoría AZ");

        db.collection("componentes")
                .orderBy("categoria", Query.Direction.ASCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {

                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                            addComponente(document);
                        }
                        cargarRecyclerView();
                        isLoading = false;
                    }
                });
    }

    /**
     * Inicialización de la lista de componentes recuperando los 10 primeros de la base de datos ordenándolos por categoría de Z a A
     */
    private void iniciarListaComponentesCategoriaZA() {

        Log.d("Listado Main", "Se inicializa la lista de componentes por categoría ZA");

        db.collection("componentes")
                .orderBy("categoria", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {

                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                            addComponente(document);
                        }
                        cargarRecyclerView();
                        isLoading = false;
                    }
                });
    }

    /**
     * Inicialización de la lista de componentes recuperando los 10 primeros de la base de datos con el precio más alto
     */
    private void iniciarListaComponentesPrecioMayor() {

        Log.d("Listado Main", "Se inicializa la lista de componentes por precio más alto");

        db.collection("componentes")
                .orderBy("precio", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {

                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                            addComponente(document);
                        }
                        cargarRecyclerView();
                        isLoading = false;
                    }
                });
    }

    /**
     * Inicialización de la lista de componentes recuperando los 10 primeros de la base de datos con el precio más bajo
     */
    private void iniciarListaComponentesPrecioMenor() {

        Log.d("Listado Main", "Se inicializa la lista de componentes por precio más bajo");

        db.collection("componentes")
                .orderBy("precio", Query.Direction.ASCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {

                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                            addComponente(document);
                        }
                        cargarRecyclerView();
                        isLoading = false;
                    }
                });
    }

    // ------------------------------------------------------------------------------------------------------------
    // --------------------------------------- AMPLIACIÓN LISTA COMPONENTES ---------------------------------------
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Ampliación de la lista de componentes que se produce cuando el usuario hace scroll y llega al final de la lista actual. Se cargan 10 nuevos componentes a partir del
     * último cargado anteriormente (lastVisible). También se tiene en cuenta si se ha aplicado un filtro antes de hacer scroll
     */
    private void ampliarListaComponentes() {

        Log.d("Listado Main", "Final del scroll, número de componentes actuales cargados: " + listadoComponentes.size());

        Query docs;

        if (filtro.equals("")) {

            // Si no hay filtro escrito se amplía el listado dependiendo del tipo de ordenación seleccionado
            switch (modoBusqueda) {
                // Estándar
                case 0: {
                    docs = db.collection("componentes")
                            .startAfter(lastVisible)
                            .limit(10);
                    break;
                }
                // Nombre AZ
                case 1: {
                    docs = db.collection("componentes")
                            .orderBy("nombre", Query.Direction.ASCENDING)
                            .startAfter(lastVisible)
                            .limit(10);
                    break;
                }
                // Nombre ZA
                case 2: {
                    docs = db.collection("componentes")
                            .orderBy("nombre", Query.Direction.DESCENDING)
                            .startAfter(lastVisible)
                            .limit(10);
                    break;
                }
                // Categoría AZ
                case 3: {
                    docs = db.collection("componentes")
                            .orderBy("categoria", Query.Direction.ASCENDING)
                            .startAfter(lastVisible)
                            .limit(10);
                    break;
                }
                // Categoría ZA
                case 4: {
                    docs = db.collection("componentes")
                            .orderBy("categoria", Query.Direction.DESCENDING)
                            .startAfter(lastVisible)
                            .limit(10);
                    break;
                }
                // Precio mayor
                case 5: {
                    docs = db.collection("componentes")
                            .orderBy("precio", Query.Direction.DESCENDING)
                            .startAfter(lastVisible)
                            .limit(10);
                    break;
                }
                // Precio menor
                case 6: {
                    docs = db.collection("componentes")
                            .orderBy("precio", Query.Direction.ASCENDING)
                            .startAfter(lastVisible)
                            .limit(10);
                    break;
                }
                // Se trata como estándar pero nunca debería llegar a este caso
                default : {
                    docs = db.collection("componentes")
                            .startAfter(lastVisible)
                            .limit(10);
                    break;
                }
            }
        }
        // Si hay filtro escrito se comprueba el estado del switch
        else {

            // Si no se ha seleccionado el check se busca por nombre
            if (!modoSwitchBusqueda) {
                docs = db.collection("componentes")
                        // Firebase no admite la búsqueda por campos (Contains) https://firebase.google.com/docs/firestore/solutions/search, devolviendo
                        // .whereGreaterThanOrEqualTo("nombre", filtro) consultas incorrectas. La mejor solución es permitir filtros de "empieza con",
                        // añadiendo un wildcard "\uf8ff" en el "termina con"
                        .orderBy("nombre")
                        .startAt(filtro)
                        .endAt(filtro + "\uf8ff")
                        .startAfter(lastVisible)
                        .limit(10);
            }
            // Si se ha seleccionado se busca por categoría
            else {
                docs = db.collection("componentes")
                        // Firebase no admite la búsqueda por campos (Contains) https://firebase.google.com/docs/firestore/solutions/search, devolviendo
                        // .whereGreaterThanOrEqualTo("categoria", filtro) consultas incorrectas. La mejor solución es permitir filtros de "empieza con",
                        // añadiendo un wildcard "\uf8ff" en el "termina con"
                        .orderBy("categoria")
                        .startAt(filtro)
                        .endAt(filtro + "\uf8ff")
                        .startAfter(lastVisible)
                        .limit(10);
            }
        }

        docs.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {

                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                            addComponente(document);
                        }
                        rva.notifyDataSetChanged();
                        isLoading = false;
                    }
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // ----------------------------------------- FILTRO LISTA COMPONENTES -----------------------------------------
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Recuperación de los 10 primeros componentes según el filtro del nombre escrito por el usuario. Este filtro por búsqueda
     * manual reinicia los otros filtros del Navigation Drawer
     */
    private void filtrarListaComponentes() {

        Log.d("Filtro Main", "Se solicita recuperar de la base de datos los componentes con filtro: " + filtro);

        // Si no se ha seleccionado el check se busca por nombre
        if (!modoSwitchBusqueda) {
            db.collection("componentes")
                    // Firebase no admite la búsqueda por campos (Contains) https://firebase.google.com/docs/firestore/solutions/search, devolviendo
                    // .whereGreaterThanOrEqualTo("nombre", filtro) consultas incorrectas. La mejor solución es permitir filtros de "empieza con",
                    // añadiendo un wildcard "\uf8ff" en el "termina con"
                    .orderBy("nombre")
                    .startAt(filtro)
                    .endAt(filtro + "\uf8ff")
                    .limit(10)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {

                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                                addComponente(document);
                            }
                            cargarRecyclerView();
                            isLoading = false;
                        }
                    });
        }
        // Si se ha seleccionado se busca por categoría
        else {
            db.collection("componentes")
                    // Firebase no admite la búsqueda por campos (Contains) https://firebase.google.com/docs/firestore/solutions/search, devolviendo
                    // .whereGreaterThanOrEqualTo("categoria", filtro) consultas incorrectas. La mejor solución es permitir filtros de "empieza con",
                    // añadiendo un wildcard "\uf8ff" en el "termina con"
                    .orderBy("categoria")
                    .startAt(filtro)
                    .endAt(filtro + "\uf8ff")
                    .limit(10)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {

                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                                addComponente(document);
                            }
                            cargarRecyclerView();
                            isLoading = false;
                        }
                    });
        }
    }

    /**
     * Filtra el listado de componentes con el texto escrito cuando el usuario pulsa en la lupa
     *
     * Nota: onQueryTextChange y onQueryTextSubmit tienen un comportamiento indeseado cuando se usa el teclado físico y no
     * el del móvil (sea el del emulador o un móvil real), ejecutando dos veces el método. Sólo ocurre cuando se pulsa ENTER
     * para ejecutar la consulta, pero no ocurre si se escribe con el teclado físico pero se pulsa con el ratón el icono de la lupa
     *
     * @param query filtro escrito por el usuario
     * @return true
     */
    @Override
    public boolean onQueryTextSubmit(String query) {

        listadoComponentes.clear();
        rva.notifyDataSetChanged();
        this.filtro = query;

        // Si el switch está desactivado (false) se busca por nombre, si está activo (true) por categoría
        if (!switchBusqueda.isChecked())
            modoSwitchBusqueda = false;
        else
            modoSwitchBusqueda = true;

        // Se reinicia el modo búsqueda del Navigation Drawer
        modoBusqueda = 0;

        if(query.equals(""))
            iniciarListaComponentes();

        else
            filtrarListaComponentes();

        Log.d("Listado Main", "Aplicado filtro: " + query);

        return true;
    }

    /**
     * Filtra el listado de componentes con cada letra que se escriba en el formulario de búsqueda. Sólo lo usamos
     * cuando se vacía la caja de texto para evitar lecturas múltiples en la base de datos
     *
     * Nota: onQueryTextChange y onQueryTextSubmit tienen un comportamiento indeseado cuando se usa el teclado físico y no
     * el del móvil (sea el del emulador o un móvil real), ejecutando dos veces el método. Sólo ocurre cuando se pulsa ENTER
     * para ejecutar la consulta, pero no ocurre si se escribe con el teclado físico pero se pulsa con el ratón el icono de la lupa
     *
     * @param newText filtro escrito por el usuario
     * @return true
     */
    @Override
    public boolean onQueryTextChange(String newText) {

        if(newText.equals("")) {
            listadoComponentes.clear();
            rva.notifyDataSetChanged();
            this.filtro = newText;

            iniciarListaComponentes();
        }

        return true;

    }

    // ------------------------------------------------------------------------------------------------------------
    // -------------------------------------------- AÑADIR COMPONENTES --------------------------------------------
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Añade un componente a la lista de componentes
     *
     * @param document documento del componente
     */
    public void addComponente(QueryDocumentSnapshot document) {

        Log.d("Componente Main", "Se añade a la lista el documento con id: " + document.getId());

        Map<String,Object> componente = document.getData();
        Item component = new Item(
                document.getId(),
                (String)componente.get("nombre"),
                (String)componente.get("img"),
                ((Number)componente.get("precio")).doubleValue(),
                (String)componente.get("url"),
                (String)componente.get("categoria"),
                (Boolean)componente.get("valida"));

        listadoComponentes.add(component);
        setLastVisible(document);

        Log.d("Componente Main", "El documento se corresponde con el componente con nombre: " + componente.get("nombre"));
    }

    /**
     * Crea las fichas de los componentes y redirecciona a la vista ComponentView cuando se selecciona una de ellas
     */
    private void cargarRecyclerView(){

        this.rva= new Adapter(listadoComponentes);
        rva.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent componenteView = new Intent(MainActivity.this, ComponenteView.class);
                Bundle miBundle = new Bundle();
                miBundle.putSerializable("componente", listadoComponentes.get(recycler.getChildAdapterPosition(v)));
                componenteView.putExtras(miBundle);

                MainActivity.this.startActivity(componenteView);
            }
        });

        recycler.setAdapter(rva);
    }

    /**
     * Listener del recycler referente a los scrolls
     */
    private void addListenerRecycler() {

        /*
         * Creación del listener para el scroll
         */
        this.recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            /**
             * Se comprueba si el usuario ha llegado al final del listado haciendo scroll
             *
             * @param recyclerView recyclerView
             * @param dx eje x
             * @param dy eje y
             */
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (!isLoading) {
                    if (!recyclerView.canScrollVertically(1)) {

                        //Final del listado, se llama al método que carga 10 más
                        ampliarListaComponentes();
                        isLoading = true;
                    }
                }
            }
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // --------------------------------------------------- MENÚ ---------------------------------------------------
    // ------------------------------------------------------------------------------------------------------------


    /**
     * Menú superior, con el filtro de componentes y la hamburguesa para la redirección al perfil y el cierre de sesión
     *
     * @param menu menu de la aplicación
     * @return true
     */
    public boolean onCreateOptionsMenu(Menu menu){

        getMenuInflater().inflate(R.menu.buscador, menu);
        MenuItem mi = menu.findItem(R.id.buscador);
        SearchView sv = (SearchView) mi.getActionView();
        sv.setOnQueryTextListener(this);

        mi.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) { return true; }
        });

        // Switch de búsqueda por nombre/categoría

        MenuItem si = menu.findItem(R.id.app_bar_switch);
        si.setActionView(R.layout.switch_item);

        switchBusqueda = (Switch) menu.findItem(R.id.app_bar_switch).getActionView().findViewById(R.id.switchMenuTop);

        switchBusqueda.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(MainActivity.this, "Búsqueda en lupa por nombre de componente", Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(MainActivity.this, "Búsqueda en lupa por categoría de componente", Toast.LENGTH_LONG).show();
                }
            }
        });

        return true;
    }

    /**
     * Opciones de la hamburguesa: se redirecciona al perfil o se cierra sesión
     *
     * @param item menuItem
     * @return opcion elegida
     */
    public boolean onOptionsItemSelected(MenuItem item){

        int id = item.getItemId();
        Intent i;

        switch (id){

            // Para el caso de cierre de sesión se añade un flag que cierra todas las ventanas abiertas anteriormente
            case R.id.opCerrarSesion:
                FirebaseAuth.getInstance().signOut();
                i = new Intent(getApplicationContext(), Main.class);
                startActivity(i);
                finish();
                overridePendingTransition(0,0);
                break;

            case R.id.op_perfil:
                i = new Intent(getApplicationContext(), Profile.class);
                startActivity(i);
                overridePendingTransition(0,0);
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Menú inferior con la redirección a la lista de seguidos o al listado general de componentes (en este último caso se refresca la vista)
     */
    private void setListenerBottomMenu() {

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId() == R.id.seguidos){

                    Intent i = new Intent(getApplicationContext(), SeguidosView.class);
                    startActivity(i);
                    overridePendingTransition(0,0);

                    return true;
                }
                else if(item.getItemId() == R.id.principal){

                    recreate();
                    overridePendingTransition(0,0);

                    return true;
                }
                return false;
            }
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // --------------------------------------------------- MISC ---------------------------------------------------
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Método que almacena el último documento cargado en la lista, de esta forma cuando se llega al final del scroll cargamos 10 nuevos componentes desde el último de ellos.
     * Este método nos permite almacenar el documento desde dentro de los Listener
     *
     * @param last Documento Firebase
     */
    private void setLastVisible(DocumentSnapshot last) {
        this.lastVisible = last;
    }

    /**
     * Método para mostrar errores con una ventana emergente
     *
     * @param mensajeError texto a mostrar
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

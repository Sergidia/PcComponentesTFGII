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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class SeguidosView extends AppCompatActivity implements SearchView.OnQueryTextListener {

    // Variables del Recycler y de los filtros
    private RecyclerView recycler;
    private ArrayList<Item> listadoComponentes;
    String filtro = "";

    private Adapter rva;
    private boolean isLoading;

    // Variables de Firebase para la lista de seguimiento
    private FirebaseFirestore db;
    private String email;
    private DocumentSnapshot lastVisibleSeguido;

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
        setTitle("Listado de seguimiento");
        super.onCreate(savedInstanceState);

        try {
            // Se recupera el email del usuario
            email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

            // Se comprueba que el usuario está correctamente autenticado, de lo contrario se vuelve a la vista del login
            if (email == null || email.equals("") || email.equals("null")) {
                showAlert("Error de autenticación");
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), Main.class));

            } else {
                setContentView(R.layout.nav_seguimiento_activity); // Esto abre el Navigation Drawer también. Si no se quiere -> activity_seguidos_view

                // Generamos el listener para el Recycler
                recycler = findViewById(R.id.listSeguidos);
                addListenerRecycler();

                // Generamos el listener para el menú inferior
                bottomNavigationView = findViewById(R.id.bottom_navigation);
                bottomNavigationView.setSelectedItemId(R.id.seguidos);
                setListenerBottomMenu();

                LinearLayoutManager llm = new LinearLayoutManager(this);
                llm.setOrientation(LinearLayoutManager.VERTICAL);
                recycler.setLayoutManager(llm);

                recycler.setHasFixedSize(true);
                listadoComponentes = new ArrayList<>();

                db = FirebaseFirestore.getInstance();
                isLoading = true;

                // Generamos el listener para los botones del Navigation Drawer
                navigationView = findViewById(R.id.nav_view);
                navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                        Intent i;

                        switch (item.getItemId()) {

                            case R.id.mainActivity:
                                // No se crea un intent nuevo del tipo 'MainActivity' porque el listado general de componentes siempre se abrirá al iniciar la aplicación,
                                // por lo que cerrando el actual (Seguidos) volveríamos a dicha vista siempre, evitando lecturas innecesarias a la base de datos y manteniendo
                                // al usuario en el lugar del listado donde lo dejó. De querer refrescar la vista lo podría hacer pulsando 'Principal' desde MainActivity
                                finish();
                                overridePendingTransition(0,0);
                                return true;

                            case R.id.seguidosView:
                                // Si vuelve a pulsar listado de seguimiento se reinicia la ventana sin filtros
                                modoBusqueda = 0;
                                recreate();
                                overridePendingTransition(0,0);
                                return true;

                            case R.id.nombreAZ:
                                modoBusqueda = 1;
                                listadoComponentes.clear();
                                // Se llama al método principal porque sirve para recuperar el documento original de un componente seguido por un usuario, ya que los documentos de seguimiento sólo constan de un id (el nombre del documento),
                                // que hace referencia al id (y nombre) del documento de la colección de componentes, y un precio de aviso. Luego, en el método de comprobar si el documento cumple los requisitos para añadirlo al listado de la
                                // aplicación se comprobará si cumple también con los filtros del modoBusqueda
                                iniciarListaComponentes();
                                return true;

                            case R.id.nombreZA:
                                modoBusqueda = 2;
                                listadoComponentes.clear();
                                // Se llama al método principal porque sirve para recuperar el documento original de un componente seguido por un usuario, ya que los documentos de seguimiento sólo constan de un id (el nombre del documento),
                                // que hace referencia al id (y nombre) del documento de la colección de componentes, y un precio de aviso. Luego, en el método de comprobar si el documento cumple los requisitos para añadirlo al listado de la
                                // aplicación se comprobará si cumple también con los filtros del modoBusqueda
                                iniciarListaComponentes();
                                return true;

                            case R.id.categoriaAZ:
                                modoBusqueda = 3;
                                listadoComponentes.clear();
                                // Se llama al método principal porque sirve para recuperar el documento original de un componente seguido por un usuario, ya que los documentos de seguimiento sólo constan de un id (el nombre del documento),
                                // que hace referencia al id (y nombre) del documento de la colección de componentes, y un precio de aviso. Luego, en el método de comprobar si el documento cumple los requisitos para añadirlo al listado de la
                                // aplicación se comprobará si cumple también con los filtros del modoBusqueda
                                iniciarListaComponentes();
                                return true;

                            case R.id.categoriaZA:
                                modoBusqueda = 4;
                                listadoComponentes.clear();
                                // Se llama al método principal porque sirve para recuperar el documento original de un componente seguido por un usuario, ya que los documentos de seguimiento sólo constan de un id (el nombre del documento),
                                // que hace referencia al id (y nombre) del documento de la colección de componentes, y un precio de aviso. Luego, en el método de comprobar si el documento cumple los requisitos para añadirlo al listado de la
                                // aplicación se comprobará si cumple también con los filtros del modoBusqueda
                                iniciarListaComponentes();
                                return true;

                            case R.id.precioMayor:
                                modoBusqueda = 5;
                                listadoComponentes.clear();
                                // Se llama al método principal porque sirve para recuperar el documento original de un componente seguido por un usuario, ya que los documentos de seguimiento sólo constan de un id (el nombre del documento),
                                // que hace referencia al id (y nombre) del documento de la colección de componentes, y un precio de aviso. Luego, en el método de comprobar si el documento cumple los requisitos para añadirlo al listado de la
                                // aplicación se comprobará si cumple también con los filtros del modoBusqueda
                                iniciarListaComponentes();
                                return true;

                            case R.id.precioMenor:
                                modoBusqueda = 6;
                                listadoComponentes.clear();
                                // Se llama al método principal porque sirve para recuperar el documento original de un componente seguido por un usuario, ya que los documentos de seguimiento sólo constan de un id (el nombre del documento),
                                // que hace referencia al id (y nombre) del documento de la colección de componentes, y un precio de aviso. Luego, en el método de comprobar si el documento cumple los requisitos para añadirlo al listado de la
                                // aplicación se comprobará si cumple también con los filtros del modoBusqueda
                                iniciarListaComponentes();
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
     * Inicialización de la lista de componentes recuperando los 10 primeros seguidos de la base de datos
     */
    private void iniciarListaComponentes() {

        Log.d("Listado Seguidos", "Se inicializa la lista de componentes");

        // Se recuperan los 10 primeros documentos de la lista de interés del usuario
        db.collection("usuarios")
                .document(email)
                .collection("interes")
                .limit(10)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                        // Una vez recuperados se itera sobre el resultado de la query. Cada documento recuperado se corresponde con un documento de 'interés',
                        // por lo que se busca el documento de tipo 'componente' que corresponda y se añade a la lista de componentes.
                        // Además, guardamos el documento de 'interés' en la variable "lastVisibleSeguidos"
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                            addComponente(recuperarComponente(document));
                            setLastVisibleSeguido(document);
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
     * último cargado anteriormente (lastVisibleSeguido).
     */
    private void ampliarListaComponentes() {

        Log.d("Listado Seguidos", "Final del scroll, número de componentes actuales cargados: " + listadoComponentes.size());

        db.collection("usuarios")
                .document(email)
                .collection("interes")
                .startAfter(lastVisibleSeguido)
                .limit(10)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {

                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            for (QueryDocumentSnapshot document : task.getResult()) {

                                addComponente(recuperarComponente(document));
                                setLastVisibleSeguido(document);
                            }
                            rva.notifyDataSetChanged();
                            isLoading = false;
                        }
                    }
                });
    }

    // ------------------------------------------------------------------------------------------------------------
    // ----------------------------------------- FILTRO LISTA COMPONENTES -----------------------------------------
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Recuperación de los 10 primeros componentes según el filtro del nombre escrito por el usuario. Para el caso de la clase 'Seguidos' podríamos usar el método de
     * iniciarListaComponentes porque el filtro no se aplica a la colección de 'interés', cuyos documentos sólo guardan id y precio, sino que se aplica a la colección de
     * 'componentes'. Por ello el filtro se aplica siempre en el método 'addComponente'. Hemos decidido mantener los métodos separados pese a la repetición del código para
     * hacer más fácil la lectura del mismo y por posibles cambios futuros en la base de datos (por ejemplo, podríamos añadir un campo nombre en el documento de 'interés' y
     * aplicar el filtro directamente en este método, pero decidimos no hacerlo así para no tener dos documentos relacionados con datos repetidos pese a que la base de datos no es relacional)
     */
    private void filtrarListaComponentes() {

        Log.d("Filtro Seguidos", "Se solicita recuperar de la base de datos los componentes con filtro: " + filtro);

        db.collection("usuarios")
                .document(email)
                .collection("interes")
                .limit(10)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {

                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {

                                addComponente(recuperarComponente(document));
                                setLastVisibleSeguido(document);
                            }
                            cargarRecyclerView();
                            isLoading = false;
                        }
                    }
                });
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

        Log.d("Listado Seguidos", "Aplicado filtro: " + query);

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

    /**
     * Comparador manual por nombre ordenado de menor a mayor
     */
    public class ordenacionNombreAZ implements Comparator<Item> {
        public int compare(Item izq, Item der) {
            return izq.getNombre().compareTo(der.getNombre());
        }
    }

    /**
     * Comparador manual por nombre ordenado de mayor a menor
     */
    public class ordenacionNombreZA implements Comparator<Item> {
        public int compare(Item izq, Item der) {
            return der.getNombre().compareTo(izq.getNombre());
        }
    }

    /**
     * Comparador manual por categoría ordenado de menor a mayor
     */
    public class ordenacionCategoriaAZ implements Comparator<Item> {
        public int compare(Item izq, Item der) {
            return izq.getCategoria().compareTo(der.getCategoria());
        }
    }

    /**
     * Comparador manual por categoría ordenado de mayor a menor
     */
    public class ordenacionCategoriaZA implements Comparator<Item> {
        public int compare(Item izq, Item der) {
            return der.getCategoria().compareTo(izq.getCategoria());
        }
    }

    /**
     * Comparador manual por precio ordenado de menor a mayor
     */
    public class ordenacionPrecioMenor implements Comparator<Item> {
        public int compare(Item izq, Item der) {
            return Double.compare(izq.getPrecio(), der.getPrecio());
        }
    }

    /**
     * Comparador manual por precio ordenado de mayor a menor
     */
    public class ordenacionPrecioMayor implements Comparator<Item> {
        public int compare(Item izq, Item der) {
            return Double.compare(der.getPrecio(), izq.getPrecio());
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // -------------------------------------------- AÑADIR COMPONENTES --------------------------------------------
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Método que devuelve el documento de la colección 'componentes' que tenga el mismo nombre que el documento de 'interés'
     *
     * @param document Documento de interés del usuario
     * @return Documento del componente relacionado con ese interés
     */
    private Task<DocumentSnapshot> recuperarComponente(QueryDocumentSnapshot document) {
        Task<DocumentSnapshot> task = db.collection("componentes")
                .document(document.getId())
                .get();

        return task;
    }

    /**
     * Añade un componente a la lista de componentes si su nombre coincide con el filtro
     *
     * @param task documento del componente
     */
    public void addComponente(Task<DocumentSnapshot> task) {

        task.addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {

                String added = "No añadido";

                Log.d("Componente Seguidos", "Documento con id: " + documentSnapshot.getId());

                Map<String, Object> componente = documentSnapshot.getData();

                if (filtro.equals("")) {

                    Item component = new Item(
                            documentSnapshot.getId(),
                            (String) componente.get("nombre"),
                            (String) componente.get("img"),
                            ((Number) componente.get("precio")).doubleValue(),
                            (String) componente.get("url"),
                            (String) componente.get("categoria"),
                            (Boolean) componente.get("valida"));

                    listadoComponentes.add(component);

                    added = "Añadido";

                } else {

                    // A diferencia del listado de componentes aquí sí podemos filtrar por "contains" porque no estamos haciendo una
                    // consulta a Firebase, sino al array de documentos que hemos descargado. No es viable hacer lo mismo en el listado
                    // normal por una cuestión de tiempo de procesamiento y, especialmente, porque habría que leer toda la base de datos para
                    // después filtrarla, realizando miles de operaciones de lectura por filtro

                    String nombre = (String) componente.get("nombre");
                    String categoria = (String) componente.get("categoria");

                    // Si no se ha seleccionado el check se busca por nombre
                    if (!modoSwitchBusqueda) {
                        if (StringUtils.containsIgnoreCase(nombre, filtro)) {

                            Item component = new Item(
                                    documentSnapshot.getId(),
                                    (String) componente.get("nombre"),
                                    (String) componente.get("img"),
                                    ((Number) componente.get("precio")).doubleValue(),
                                    (String) componente.get("url"),
                                    (String) componente.get("categoria"),
                                    (Boolean) componente.get("valida"));

                            listadoComponentes.add(component);

                            added = "Añadido";
                        }
                    }
                    // Si se ha seleccionado se busca por categoría
                    else {
                        if (StringUtils.containsIgnoreCase(categoria, filtro)) {

                            Item component = new Item(
                                    documentSnapshot.getId(),
                                    (String) componente.get("nombre"),
                                    (String) componente.get("img"),
                                    ((Number) componente.get("precio")).doubleValue(),
                                    (String) componente.get("url"),
                                    (String) componente.get("categoria"),
                                    (Boolean) componente.get("valida"));

                            listadoComponentes.add(component);

                            added = "Añadido";
                        }
                    }
                }

                // Se ordena el listado según lo seleccionado
                switch (modoBusqueda) {
                    // Estándar
                    case 0: {
                        // No se hace nada
                        break;
                    }
                    // Nombre AZ
                    case 1: {
                        Collections.sort(listadoComponentes, new ordenacionNombreAZ());
                        break;
                    }
                    // Nombre ZA
                    case 2: {
                        Collections.sort(listadoComponentes, new ordenacionNombreZA());
                        break;
                    }
                    // Categoría AZ
                    case 3: {
                        Collections.sort(listadoComponentes, new ordenacionCategoriaAZ());
                        break;
                    }
                    // Categoría ZA
                    case 4: {
                        Collections.sort(listadoComponentes, new ordenacionCategoriaZA());
                        break;
                    }
                    // Precio mayor
                    case 5: {
                        Collections.sort(listadoComponentes, new ordenacionPrecioMayor());
                        break;
                    }
                    // Precio menor
                    case 6: {
                        Collections.sort(listadoComponentes, new ordenacionPrecioMenor());
                        break;
                    }
                    // Se trata como estándar pero nunca debería llegar a este caso
                    default : {
                        // No se hace nada
                        break;
                    }
                }

                rva.notifyDataSetChanged();
                isLoading = false;

                Log.d("Componente Seguidos", "El documento se corresponde con el componente con nombre: " + componente.get("nombre") + ". " + added);

            }
        });
    }

    /**
     * Crea las fichas de los componentes y redirecciona a la vista ComponentView cuando se selecciona una de ellas
     */
    private void cargarRecyclerView() {

        Log.d("Recycler Seguidos", "Componentes actuales en el listado: " + listadoComponentes.size());

        this.rva= new Adapter(listadoComponentes);
        rva.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent componenteView = new Intent(SeguidosView.this, ComponenteView.class);
                Bundle miBundle = new Bundle();
                miBundle.putSerializable("componente", listadoComponentes.get(recycler.getChildAdapterPosition(v)));
                componenteView.putExtras(miBundle);

                SeguidosView.this.startActivity(componenteView);
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

                if (!isLoading && dy > 0) {
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
        MenuItem mi= menu.findItem(R.id.buscador);
        SearchView sv =(SearchView) mi.getActionView();
        sv.setOnQueryTextListener(this);

        mi.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) { return true; }

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
                    Toast.makeText(SeguidosView.this, "Búsqueda en lupa por nombre de componente", Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(SeguidosView.this, "Búsqueda en lupa por categoría de componente", Toast.LENGTH_LONG).show();
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
                i = new Intent(getApplicationContext(), Main.class);;
                startActivity(i);
                finish();
                overridePendingTransition(0,0);
                break;

            case R.id.op_perfil:
                i = new Intent(getApplicationContext(), Profile.class);
                startActivity(i);
                finish();
                overridePendingTransition(0,0);
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Menú inferior con la redirección al listado general de componentes a la lista de seguidos (en este último caso se refresca la vista)
     */
    private void setListenerBottomMenu() {

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId() == R.id.seguidos){

                    recreate();
                    overridePendingTransition(0,0);

                    return true;
                }
                else if(item.getItemId() == R.id.principal){

                    // No se crea un intent nuevo del tipo 'MainActivity' porque el listado general de componentes siempre se abrirá al iniciar la aplicación,
                    // por lo que cerrando el actual (Seguidos) volveríamos a dicha vista siempre, evitando lecturas innecesarias a la base de datos y manteniendo
                    // al usuario en el lugar del listado donde lo dejó. De querer refrescar la vista lo podría hacer pulsando 'Principal' desde MainActivity
                    finish();
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
    private void setLastVisibleSeguido(DocumentSnapshot last) {
        this.lastVisibleSeguido = last;
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


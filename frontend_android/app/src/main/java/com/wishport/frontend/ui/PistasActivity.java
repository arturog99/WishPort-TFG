package com.wishport.frontend.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.wishport.frontend.R;
import com.wishport.frontend.adapters.PistaAdapter;
import com.wishport.frontend.api.TokenManager;
import com.wishport.frontend.ui.viewmodels.PistasViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * PANTALLA PRINCIPAL: Muestra el catálogo de pistas disponibles.
 *
 * Es la pantalla que ven los usuarios con rol USER tras hacer login.
 * Implementa el patrón MVVM: la Activity solo gestiona la UI,
 * toda la lógica de datos vive en PistasViewModel.
 *
 * Funcionalidades:
 * - Lista de pistas con foto, deporte y estado.
 * - Pull-to-refresh (deslizar hacia abajo para recargar).
 * - Efecto Shimmer (animación de carga mientras llegan los datos).
 * - Navegación a Mis Reservas, Perfil y DetallePista.
 * - Menú superior con opción de Logout.
 */
@AndroidEntryPoint
public class PistasActivity extends AppCompatActivity {

    /** Lista visual de pistas que ocupa el cuerpo de la pantalla */
    private RecyclerView recyclerViewPistas;
    /** Adaptador que convierte la lista de Pista en filas visuales */
    private PistaAdapter pistaAdapter;
    /** Contenedor que habilita el gesto de "deslizar para recargar" */
    private SwipeRefreshLayout swipeRefreshLayout;
    /** Contenedor que muestra la animación de esqueleto mientras cargan los datos */
    private ShimmerFrameLayout shimmerFrameLayout;
    /** ViewModel que gestiona la obtención y el estado de la lista de pistas */
    private PistasViewModel viewModel;

    /**
     * Punto de entrada de la Activity.
     * 1. Obtiene el ViewModel (Hilt lo crea e inyecta sus dependencias).
     * 2. Vincula las vistas con sus IDs del XML.
     * 3. Configura los listeners de botones y gestos.
     * 4. Se suscribe a los LiveData del ViewModel.
     * 5. Lanza la primera carga de pistas.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_deportes);

        viewModel = new ViewModelProvider(this).get(PistasViewModel.class);

        vincularVistas();
        configurarEventos();
        observarDatos();

        viewModel.cargarPistas(); // Dispara GET /api/pistas al arrancar
    }

    /**
     * Vincula las variables Java con los elementos visuales del XML.
     * También configura el RecyclerView para que muestre una lista vertical.
     */
    private void vincularVistas() {
        recyclerViewPistas = findViewById(R.id.recyclerViewPistas);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshPistas);
        shimmerFrameLayout = findViewById(R.id.shimmerPistas);
        recyclerViewPistas.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * Configura los listeners de los botones de navegación y gestos:
     * - btnMisReservas: navega a ReservasActivity.
     * - btnPerfil: navega a PerfilActivity.
     * - swipeRefresh: vuelve a llamar a cargarPistas() para refrescar la lista.
     */
    private void configurarEventos() {
        findViewById(R.id.btnMisReservas).setOnClickListener(v ->
            startActivity(new Intent(this, ReservasActivity.class)));

        findViewById(R.id.btnPerfil).setOnClickListener(v ->
            startActivity(new Intent(this, PerfilActivity.class)));

        swipeRefreshLayout.setOnRefreshListener(() -> viewModel.cargarPistas());
    }

    /**
     * Se suscribe a los LiveData del ViewModel para actualizar la UI automáticamente:
     *
     * pistas: cuando llega la lista, crea el adaptador y lo asigna al RecyclerView.
     *   También registra el listener para que al pulsar una pista, se abra
     *   DetallePistaActivity pasándole el objeto Pista como Serializable.
     *
     * isLoading: gestiona la animación Shimmer y el indicador de swipe-refresh.
     *   - true  -> muestra shimmer, oculta la lista.
     *   - false -> oculta shimmer, muestra la lista, para el indicador de refresh.
     *
     * error: si algo falla, muestra el mensaje en un Toast.
     */
    private void observarDatos() {
        viewModel.pistas.observe(this, listaPistas -> {
            pistaAdapter = new PistaAdapter(listaPistas);
            recyclerViewPistas.setAdapter(pistaAdapter);
            pistaAdapter.setOnItemClickListener(pista -> {
                Intent intent = new Intent(this, DetallePistaActivity.class);
                // EXTRA_PISTA = clave de contrato para pasar la Pista entre Activities
                intent.putExtra(DetallePistaActivity.EXTRA_PISTA, pista);
                startActivity(intent);
            });
        });

        viewModel.isLoading.observe(this, isLoading -> {
            if (isLoading) {
                if (!swipeRefreshLayout.isRefreshing()) {
                    shimmerFrameLayout.setVisibility(View.VISIBLE);
                    shimmerFrameLayout.startShimmer();
                    recyclerViewPistas.setVisibility(View.GONE);
                }
            } else {
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
                recyclerViewPistas.setVisibility(View.VISIBLE);
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        viewModel.error.observe(this, mensaje -> {
            if (mensaje != null) {
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Infla el menú de la barra superior (action bar) con las opciones definidas
     * en res/menu/menu_main.xml. En este caso, solo tiene el botón de "Cerrar sesión".
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Maneja los clics en los items del menú superior.
     * Si el usuario pulsa "Cerrar sesión", llama a hacerLogout().
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            hacerLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Cierra la sesión del usuario:
     * 1. Borra todos los datos de SharedPreferences (id, nombre, email, rol).
     * 2. Borra el token JWT de EncryptedSharedPreferences via TokenManager.
     * 3. Navega a LoginActivity limpiando toda la pila de Activities
     *    (FLAG_ACTIVITY_CLEAR_TASK) para que el usuario no pueda volver atrás.
     */
    private void hacerLogout() {
        getSharedPreferences("WishPortPrefs", MODE_PRIVATE).edit().clear().apply();
        TokenManager.clear(this);
        startActivity(new Intent(this, LoginActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}

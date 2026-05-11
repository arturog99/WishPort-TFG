package com.wishport.frontend.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.wishport.frontend.data.repository.PistasRepository;
import com.wishport.frontend.models.Pista;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * VIEWMODEL DE PISTAS: Gestiona el estado de la pantalla de pistas.
 *
 * Mantiene los datos incluso si el usuario rota el móvil o cambia de pantalla,
 * porque los ViewModels no se destruyen con la Activity.
 *
 * PistasActivity crea este ViewModel, llama a cargarPistas() una vez,
 * y luego observa los LiveData para reaccionar a los cambios.
 */
@HiltViewModel
public class PistasViewModel extends ViewModel {

    /** Repositorio que gestiona la petición al backend para obtener las pistas */
    private final PistasRepository repository;

    /**
     * Lista de pistas obtenidas del servidor.
     * Cuando cambia, PistasActivity actualiza el RecyclerView con las nuevas pistas.
     * MutableLiveData = versión modificable (solo el ViewModel escribe).
     * LiveData público = versión de solo lectura (la Activity solo observa).
     */
    private final MutableLiveData<List<Pista>> _pistas = new MutableLiveData<>();
    public LiveData<List<Pista>> pistas = _pistas;

    /**
     * Indicador de operación en curso.
     * true = se están descargando pistas (mostrar spinner).
     * false = operación terminada (ocultar spinner).
     */
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    /**
     * Mensaje de error si la descarga falla.
     * PistasActivity lo observa para mostrar un Toast o mensaje en pantalla.
     * Se resetea a null al inicio de cada nueva carga.
     */
    private final MutableLiveData<String> _error = new MutableLiveData<>(null);
    public LiveData<String> error = _error;

    /**
     * Constructor inyectado por Hilt.
     * @param repository Repositorio de pistas proporcionado automáticamente por Hilt.
     */
    @Inject
    public PistasViewModel(PistasRepository repository) {
        this.repository = repository;
    }

    /**
     * Lanza la petición al servidor para obtener todas las pistas disponibles.
     * 1. Activa el estado de carga y limpia errores previos.
     * 2. Pide al repositorio la llamada GET /api/pistas.
     * 3. Si hay éxito: publica la lista en _pistas para que la Activity la dibuje.
     * 4. Si hay error de servidor o fallo de red: publica el mensaje en _error.
     *
     * Se llama desde PistasActivity en onCreate() y en el pull-to-refresh.
     */
    public void cargarPistas() {
        _isLoading.setValue(true);
        _error.setValue(null);

        repository.getPistas().enqueue(new Callback<List<Pista>>() {
            @Override
            public void onResponse(Call<List<Pista>> call, Response<List<Pista>> response) {
                _isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    // Pistas recibidas: notificamos a PistasActivity para que las dibuje
                    _pistas.setValue(response.body());
                } else {
                    _error.setValue("Error al cargar las pistas");
                }
            }

            @Override
            public void onFailure(Call<List<Pista>> call, Throwable t) {
                _isLoading.setValue(false);
                _error.setValue("Fallo de conexión: " + t.getMessage());
            }
        });
    }
}

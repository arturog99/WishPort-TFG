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
 * Se encarga de pedir los datos al repositorio y exponerlos a la actividad
 * de forma que sobrevivan a rotaciones de pantalla.
 */
@HiltViewModel
public class PistasViewModel extends ViewModel {

    private final PistasRepository repository;
    
    // LiveData: Contenedores de datos observables por la Activity
    private final MutableLiveData<List<Pista>> _pistas = new MutableLiveData<>();
    public LiveData<List<Pista>> pistas = _pistas;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _error = new MutableLiveData<>(null);
    public LiveData<String> error = _error;

    @Inject
    public PistasViewModel(PistasRepository repository) {
        this.repository = repository;
    }

    /**
     * Carga la lista de pistas desde el servidor.
     */
    public void cargarPistas() {
        _isLoading.setValue(true);
        _error.setValue(null);

        repository.getPistas().enqueue(new Callback<List<Pista>>() {
            @Override
            public void onResponse(Call<List<Pista>> call, Response<List<Pista>> response) {
                _isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
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

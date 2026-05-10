package com.wishport.frontend.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.wishport.frontend.data.dto.LoginRequest;
import com.wishport.frontend.data.dto.RegistroRequest;
import com.wishport.frontend.data.repository.AuthRepository;
import com.wishport.frontend.models.Usuario;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * VIEWMODEL DE AUTENTICACIÓN: Maneja la lógica de Login y Registro.
 * Separa la lógica de negocio de la interfaz de usuario.
 */
@HiltViewModel
public class AuthViewModel extends ViewModel {

    private final AuthRepository repository;

    private final MutableLiveData<Map<String, Object>> _loginResponse = new MutableLiveData<>();
    public LiveData<Map<String, Object>> loginResponse = _loginResponse;

    private final MutableLiveData<Usuario> _registroResponse = new MutableLiveData<>();
    public LiveData<Usuario> registroResponse = _registroResponse;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _error = new MutableLiveData<>(null);
    public LiveData<String> error = _error;

    @Inject
    public AuthViewModel(AuthRepository repository) {
        this.repository = repository;
    }

    public void login(String email, String password) {
        _isLoading.setValue(true);
        _error.setValue(null);

        repository.login(new LoginRequest(email, password)).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                _isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    _loginResponse.setValue(response.body());
                } else {
                    _error.setValue(extraerMensajeError(response, "Credenciales incorrectas"));
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                _isLoading.setValue(false);
                _error.setValue("Error de conexión: " + t.getMessage());
            }
        });
    }

    public void registrar(String nombre, String email, String password, String telefono) {
        _isLoading.setValue(true);
        _error.setValue(null);

        repository.registrar(new RegistroRequest(nombre, email, password, telefono)).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                _isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    _registroResponse.setValue(response.body());
                } else {
                    _error.setValue(extraerMensajeError(response, "Error en el registro: email ya en uso"));
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                _isLoading.setValue(false);
                _error.setValue("Error de conexión: " + t.getMessage());
            }
        });
    }

    /**
     * Intenta leer el mensaje de error del cuerpo de la respuesta HTTP.
     * Si no puede, devuelve el mensaje por defecto proporcionado.
     */
    private <T> String extraerMensajeError(Response<T> response, String porDefecto) {
        if (response.errorBody() != null) {
            try {
                String errorRaw = response.errorBody().string();
                if (errorRaw != null && !errorRaw.isEmpty()) {
                    return errorRaw;
                }
            } catch (IOException ignored) {
            }
        }
        return porDefecto;
    }
}

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
 * VIEWMODEL DE AUTENTICACIÓN: Maneja toda la lógica de Login y Registro.
 *
 * Un ViewModel es un componente de arquitectura de Android que:
 * 1. Sobrevive a rotaciones de pantalla (no se destruye al girar el móvil).
 * 2. Separa la lógica de negocio de la interfaz visual (Activity).
 * 3. Expone datos mediante LiveData para que la Activity los observe.
 *
 * La Activity nunca toca directamente la red; solo llama métodos del ViewModel
 * y observa sus LiveData para saber qué mostrar.
 *
 * @HiltViewModel indica a Hilt que debe inyectar las dependencias de este ViewModel
 * automáticamente cuando la Activity lo solicite con ViewModelProvider.
 */
@HiltViewModel
public class AuthViewModel extends ViewModel {

    /** Repositorio que gestiona todas las llamadas HTTP de usuarios */
    private final AuthRepository repository;

    /**
     * MutableLiveData interno (privado): solo el ViewModel puede modificarlo.
     * Cuando el login es exitoso, se pone aquí el mapa con token, nombre, rol, etc.
     */
    private final MutableLiveData<Map<String, Object>> _loginResponse = new MutableLiveData<>();
    /**
     * LiveData público (solo lectura): la Activity lo observa.
     * Cuando cambia, la Activity sabe que el login fue exitoso.
     */
    public LiveData<Map<String, Object>> loginResponse = _loginResponse;

    /**
     * Resultado del registro. Cuando cambia, significa que el nuevo usuario fue creado.
     * La Activity observa esto para volver al login con un mensaje de éxito.
     */
    private final MutableLiveData<Usuario> _registroResponse = new MutableLiveData<>();
    public LiveData<Usuario> registroResponse = _registroResponse;

    /**
     * Estado de carga: true = hay una petición en curso, false = terminada.
     * La Activity lo usa para mostrar/ocultar un ProgressBar.
     */
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    /**
     * Mensaje de error. Cuando no es null, la Activity muestra un Toast con el texto.
     * Se limpia a null al iniciar una nueva operación para no mostrar errores viejos.
     */
    private final MutableLiveData<String> _error = new MutableLiveData<>(null);
    public LiveData<String> error = _error;

    /**
     * Constructor inyectado por Hilt.
     * @Inject le dice a Hilt que rellene el parámetro repository automáticamente.
     * @param repository Repositorio de autenticación proporcionado por Hilt.
     */
    @Inject
    public AuthViewModel(AuthRepository repository) {
        this.repository = repository;
    }

    /**
     * Inicia el proceso de login.
     * 1. Activa el estado de carga y limpia errores anteriores.
     * 2. Crea un LoginRequest con email y password.
     * 3. Lo envía al repositorio para que llame al backend.
     * 4. Según el resultado, actualiza loginResponse o error.
     *
     * @param email    Email introducido por el usuario en el formulario.
     * @param password Contraseña introducida por el usuario.
     */
    public void login(String email, String password) {
        _isLoading.setValue(true);
        _error.setValue(null);

        repository.login(new LoginRequest(email, password)).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                _isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    // Login correcto: notificamos a la Activity con los datos del usuario
                    _loginResponse.setValue(response.body());
                } else {
                    // El servidor rechazó las credenciales (ej: HTTP 401)
                    _error.setValue(extraerMensajeError(response, "Credenciales incorrectas"));
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                // Fallo de red: sin conexión, timeout, etc.
                _isLoading.setValue(false);
                _error.setValue("Error de conexión: " + t.getMessage());
            }
        });
    }

    /**
     * Inicia el proceso de registro de un nuevo usuario.
     * 1. Activa el estado de carga.
     * 2. Crea un RegistroRequest con los datos del formulario.
     * 3. Lo envía al repositorio.
     * 4. Si tiene éxito, registroResponse notifica a la Activity para volver al login.
     *
     * @param nombre   Nombre del nuevo usuario.
     * @param email    Email del nuevo usuario.
     * @param password Contraseña en texto plano.
     * @param telefono Teléfono del nuevo usuario.
     */
    public void registrar(String nombre, String email, String password, String telefono) {
        _isLoading.setValue(true);
        _error.setValue(null);

        repository.registrar(new RegistroRequest(nombre, email, password, telefono)).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                _isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    // Registro correcto: notificamos a la Activity para que cierre
                    _registroResponse.setValue(response.body());
                } else {
                    // Error del servidor: email ya en uso, datos inválidos, etc.
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
     * Intenta extraer un mensaje legible del cuerpo de error HTTP.
     * Cuando el backend devuelve un error (ej: 400, 409), puede incluir
     * un texto explicativo en el cuerpo. Este método lo extrae.
     * Si no hay cuerpo o falla la lectura, devuelve el mensaje por defecto.
     *
     * @param response   Respuesta HTTP con código de error.
     * @param porDefecto Mensaje a usar si no se puede leer el cuerpo.
     * @return Mensaje de error legible para mostrar en la UI.
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

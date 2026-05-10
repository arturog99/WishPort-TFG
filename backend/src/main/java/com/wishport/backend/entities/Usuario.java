package com.wishport.backend.entities;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

/**
 * Entidad que representa a un usuario en el sistema.
 *
 * Un usuario puede ser un cliente regular que hace reservas de pistas o un
 * administrador que gestiona la aplicación. Mapea directamente con la tabla
 * {@code usuarios} de la base de datos.
 *
 */
@Entity
@Table(name = "usuarios")
public class Usuario implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Identificador único del usuario.
     * Generado automáticamente por la base de datos (Auto-incremental).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    /**
     * Nombre completo o de perfil del usuario.
     */
    @Column(name = "nombre", nullable = false)
    private String nombre;

    /**
     * Correo electrónico del usuario. Debe ser único en el sistema.
     * Se utiliza como nombre de usuario para el inicio de sesión (Login).
     */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /**
     * Contraseña del usuario (almacenada en formato hash B-Crypt).
     * La anotación @JsonProperty(Access.WRITE_ONLY) indica a Jackson que
     * debe permitir leer este campo cuando nos llega un JSON de registro/login,
     * pero NUNCA debe devolver la contraseña cuando devolvamos el usuario al frontend.
     */
    @Column(name = "password", nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /**
     * Número de teléfono de contacto.
     * Es OBLIGATORIO ya que se utiliza para verificar la identidad y 
     * contactar con el usuario en lugar de pedir el DNI.
     */
    @Column(name = "telefono", nullable = false)
    private String telefono;

    /**
     * Rol del usuario dentro de la aplicación.
     * Valores comunes: "USER" (por defecto) o "ADMIN".
     */
    @Column(name = "rol")
    private String rol;

    /**
     * Relación 1:N (Uno a Muchos) con la entidad Reserva.
     * Un usuario puede tener múltiples reservas asociadas a él.
     * <p>
     * La anotación @JsonIgnore es crítica aquí: evita que al enviar un usuario
     * al frontend como JSON, se serialicen sus reservas. Si no lo pusiéramos,
     * se generaría un bucle infinito (Usuario -> Reservas -> Usuario -> Reservas...).
     * </p>
     */
    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "idUsuario")
    private List<Reserva> reservasList;

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * Constructor vacío necesario para que JPA (Hibernate) pueda crear
     * instancias dinámicamente al leer la base de datos.
     */
    public Usuario() {}

    /**
     * Constructor que inicializa el usuario solo con su ID.
     * Útil cuando se necesita vincular una reserva a un usuario sin cargar todo.
     */
    public Usuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    /**
     * Constructor con todos los campos obligatorios para crear un usuario.
     * Se incluye el teléfono como parámetro obligatorio.
     */
    public Usuario(Integer idUsuario, String nombre, String email, String password, String telefono) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.telefono = telefono;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public List<Reserva> getReservasList() { return reservasList; }
    public void setReservasList(List<Reserva> reservasList) { this.reservasList = reservasList; }
}

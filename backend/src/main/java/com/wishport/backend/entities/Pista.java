package com.wishport.backend.entities;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * Entidad que representa a una pista deportiva en el sistema.
 * <p>
 * Mapea directamente con la tabla {@code pistas} de la base de datos.
 * Esta entidad contiene toda la información pública sobre las instalaciones
 * deportivas que los usuarios pueden consultar y reservar.
 * </p>
 */
@Entity
@Table(name = "pistas")
public class Pista implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Identificador único de la pista.
     * Generado automáticamente por la base de datos (Auto-incremental).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pista")
    private Integer idPista;

    /**
     * Nombre visible de la pista (ej. "Pista Central", "Pista Norte").
     */
    @Column(name = "nombre", nullable = false)
    private String nombre;

    /**
     * Deporte al que está destinado la pista (ej. "Pádel", "Fútbol 11").
     */
    @Column(name = "deporte", nullable = false)
    private String deporte;

    /**
     * URL o ruta donde se encuentra alojada la imagen representativa
     * de la pista para mostrarse en la interfaz de la aplicación.
     */
    @Column(name = "foto_url")
    private String fotoUrl;

    /**
     * Estado actual de la pista (ej. "disponible", "mantenimiento").
     * Si la pista no está disponible, no debería poder ser reservada.
     */
    @Column(name = "estado")
    private String estado;

    /**
     * Relación 1:N (Uno a Muchos) con la entidad Reserva.
     * Una pista puede tener múltiples reservas asignadas a lo largo del tiempo.
     * <p>
     * Se usa @JsonIgnore para evitar que al pedir una lista de pistas
     * desde la aplicación móvil o web, nos llegue todo el histórico de
     * reservas de la pista (evitando respuestas inmensas y bucles infinitos).
     * </p>
     */
    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "idPista", fetch = FetchType.LAZY)
    private List<Reserva> reservasList;

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * Constructor vacío necesario para que JPA (Hibernate) instancie
     * la clase al recuperar datos de la base de datos.
     */
    public Pista() {}

    /**
     * Constructor usado para referenciar a una pista únicamente por su ID.
     * Útil al crear reservas donde solo pasamos el ID de la pista.
     */
    public Pista(Integer idPista) {
        this.idPista = idPista;
    }

    /**
     * Constructor con campos obligatorios para registrar una nueva pista.
     */
    public Pista(Integer idPista, String nombre, String deporte) {
        this.idPista = idPista;
        this.nombre = nombre;
        this.deporte = deporte;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    public Integer getIdPista() { return idPista; }
    public void setIdPista(Integer idPista) { this.idPista = idPista; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDeporte() { return deporte; }
    public void setDeporte(String deporte) { this.deporte = deporte; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public List<Reserva> getReservasList() { return reservasList; }
    public void setReservasList(List<Reserva> reservasList) { this.reservasList = reservasList; }
}

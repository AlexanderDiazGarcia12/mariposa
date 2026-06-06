package com.mariposa.biblioteca.infraestructura.persistencia.entidades;

import com.mariposa.biblioteca.dominio.modelo.Genero;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "libros")
public class EntidadLibro {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Column(name = "autor", nullable = false, length = 255)
    private String autor;

    @Column(name = "isbn", nullable = false, length = 13, unique = true)
    private String isbn;

    @Column(name = "anio_publicacion", nullable = false)
    private int anioPublicacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "genero", nullable = false, length = 20)
    private Genero genero;

    @Column(name = "copias_totales", nullable = false)
    private int copiasTotales;

    @Column(name = "copias_disponibles", nullable = false)
    private int copiasDisponibles;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    protected EntidadLibro() {
    }

    public EntidadLibro(
            UUID id,
            String titulo,
            String autor,
            String isbn,
            int anioPublicacion,
            Genero genero,
            int copiasTotales,
            int copiasDisponibles,
            Instant fechaCreacion,
            Instant fechaActualizacion
    ) {
        this.id = id;
        this.titulo = titulo;
        this.autor = autor;
        this.isbn = isbn;
        this.anioPublicacion = anioPublicacion;
        this.genero = genero;
        this.copiasTotales = copiasTotales;
        this.copiasDisponibles = copiasDisponibles;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public int getAnioPublicacion() {
        return anioPublicacion;
    }

    public void setAnioPublicacion(int anioPublicacion) {
        this.anioPublicacion = anioPublicacion;
    }

    public Genero getGenero() {
        return genero;
    }

    public void setGenero(Genero genero) {
        this.genero = genero;
    }

    public int getCopiasTotales() {
        return copiasTotales;
    }

    public void setCopiasTotales(int copiasTotales) {
        this.copiasTotales = copiasTotales;
    }

    public int getCopiasDisponibles() {
        return copiasDisponibles;
    }

    public void setCopiasDisponibles(int copiasDisponibles) {
        this.copiasDisponibles = copiasDisponibles;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Instant getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(Instant fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof EntidadLibro entidad)) {
            return false;
        }
        return Objects.equals(id, entidad.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

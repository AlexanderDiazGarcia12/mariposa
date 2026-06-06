package com.mariposa.biblioteca.infraestructura.persistencia.specifications;

import com.mariposa.biblioteca.dominio.modelo.Genero;
import com.mariposa.biblioteca.dominio.modelo.consultas.FiltroLibros;
import com.mariposa.biblioteca.infraestructura.persistencia.entidades.EntidadLibro;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class EspecificacionesLibro {

    private static final String CAMPO_AUTOR = "autor";
    private static final String CAMPO_GENERO = "genero";
    private static final String CAMPO_COPIAS_DISPONIBLES = "copiasDisponibles";

    private EspecificacionesLibro() {
    }

    public static Specification<EntidadLibro> porAutor(String autor) {
        var prefijo = autor.toLowerCase() + "%";
        return (raiz, consulta, constructor) ->
                constructor.like(constructor.lower(raiz.get(CAMPO_AUTOR)), prefijo);
    }

    public static Specification<EntidadLibro> porGenero(Genero genero) {
        return (raiz, consulta, constructor) ->
                constructor.equal(raiz.get(CAMPO_GENERO), genero);
    }

    public static Specification<EntidadLibro> conDisponibilidad() {
        return (raiz, consulta, constructor) ->
                constructor.greaterThan(raiz.get(CAMPO_COPIAS_DISPONIBLES), 0);
    }

    public static Specification<EntidadLibro> sinDisponibilidad() {
        return (raiz, consulta, constructor) ->
                constructor.equal(raiz.get(CAMPO_COPIAS_DISPONIBLES), 0);
    }

    public static Specification<EntidadLibro> desde(FiltroLibros filtro) {
        List<Specification<EntidadLibro>> criterios = new ArrayList<>();
        filtro.autor().filter(valor -> !valor.isBlank()).ifPresent(valor -> criterios.add(porAutor(valor)));
        filtro.genero().ifPresent(valor -> criterios.add(porGenero(valor)));
        filtro.conDisponibilidad().ifPresent(valor -> criterios.add(valor ? conDisponibilidad() : sinDisponibilidad()));
        return Specification.allOf(criterios);
    }
}

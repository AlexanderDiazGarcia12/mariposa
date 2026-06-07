package com.mariposa.biblioteca.infraestructura.seguridad;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RegistroLimitadoresTasa {

    private final PropiedadesLimiteTasaInicioSesion propiedadesInicioSesion;
    private final ConcurrentMap<String, Bucket> bucketsInicioSesion = new ConcurrentHashMap<>();

    public RegistroLimitadoresTasa(PropiedadesLimiteTasaInicioSesion propiedadesInicioSesion) {
        this.propiedadesInicioSesion = propiedadesInicioSesion;
    }

    public Bucket resolverInicioSesion(String clave) {
        return bucketsInicioSesion.computeIfAbsent(clave, ignorado -> construirBucketInicioSesion());
    }

    private Bucket construirBucketInicioSesion() {
        var capacidad = propiedadesInicioSesion.capacidad();
        var ventana = Duration.ofSeconds(propiedadesInicioSesion.ventanaSegundos());
        var ancho = Bandwidth.builder()
                .capacity(capacidad)
                .refillIntervally(capacidad, ventana)
                .build();
        return Bucket.builder().addLimit(ancho).build();
    }
}

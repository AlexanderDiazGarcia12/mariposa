package com.mariposa.biblioteca.dominio.puertos.salida;

import com.mariposa.biblioteca.dominio.modelo.Rol;
import com.mariposa.biblioteca.dominio.modelo.Usuario;

import java.time.Instant;
import java.util.UUID;

public interface ProveedorToken {

    TokenGenerado generarAcceso(Usuario usuario);

    TokenGenerado generarRefresco(Usuario usuario);

    ClaimsToken validar(String token);

    record TokenGenerado(String token, Instant expiraEn) {
    }

    record ClaimsToken(UUID idUsuario, String nombreUsuario, Rol rol, String tipo, Instant expiraEn) {

        public static final String TIPO_ACCESO = "acceso";
        public static final String TIPO_REFRESCO = "refresco";

        public boolean esAcceso() {
            return TIPO_ACCESO.equals(tipo);
        }

        public boolean esRefresco() {
            return TIPO_REFRESCO.equals(tipo);
        }
    }
}

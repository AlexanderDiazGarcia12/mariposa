package com.mariposa.biblioteca.infraestructura.seguridad;

import com.mariposa.biblioteca.dominio.puertos.salida.CodificadorContrasena;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CodificadorContrasenaBCrypt implements CodificadorContrasena {

    private final BCryptPasswordEncoder codificador;

    public CodificadorContrasenaBCrypt(BCryptPasswordEncoder codificador) {
        this.codificador = codificador;
    }

    @Override
    public String codificar(String contrasenaPlana) {
        return codificador.encode(contrasenaPlana);
    }

    @Override
    public boolean coincide(String contrasenaPlana, String contrasenaCodificada) {
        if (contrasenaPlana == null || contrasenaCodificada == null) {
            return false;
        }
        return codificador.matches(contrasenaPlana, contrasenaCodificada);
    }
}

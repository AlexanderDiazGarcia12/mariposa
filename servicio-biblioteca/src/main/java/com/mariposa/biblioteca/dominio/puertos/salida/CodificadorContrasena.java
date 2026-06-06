package com.mariposa.biblioteca.dominio.puertos.salida;

public interface CodificadorContrasena {

    String codificar(String contrasenaPlana);

    boolean coincide(String contrasenaPlana, String contrasenaCodificada);
}

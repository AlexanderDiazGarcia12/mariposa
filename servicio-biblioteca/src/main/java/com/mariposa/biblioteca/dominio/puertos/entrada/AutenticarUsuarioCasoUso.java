package com.mariposa.biblioteca.dominio.puertos.entrada;

import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.CredencialesComando;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.ResultadoAutenticacion;

public interface AutenticarUsuarioCasoUso {

    ResultadoAutenticacion iniciarSesion(CredencialesComando credenciales);

    ResultadoAutenticacion refrescarToken(String tokenRefresco);
}

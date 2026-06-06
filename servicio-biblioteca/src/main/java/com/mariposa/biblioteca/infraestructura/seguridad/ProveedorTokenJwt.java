package com.mariposa.biblioteca.infraestructura.seguridad;

import com.mariposa.biblioteca.dominio.excepciones.CredencialesInvalidas;
import com.mariposa.biblioteca.dominio.modelo.Rol;
import com.mariposa.biblioteca.dominio.modelo.Usuario;
import com.mariposa.biblioteca.dominio.puertos.salida.ProveedorToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class ProveedorTokenJwt implements ProveedorToken {

    private static final String CLAIM_NOMBRE_USUARIO = "nombre_usuario";
    private static final String CLAIM_CORREO = "correo";
    private static final String CLAIM_ROL = "rol";
    private static final String CLAIM_TIPO = "tipo";

    private final SecretKey clave;
    private final String emisor;
    private final Duration duracionAcceso;
    private final Duration duracionRefresco;
    private final Clock reloj;

    public ProveedorTokenJwt(PropiedadesJwt propiedades, Clock reloj) {
        this.clave = Keys.hmacShaKeyFor(propiedades.claveSecreta().getBytes(StandardCharsets.UTF_8));
        this.emisor = propiedades.emisor();
        this.duracionAcceso = Duration.ofMinutes(propiedades.duracionAccesoMinutos());
        this.duracionRefresco = Duration.ofDays(propiedades.duracionRefrescoDias());
        this.reloj = reloj;
    }

    @Override
    public TokenGenerado generarAcceso(Usuario usuario) {
        var emitido = reloj.instant();
        var expira = emitido.plus(duracionAcceso);
        var token = Jwts.builder()
                .issuer(emisor)
                .subject(usuario.id().toString())
                .claim(CLAIM_NOMBRE_USUARIO, usuario.nombreUsuario())
                .claim(CLAIM_CORREO, usuario.correoElectronico().valor())
                .claim(CLAIM_ROL, usuario.rol().name())
                .claim(CLAIM_TIPO, ClaimsToken.TIPO_ACCESO)
                .issuedAt(Date.from(emitido))
                .expiration(Date.from(expira))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
        return new TokenGenerado(token, expira);
    }

    @Override
    public TokenGenerado generarRefresco(Usuario usuario) {
        var emitido = reloj.instant();
        var expira = emitido.plus(duracionRefresco);
        var token = Jwts.builder()
                .issuer(emisor)
                .subject(usuario.id().toString())
                .claim(CLAIM_TIPO, ClaimsToken.TIPO_REFRESCO)
                .issuedAt(Date.from(emitido))
                .expiration(Date.from(expira))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
        return new TokenGenerado(token, expira);
    }

    @Override
    public ClaimsToken validar(String token) {
        var claims = analizarClaims(token);
        return new ClaimsToken(
                extraerIdUsuario(claims),
                claims.get(CLAIM_NOMBRE_USUARIO, String.class),
                extraerRol(claims),
                extraerTipo(claims),
                extraerExpiracion(claims)
        );
    }

    private Claims analizarClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new CredencialesInvalidas();
        }
        try {
            return Jwts.parser()
                    .verifyWith(clave)
                    .requireIssuer(emisor)
                    .clock(() -> Date.from(reloj.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new CredencialesInvalidas();
        }
    }

    private UUID extraerIdUsuario(Claims claims) {
        try {
            return UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CredencialesInvalidas();
        }
    }

    private Rol extraerRol(Claims claims) {
        var rolTexto = claims.get(CLAIM_ROL, String.class);
        if (rolTexto == null) {
            return null;
        }
        try {
            return Rol.valueOf(rolTexto);
        } catch (IllegalArgumentException e) {
            throw new CredencialesInvalidas();
        }
    }

    private String extraerTipo(Claims claims) {
        var tipo = claims.get(CLAIM_TIPO, String.class);
        if (tipo == null || tipo.isBlank()) {
            throw new CredencialesInvalidas();
        }
        return tipo;
    }

    private Instant extraerExpiracion(Claims claims) {
        var expiracion = claims.getExpiration();
        if (expiracion == null) {
            throw new CredencialesInvalidas();
        }
        return expiracion.toInstant();
    }
}

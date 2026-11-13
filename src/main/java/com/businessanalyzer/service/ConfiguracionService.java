package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.repository.ConfiguracionRepository;

import java.util.regex.Pattern;

/** Preferencias de la aplicacion guardadas en la base de datos local. */
public class ConfiguracionService {

    public static final String CLAVE_TEMA = "app.tema";
    public static final String CLAVE_ULTIMO_MODULO = "ui.ultimo_modulo";

    private static final Pattern FORMATO_CLAVE = Pattern.compile("[a-z0-9]+([._-][a-z0-9]+)*");
    private static final int LONGITUD_MAXIMA_CLAVE = 100;
    private static final int LONGITUD_MAXIMA_VALOR = 1_000;

    private final ConfiguracionRepository repository;

    public ConfiguracionService(ConfiguracionRepository repository) {
        this.repository = repository;
    }

    public String obtener(String clave, String porDefecto) {
        validarClave(clave);
        return repository.buscar(clave).orElse(porDefecto);
    }

    public void guardar(String clave, String valor) {
        validarClave(clave);
        if (valor == null) {
            throw new ValidacionException("El valor de la configuración '" + clave + "' es obligatorio");
        }
        if (valor.length() > LONGITUD_MAXIMA_VALOR) {
            throw new ValidacionException("El valor de la configuración '" + clave
                    + "' supera los " + LONGITUD_MAXIMA_VALOR + " caracteres");
        }
        repository.guardar(clave, valor);
    }

    private static void validarClave(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new ValidacionException("La clave de configuración es obligatoria");
        }
        if (clave.length() > LONGITUD_MAXIMA_CLAVE || !FORMATO_CLAVE.matcher(clave).matches()) {
            throw new ValidacionException("Clave de configuración inválida: " + clave);
        }
    }
}

package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.repository.ConfiguracionRepositoryFGEM;

import java.util.regex.Pattern;

public class ConfiguracionServiceFGEM {

    public static final String CLAVE_TEMA_FGEM = "app.tema";
    public static final String CLAVE_ULTIMO_MODULO_FGEM = "ui.ultimo_modulo";

    private static final Pattern FORMATO_CLAVE_FGEM = Pattern.compile("[a-z0-9]+([._-][a-z0-9]+)*");
    private static final int LONGITUD_MAXIMA_CLAVE_FGEM = 100;
    private static final int LONGITUD_MAXIMA_VALOR_FGEM = 1_000;

    private final ConfiguracionRepositoryFGEM repositoryFGEM;

    public ConfiguracionServiceFGEM(ConfiguracionRepositoryFGEM repositoryFGEM) {
        this.repositoryFGEM = repositoryFGEM;
    }

    public String obtenerFGEM(String claveFGEM, String porDefectoFGEM) {
        validarClaveFGEM(claveFGEM);
        return repositoryFGEM.buscarFGEM(claveFGEM).orElse(porDefectoFGEM);
    }

    public void guardarFGEM(String claveFGEM, String valorFGEM) {
        validarClaveFGEM(claveFGEM);
        if (valorFGEM == null) {
            throw new ValidacionExceptionFGEM("El valor de la configuración '" + claveFGEM + "' es obligatorio");
        }
        if (valorFGEM.length() > LONGITUD_MAXIMA_VALOR_FGEM) {
            throw new ValidacionExceptionFGEM("El valor de la configuración '" + claveFGEM
                    + "' supera los " + LONGITUD_MAXIMA_VALOR_FGEM + " caracteres");
        }
        repositoryFGEM.guardarFGEM(claveFGEM, valorFGEM);
    }

    private static void validarClaveFGEM(String claveFGEM) {
        if (claveFGEM == null || claveFGEM.isBlank()) {
            throw new ValidacionExceptionFGEM("La clave de configuración es obligatoria");
        }
        if (claveFGEM.length() > LONGITUD_MAXIMA_CLAVE_FGEM || !FORMATO_CLAVE_FGEM.matcher(claveFGEM).matches()) {
            throw new ValidacionExceptionFGEM("Clave de configuración inválida: " + claveFGEM);
        }
    }
}

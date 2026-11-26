package com.businessanalyzer.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VersionAppFGEM {

    public static final String NUMERO_FGEM = leerFGEM();

    public static final String CORTA_FGEM = cortaFGEM(NUMERO_FGEM);

    public static final String SUBTITULO_FGEM = "Desktop " + CORTA_FGEM;

    public static final String PRODUCTO_FGEM = "Business Analyzer " + SUBTITULO_FGEM;

    private static final String SIN_VERSION_FGEM = "0.0.0";

    private VersionAppFGEM() {
    }

    private static String leerFGEM() {
        try (InputStream entradaFGEM = VersionAppFGEM.class.getResourceAsStream("/version.properties")) {
            if (entradaFGEM == null) {
                return SIN_VERSION_FGEM;
            }
            Properties propiedadesFGEM = new Properties();
            propiedadesFGEM.load(entradaFGEM);
            String versionFGEM = propiedadesFGEM.getProperty("version", "").trim();
            return versionFGEM.isEmpty() || versionFGEM.contains("${") ? SIN_VERSION_FGEM : versionFGEM;
        } catch (IOException eFGEM) {
            return SIN_VERSION_FGEM;
        }
    }

    static String cortaFGEM(String versionFGEM) {
        Matcher mFGEM = Pattern.compile("^(\\d+)\\.(\\d+)").matcher(versionFGEM);
        return mFGEM.find() ? mFGEM.group(1) + "." + mFGEM.group(2) : versionFGEM;
    }
}

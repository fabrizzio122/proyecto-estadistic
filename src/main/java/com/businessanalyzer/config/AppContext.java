package com.businessanalyzer.config;

import com.businessanalyzer.repository.ConfiguracionRepository;
import com.businessanalyzer.service.ConfiguracionService;

import java.io.IOException;

/**
 * Contenedor de las dependencias compartidas de la aplicacion (inyeccion manual,
 * sin Spring). Los controladores lo reciben en su constructor mediante
 * {@link com.businessanalyzer.ui.Vistas}.
 * <p>
 * En cada fase se agregan aqui los nuevos repositorios y servicios.
 */
public final class AppContext {

    private final AppConfig config;
    private final Database database;
    private final int versionEsquema;
    private final ConfiguracionService configuracionService;

    private AppContext(AppConfig config, Database database, int versionEsquema) {
        this.config = config;
        this.database = database;
        this.versionEsquema = versionEsquema;
        this.configuracionService = new ConfiguracionService(new ConfiguracionRepository(database));
    }

    /** Prepara las carpetas, migra la base de datos y crea los servicios. */
    public static AppContext crear(AppConfig config) throws IOException {
        config.prepararDirectorios();
        Database database = new Database(config.archivoBaseDatos());
        int version = new Migrador(database).migrar();
        return new AppContext(config, database, version);
    }

    public AppConfig config() {
        return config;
    }

    public Database database() {
        return database;
    }

    public int versionEsquema() {
        return versionEsquema;
    }

    public ConfiguracionService configuracion() {
        return configuracionService;
    }
}

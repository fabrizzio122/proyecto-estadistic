package com.businessanalyzer.config;

import com.businessanalyzer.repository.CategoriaRepository;
import com.businessanalyzer.repository.ConfiguracionRepository;
import com.businessanalyzer.repository.EmpresaRepository;
import com.businessanalyzer.repository.MovimientoRepository;
import com.businessanalyzer.service.FinanzasService;
import com.businessanalyzer.service.AnalisisService;
import com.businessanalyzer.service.EstadisticaService;
import com.businessanalyzer.service.PrediccionService;
import com.businessanalyzer.service.SimulacionService;
import com.businessanalyzer.repository.EscenarioRepository;
import com.businessanalyzer.repository.OperacionRepository;
import com.businessanalyzer.repository.ProductoRepository;
import com.businessanalyzer.repository.RegistroAccesoRepository;
import com.businessanalyzer.repository.RolRepository;
import com.businessanalyzer.repository.UsuarioRepository;
import com.businessanalyzer.seguridad.PasswordHasher;
import com.businessanalyzer.seguridad.Sesion;
import com.businessanalyzer.service.AutenticacionService;
import com.businessanalyzer.service.CatalogoService;
import com.businessanalyzer.service.ConfiguracionService;
import com.businessanalyzer.service.EmpresaService;
import com.businessanalyzer.service.OperacionService;
import com.businessanalyzer.service.UsuarioService;

import java.io.IOException;
import java.time.Clock;

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
    private final Sesion sesion = new Sesion();
    private final ConfiguracionService configuracionService;
    private final AutenticacionService autenticacionService;
    private final UsuarioService usuarioService;
    private final EmpresaService empresaService;
    private final CatalogoService catalogoService;
    private final OperacionService operacionService;
    private final FinanzasService finanzasService;
    private final AnalisisService analisisService;
    private final EstadisticaService estadisticaService;
    private final PrediccionService prediccionService;
    private final SimulacionService simulacionService;

    private AppContext(AppConfig config, Database database, int versionEsquema) {
        this.config = config;
        this.database = database;
        this.versionEsquema = versionEsquema;

        Clock reloj = Clock.systemDefaultZone();
        PasswordHasher hasher = new PasswordHasher();
        RolRepository rolRepository = new RolRepository();
        UsuarioRepository usuarioRepository = new UsuarioRepository(rolRepository);
        RegistroAccesoRepository registroAccesoRepository = new RegistroAccesoRepository();

        this.configuracionService = new ConfiguracionService(new ConfiguracionRepository(database));
        this.autenticacionService = new AutenticacionService(database, usuarioRepository, rolRepository,
                registroAccesoRepository, hasher, sesion, reloj);
        this.usuarioService = new UsuarioService(database, usuarioRepository, rolRepository,
                registroAccesoRepository, hasher, sesion, reloj);
        this.empresaService = new EmpresaService(database, new EmpresaRepository(), usuarioRepository,
                configuracionService, sesion, reloj);
        ProductoRepository productoRepository = new ProductoRepository();
        this.catalogoService = new CatalogoService(database, productoRepository, new CategoriaRepository(),
                empresaService, sesion, reloj);
        this.operacionService = new OperacionService(database, new OperacionRepository(), productoRepository,
                empresaService, configuracionService, sesion, reloj);
        MovimientoRepository movimientoRepository = new MovimientoRepository();
        this.finanzasService = new FinanzasService(database, movimientoRepository, empresaService, sesion, reloj);
        OperacionRepository operacionRepository = new OperacionRepository();
        this.analisisService = new AnalisisService(database, operacionRepository, movimientoRepository,
                empresaService, sesion);
        this.estadisticaService = new EstadisticaService(database, operacionRepository, movimientoRepository,
                empresaService, sesion, reloj);
        this.prediccionService = new PrediccionService(database, operacionRepository, movimientoRepository,
                empresaService, sesion, reloj);
        this.simulacionService = new SimulacionService(database, operacionRepository, movimientoRepository,
                new EscenarioRepository(), empresaService, sesion, reloj);
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

    public Sesion sesion() {
        return sesion;
    }

    public ConfiguracionService configuracion() {
        return configuracionService;
    }

    public AutenticacionService autenticacion() {
        return autenticacionService;
    }

    public UsuarioService usuarios() {
        return usuarioService;
    }

    public EmpresaService empresas() {
        return empresaService;
    }

    public CatalogoService catalogo() {
        return catalogoService;
    }

    public OperacionService operaciones() {
        return operacionService;
    }

    public FinanzasService finanzas() {
        return finanzasService;
    }

    public AnalisisService analisis() {
        return analisisService;
    }

    public EstadisticaService estadistica() {
        return estadisticaService;
    }

    public PrediccionService predicciones() {
        return prediccionService;
    }

    public SimulacionService simulacion() {
        return simulacionService;
    }
}

package com.businessanalyzer.config;

import com.businessanalyzer.repository.CategoriaRepository;
import com.businessanalyzer.repository.TesoreriaRepository;
import com.businessanalyzer.service.TesoreriaService;
import com.businessanalyzer.service.PreferenciasService;
import com.businessanalyzer.repository.ConfiguracionRepository;
import com.businessanalyzer.repository.EmpresaRepository;
import com.businessanalyzer.repository.MovimientoRepository;
import com.businessanalyzer.service.FinanzasService;
import com.businessanalyzer.service.PatrimonioService;
import com.businessanalyzer.service.GanaderiaService;
import com.businessanalyzer.service.AgriculturaService;
import com.businessanalyzer.service.ValoracionService;
import com.businessanalyzer.service.VisitaService;
import com.businessanalyzer.service.InformeTecnicoService;
import com.businessanalyzer.repository.VisitaRepository;
import com.businessanalyzer.repository.AgriculturaRepository;
import com.businessanalyzer.service.DatosDemoAgroService;
import com.businessanalyzer.repository.GanaderiaRepository;
import com.businessanalyzer.repository.ActivoRepository;
import com.businessanalyzer.repository.DeudaRepository;
import com.businessanalyzer.service.AnalisisService;
import com.businessanalyzer.service.EstadisticaService;
import com.businessanalyzer.service.PrediccionService;
import com.businessanalyzer.service.SimulacionService;
import com.businessanalyzer.service.DatosDemoService;
import com.businessanalyzer.service.DiagnosticoService;
import com.businessanalyzer.service.InformeService;
import com.businessanalyzer.service.CopiaSeguridadService;
import com.businessanalyzer.repository.InformeRepository;
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
    private final PatrimonioService patrimonioService;
    private final GanaderiaService ganaderiaService;
    private final AgriculturaService agriculturaService;
    private final ValoracionService valoracionService;
    private final VisitaService visitaService;
    private final InformeTecnicoService informeTecnicoService;
    private final DatosDemoAgroService datosDemoAgroService;
    private final AnalisisService analisisService;
    private final EstadisticaService estadisticaService;
    private final PrediccionService prediccionService;
    private final SimulacionService simulacionService;
    private final DatosDemoService datosDemoService;
    private final DiagnosticoService diagnosticoService;
    private final InformeService informeService;
    private final TesoreriaService tesoreriaService;
    private final PreferenciasService preferenciasService;
    private final CopiaSeguridadService copiaSeguridadService;
    private final boolean restauracionAplicada;

    private AppContext(AppConfig config, Database database, int versionEsquema, boolean restauracionAplicada) {
        this.config = config;
        this.restauracionAplicada = restauracionAplicada;
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
        this.preferenciasService = new PreferenciasService(database, usuarioRepository, new EmpresaRepository(),
                configuracionService, sesion);
        ProductoRepository productoRepository = new ProductoRepository();
        this.catalogoService = new CatalogoService(database, productoRepository, new CategoriaRepository(),
                empresaService, sesion, reloj);
        this.operacionService = new OperacionService(database, new OperacionRepository(), productoRepository,
                empresaService, configuracionService, sesion, reloj);
        MovimientoRepository movimientoRepository = new MovimientoRepository();
        this.finanzasService = new FinanzasService(database, movimientoRepository, empresaService, sesion, reloj);
        OperacionRepository operacionRepository = new OperacionRepository();
        this.ganaderiaService = new GanaderiaService(database, new GanaderiaRepository(), empresaService, sesion, reloj);
        this.agriculturaService = new AgriculturaService(database, new AgriculturaRepository(), empresaService, sesion, reloj);
        this.visitaService = new VisitaService(database, new VisitaRepository(), empresaService, sesion, reloj,
                config.directorioDatos());
        this.tesoreriaService = new TesoreriaService(database, new TesoreriaRepository(), operacionRepository,
                new DeudaRepository(), empresaService, sesion, reloj);
        this.patrimonioService = new PatrimonioService(database, new ActivoRepository(), new DeudaRepository(),
                productoRepository, operacionRepository, movimientoRepository, empresaService, sesion, reloj);
        this.analisisService = new AnalisisService(database, operacionRepository, movimientoRepository,
                empresaService, sesion);
        this.estadisticaService = new EstadisticaService(database, operacionRepository, movimientoRepository,
                empresaService, sesion, reloj);
        this.prediccionService = new PrediccionService(database, operacionRepository, movimientoRepository,
                empresaService, sesion, reloj);
        this.simulacionService = new SimulacionService(database, operacionRepository, movimientoRepository,
                new EscenarioRepository(), empresaService, sesion, reloj);
        this.diagnosticoService = new DiagnosticoService(database, operacionRepository, movimientoRepository,
                empresaService, sesion, reloj);
        this.copiaSeguridadService = new CopiaSeguridadService(database, config.directorioDatos().resolve("copias"),
                configuracionService, sesion, reloj);
        this.informeService = new InformeService(database, new InformeRepository(), analisisService,
                diagnosticoService, operacionService, finanzasService, empresaService, sesion, reloj);
        this.valoracionService = new ValoracionService(database, operacionRepository, movimientoRepository,
                new InformeRepository(), empresaService, configuracionService, sesion, reloj);
        this.informeTecnicoService = new InformeTecnicoService(database, new InformeRepository(), visitaService,
                ganaderiaService, agriculturaService, patrimonioService, empresaService, sesion, reloj);
        this.datosDemoService = new DatosDemoService(empresaService, catalogoService, operacionService,
                finanzasService, patrimonioService, tesoreriaService, sesion, reloj);
        this.datosDemoAgroService = new DatosDemoAgroService(empresaService, ganaderiaService, agriculturaService, patrimonioService,
                sesion, reloj, visitaService, finanzasService, tesoreriaService);
    }

    /** Prepara las carpetas, migra la base de datos y crea los servicios. */
    public static AppContext crear(AppConfig config) throws IOException {
        config.prepararDirectorios();
        boolean restaurada = RestauracionPendiente.aplicar(config.archivoBaseDatos());
        Database database = new Database(config.archivoBaseDatos());
        int version = new Migrador(database).migrar();
        return new AppContext(config, database, version, restaurada);
    }

    private java.util.function.Consumer<com.businessanalyzer.ui.Modulo> irAModulo = m -> { };

    /** La ventana principal indica cómo abrir un módulo (lo usan los accesos del Dashboard). */
    public void alAbrirModulo(java.util.function.Consumer<com.businessanalyzer.ui.Modulo> accion) {
        this.irAModulo = accion == null ? m -> { } : accion;
    }

    public void abrirModulo(com.businessanalyzer.ui.Modulo modulo) {
        irAModulo.accept(modulo);
    }

    private java.util.function.BiConsumer<com.businessanalyzer.ui.Modulo, String> fraseModulo = (m, f) -> { };

    /** La ventana principal indica cómo mostrar la frase resumen de una pantalla de análisis. */
    public void alMostrarFrase(java.util.function.BiConsumer<com.businessanalyzer.ui.Modulo, String> accion) {
        this.fraseModulo = accion == null ? (m, f) -> { } : accion;
    }

    /**
     * Frase que resume en simple lo que muestra la pantalla (la primera de su lectura); {@code null} la oculta.
     * Se ignora si el usuario ya cambió de módulo.
     */
    public void mostrarFrase(com.businessanalyzer.ui.Modulo modulo, String frase) {
        fraseModulo.accept(modulo, frase);
    }

    private java.util.function.IntConsumer alertasDiagnostico = n -> { };

    /** La ventana principal indica cómo mostrar cuántas alertas del diagnóstico hay por atender. */
    public void alActualizarAlertas(java.util.function.IntConsumer accion) {
        this.alertasDiagnostico = accion == null ? n -> { } : accion;
    }

    /** Cantidad de indicadores en amarillo o rojo (el contador junto a «Diagnóstico empresarial»). */
    public void mostrarAlertasDiagnostico(int cantidad) {
        alertasDiagnostico.accept(cantidad);
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

    public DatosDemoAgroService datosDemoAgro() {
        return datosDemoAgroService;
    }

    public VisitaService visitas() {
        return visitaService;
    }

    public InformeTecnicoService informeTecnico() {
        return informeTecnicoService;
    }

    public ValoracionService valoracion() {
        return valoracionService;
    }

    public AgriculturaService agricultura() {
        return agriculturaService;
    }

    public GanaderiaService ganaderia() {
        return ganaderiaService;
    }

    public PatrimonioService patrimonio() {
        return patrimonioService;
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

    public DiagnosticoService diagnostico() {
        return diagnosticoService;
    }

    public CopiaSeguridadService copias() {
        return copiaSeguridadService;
    }

    /** Al iniciar se aplicó una copia de seguridad restaurada desde Configuración. */
    public boolean restauracionAplicada() {
        return restauracionAplicada;
    }

    public InformeService informes() {
        return informeService;
    }

    public DatosDemoService datosDemo() {
        return datosDemoService;
    }

    public PreferenciasService preferencias() {
        return preferenciasService;
    }

    public TesoreriaService tesoreria() {
        return tesoreriaService;
    }

    public SimulacionService simulacion() {
        return simulacionService;
    }
}

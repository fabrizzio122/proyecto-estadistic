package com.businessanalyzer.config;

import com.businessanalyzer.repository.CategoriaRepositoryFGEM;
import com.businessanalyzer.repository.TesoreriaRepositoryFGEM;
import com.businessanalyzer.service.TesoreriaServiceFGEM;
import com.businessanalyzer.service.PreferenciasServiceFGEM;
import com.businessanalyzer.repository.ConfiguracionRepositoryFGEM;
import com.businessanalyzer.repository.EmpresaRepositoryFGEM;
import com.businessanalyzer.repository.MovimientoRepositoryFGEM;
import com.businessanalyzer.service.FinanzasServiceFGEM;
import com.businessanalyzer.service.PatrimonioServiceFGEM;
import com.businessanalyzer.service.GanaderiaServiceFGEM;
import com.businessanalyzer.service.AgriculturaServiceFGEM;
import com.businessanalyzer.service.ValoracionServiceFGEM;
import com.businessanalyzer.service.VisitaServiceFGEM;
import com.businessanalyzer.service.InformeTecnicoServiceFGEM;
import com.businessanalyzer.repository.VisitaRepositoryFGEM;
import com.businessanalyzer.repository.AgriculturaRepositoryFGEM;
import com.businessanalyzer.service.DatosDemoAgroServiceFGEM;
import com.businessanalyzer.repository.GanaderiaRepositoryFGEM;
import com.businessanalyzer.repository.ActivoRepositoryFGEM;
import com.businessanalyzer.repository.DeudaRepositoryFGEM;
import com.businessanalyzer.service.AnalisisServiceFGEM;
import com.businessanalyzer.service.EstadisticaServiceFGEM;
import com.businessanalyzer.service.PrediccionServiceFGEM;
import com.businessanalyzer.service.SimulacionServiceFGEM;
import com.businessanalyzer.service.DatosDemoServiceFGEM;
import com.businessanalyzer.service.DiagnosticoServiceFGEM;
import com.businessanalyzer.service.InformeServiceFGEM;
import com.businessanalyzer.service.CopiaSeguridadServiceFGEM;
import com.businessanalyzer.repository.InformeRepositoryFGEM;
import com.businessanalyzer.repository.EscenarioRepositoryFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;
import com.businessanalyzer.repository.ProductoRepositoryFGEM;
import com.businessanalyzer.repository.RegistroAccesoRepositoryFGEM;
import com.businessanalyzer.repository.RolRepositoryFGEM;
import com.businessanalyzer.repository.UsuarioRepositoryFGEM;
import com.businessanalyzer.seguridad.PasswordHasherFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;
import com.businessanalyzer.service.AutenticacionServiceFGEM;
import com.businessanalyzer.service.CatalogoServiceFGEM;
import com.businessanalyzer.service.ConfiguracionServiceFGEM;
import com.businessanalyzer.service.EmpresaServiceFGEM;
import com.businessanalyzer.service.OperacionServiceFGEM;
import com.businessanalyzer.service.UsuarioServiceFGEM;

import java.io.IOException;
import java.time.Clock;

/**
 * Contenedor de las dependencias compartidas de la aplicacion (inyeccion manual,
 * sin Spring). Los controladores lo reciben en su constructor mediante
 * {@link com.businessanalyzer.ui.Vistas}.
 * <p>
 * En cada fase se agregan aqui los nuevos repositorios y servicios.
 */
public final class AppContextFGEM {

    private final AppConfigFGEM configFGEM;
    private final DatabaseFGEM databaseFGEM;
    private final int versionEsquemaFGEM;
    private final SesionFGEM sesionFGEM = new SesionFGEM();
    private final ConfiguracionServiceFGEM configuracionServiceFGEM;
    private final AutenticacionServiceFGEM autenticacionServiceFGEM;
    private final UsuarioServiceFGEM usuarioServiceFGEM;
    private final EmpresaServiceFGEM empresaServiceFGEM;
    private final CatalogoServiceFGEM catalogoServiceFGEM;
    private final OperacionServiceFGEM operacionServiceFGEM;
    private final FinanzasServiceFGEM finanzasServiceFGEM;
    private final PatrimonioServiceFGEM patrimonioServiceFGEM;
    private final GanaderiaServiceFGEM ganaderiaServiceFGEM;
    private final AgriculturaServiceFGEM agriculturaServiceFGEM;
    private final ValoracionServiceFGEM valoracionServiceFGEM;
    private final VisitaServiceFGEM visitaServiceFGEM;
    private final InformeTecnicoServiceFGEM informeTecnicoServiceFGEM;
    private final DatosDemoAgroServiceFGEM datosDemoAgroServiceFGEM;
    private final AnalisisServiceFGEM analisisServiceFGEM;
    private final EstadisticaServiceFGEM estadisticaServiceFGEM;
    private final PrediccionServiceFGEM prediccionServiceFGEM;
    private final SimulacionServiceFGEM simulacionServiceFGEM;
    private final DatosDemoServiceFGEM datosDemoServiceFGEM;
    private final DiagnosticoServiceFGEM diagnosticoServiceFGEM;
    private final InformeServiceFGEM informeServiceFGEM;
    private final TesoreriaServiceFGEM tesoreriaServiceFGEM;
    private final PreferenciasServiceFGEM preferenciasServiceFGEM;
    private final CopiaSeguridadServiceFGEM copiaSeguridadServiceFGEM;
    private final boolean restauracionAplicadaFGEM;

    private AppContextFGEM(AppConfigFGEM configFGEM, DatabaseFGEM databaseFGEM, int versionEsquemaFGEM, boolean restauracionAplicadaFGEM) {
        this.configFGEM = configFGEM;
        this.restauracionAplicadaFGEM = restauracionAplicadaFGEM;
        this.databaseFGEM = databaseFGEM;
        this.versionEsquemaFGEM = versionEsquemaFGEM;

        Clock relojFGEM = Clock.systemDefaultZone();
        PasswordHasherFGEM hasherFGEM = new PasswordHasherFGEM();
        RolRepositoryFGEM rolRepositoryFGEM = new RolRepositoryFGEM();
        UsuarioRepositoryFGEM usuarioRepositoryFGEM = new UsuarioRepositoryFGEM(rolRepositoryFGEM);
        RegistroAccesoRepositoryFGEM registroAccesoRepositoryFGEM = new RegistroAccesoRepositoryFGEM();

        this.configuracionServiceFGEM = new ConfiguracionServiceFGEM(new ConfiguracionRepositoryFGEM(databaseFGEM));
        this.autenticacionServiceFGEM = new AutenticacionServiceFGEM(databaseFGEM, usuarioRepositoryFGEM, rolRepositoryFGEM,
                registroAccesoRepositoryFGEM, hasherFGEM, sesionFGEM, relojFGEM);
        this.usuarioServiceFGEM = new UsuarioServiceFGEM(databaseFGEM, usuarioRepositoryFGEM, rolRepositoryFGEM,
                registroAccesoRepositoryFGEM, hasherFGEM, sesionFGEM, relojFGEM);
        this.empresaServiceFGEM = new EmpresaServiceFGEM(databaseFGEM, new EmpresaRepositoryFGEM(), usuarioRepositoryFGEM,
                configuracionServiceFGEM, sesionFGEM, relojFGEM);
        this.preferenciasServiceFGEM = new PreferenciasServiceFGEM(databaseFGEM, usuarioRepositoryFGEM, new EmpresaRepositoryFGEM(),
                configuracionServiceFGEM, sesionFGEM);
        ProductoRepositoryFGEM productoRepositoryFGEM = new ProductoRepositoryFGEM();
        this.catalogoServiceFGEM = new CatalogoServiceFGEM(databaseFGEM, productoRepositoryFGEM, new CategoriaRepositoryFGEM(),
                empresaServiceFGEM, sesionFGEM, relojFGEM);
        this.operacionServiceFGEM = new OperacionServiceFGEM(databaseFGEM, new OperacionRepositoryFGEM(), productoRepositoryFGEM,
                empresaServiceFGEM, configuracionServiceFGEM, sesionFGEM, relojFGEM);
        MovimientoRepositoryFGEM movimientoRepositoryFGEM = new MovimientoRepositoryFGEM();
        this.finanzasServiceFGEM = new FinanzasServiceFGEM(databaseFGEM, movimientoRepositoryFGEM, empresaServiceFGEM, sesionFGEM, relojFGEM);
        OperacionRepositoryFGEM operacionRepositoryFGEM = new OperacionRepositoryFGEM();
        this.ganaderiaServiceFGEM = new GanaderiaServiceFGEM(databaseFGEM, new GanaderiaRepositoryFGEM(), empresaServiceFGEM, sesionFGEM, relojFGEM);
        this.agriculturaServiceFGEM = new AgriculturaServiceFGEM(databaseFGEM, new AgriculturaRepositoryFGEM(), empresaServiceFGEM, sesionFGEM, relojFGEM);
        this.visitaServiceFGEM = new VisitaServiceFGEM(databaseFGEM, new VisitaRepositoryFGEM(), empresaServiceFGEM, sesionFGEM, relojFGEM,
                configFGEM.directorioDatosFGEM());
        this.tesoreriaServiceFGEM = new TesoreriaServiceFGEM(databaseFGEM, new TesoreriaRepositoryFGEM(), operacionRepositoryFGEM,
                new DeudaRepositoryFGEM(), empresaServiceFGEM, sesionFGEM, relojFGEM);
        this.patrimonioServiceFGEM = new PatrimonioServiceFGEM(databaseFGEM, new ActivoRepositoryFGEM(), new DeudaRepositoryFGEM(),
                productoRepositoryFGEM, operacionRepositoryFGEM, movimientoRepositoryFGEM, empresaServiceFGEM, sesionFGEM, relojFGEM);
        this.analisisServiceFGEM = new AnalisisServiceFGEM(databaseFGEM, operacionRepositoryFGEM, movimientoRepositoryFGEM,
                empresaServiceFGEM, sesionFGEM);
        this.estadisticaServiceFGEM = new EstadisticaServiceFGEM(databaseFGEM, operacionRepositoryFGEM, movimientoRepositoryFGEM,
                empresaServiceFGEM, sesionFGEM, relojFGEM);
        this.prediccionServiceFGEM = new PrediccionServiceFGEM(databaseFGEM, operacionRepositoryFGEM, movimientoRepositoryFGEM,
                empresaServiceFGEM, sesionFGEM, relojFGEM);
        this.simulacionServiceFGEM = new SimulacionServiceFGEM(databaseFGEM, operacionRepositoryFGEM, movimientoRepositoryFGEM,
                new EscenarioRepositoryFGEM(), empresaServiceFGEM, sesionFGEM, relojFGEM);
        this.diagnosticoServiceFGEM = new DiagnosticoServiceFGEM(databaseFGEM, operacionRepositoryFGEM, movimientoRepositoryFGEM,
                empresaServiceFGEM, sesionFGEM, relojFGEM);
        this.copiaSeguridadServiceFGEM = new CopiaSeguridadServiceFGEM(databaseFGEM, configFGEM.directorioDatosFGEM().resolve("copias"),
                configuracionServiceFGEM, sesionFGEM, relojFGEM);
        this.informeServiceFGEM = new InformeServiceFGEM(databaseFGEM, new InformeRepositoryFGEM(), analisisServiceFGEM,
                diagnosticoServiceFGEM, operacionServiceFGEM, finanzasServiceFGEM, empresaServiceFGEM, sesionFGEM, relojFGEM);
        this.valoracionServiceFGEM = new ValoracionServiceFGEM(databaseFGEM, operacionRepositoryFGEM, movimientoRepositoryFGEM,
                new InformeRepositoryFGEM(), empresaServiceFGEM, configuracionServiceFGEM, sesionFGEM, relojFGEM);
        this.informeTecnicoServiceFGEM = new InformeTecnicoServiceFGEM(databaseFGEM, new InformeRepositoryFGEM(), visitaServiceFGEM,
                ganaderiaServiceFGEM, agriculturaServiceFGEM, patrimonioServiceFGEM, empresaServiceFGEM, sesionFGEM, relojFGEM);
        this.datosDemoServiceFGEM = new DatosDemoServiceFGEM(empresaServiceFGEM, catalogoServiceFGEM, operacionServiceFGEM,
                finanzasServiceFGEM, patrimonioServiceFGEM, tesoreriaServiceFGEM, sesionFGEM, relojFGEM);
        this.datosDemoAgroServiceFGEM = new DatosDemoAgroServiceFGEM(empresaServiceFGEM, ganaderiaServiceFGEM, agriculturaServiceFGEM, patrimonioServiceFGEM,
                sesionFGEM, relojFGEM, visitaServiceFGEM, finanzasServiceFGEM, tesoreriaServiceFGEM);
    }

    /** Prepara las carpetas, migra la base de datos y crea los servicios. */
    public static AppContextFGEM crearFGEM(AppConfigFGEM configFGEM) throws IOException {
        configFGEM.prepararDirectoriosFGEM();
        boolean restauradaFGEM = RestauracionPendienteFGEM.aplicarFGEM(configFGEM.archivoBaseDatosFGEM());
        DatabaseFGEM databaseFGEM = new DatabaseFGEM(configFGEM.archivoBaseDatosFGEM());
        int versionFGEM = new MigradorFGEM(databaseFGEM).migrarFGEM();
        return new AppContextFGEM(configFGEM, databaseFGEM, versionFGEM, restauradaFGEM);
    }

    private java.util.function.Consumer<com.businessanalyzer.ui.ModuloFGEM> irAModuloFGEM = mFGEM -> { };

    /** La ventana principal indica cómo abrir un módulo (lo usan los accesos del Dashboard). */
    public void alAbrirModuloFGEM(java.util.function.Consumer<com.businessanalyzer.ui.ModuloFGEM> accionFGEM) {
        this.irAModuloFGEM = accionFGEM == null ? mFGEM -> { } : accionFGEM;
    }

    public void abrirModuloFGEM(com.businessanalyzer.ui.ModuloFGEM moduloFGEM) {
        irAModuloFGEM.accept(moduloFGEM);
    }

    private java.util.function.BiConsumer<com.businessanalyzer.ui.ModuloFGEM, String> fraseModuloFGEM = (mFGEM, fFGEM) -> { };

    /** La ventana principal indica cómo mostrar la frase resumen de una pantalla de análisis. */
    public void alMostrarFraseFGEM(java.util.function.BiConsumer<com.businessanalyzer.ui.ModuloFGEM, String> accionFGEM) {
        this.fraseModuloFGEM = accionFGEM == null ? (mFGEM, fFGEM) -> { } : accionFGEM;
    }

    /**
     * Frase que resume en simple lo que muestra la pantalla (la primera de su lectura); {@code null} la oculta.
     * Se ignora si el usuario ya cambió de módulo.
     */
    public void mostrarFraseFGEM(com.businessanalyzer.ui.ModuloFGEM moduloFGEM, String fraseFGEM) {
        fraseModuloFGEM.accept(moduloFGEM, fraseFGEM);
    }

    private java.util.function.IntConsumer alertasDiagnosticoFGEM = nFGEM -> { };

    /** La ventana principal indica cómo mostrar cuántas alertas del diagnóstico hay por atender. */
    public void alActualizarAlertasFGEM(java.util.function.IntConsumer accionFGEM) {
        this.alertasDiagnosticoFGEM = accionFGEM == null ? nFGEM -> { } : accionFGEM;
    }

    /** Cantidad de indicadores en amarillo o rojo (el contador junto a «Diagnóstico empresarial»). */
    public void mostrarAlertasDiagnosticoFGEM(int cantidadFGEM) {
        alertasDiagnosticoFGEM.accept(cantidadFGEM);
    }

    public AppConfigFGEM configFGEM() {
        return configFGEM;
    }

    public DatabaseFGEM databaseFGEM() {
        return databaseFGEM;
    }

    public int versionEsquemaFGEM() {
        return versionEsquemaFGEM;
    }

    public SesionFGEM sesionFGEM() {
        return sesionFGEM;
    }

    public ConfiguracionServiceFGEM configuracionFGEM() {
        return configuracionServiceFGEM;
    }

    public AutenticacionServiceFGEM autenticacionFGEM() {
        return autenticacionServiceFGEM;
    }

    public UsuarioServiceFGEM usuariosFGEM() {
        return usuarioServiceFGEM;
    }

    public EmpresaServiceFGEM empresasFGEM() {
        return empresaServiceFGEM;
    }

    public CatalogoServiceFGEM catalogoFGEM() {
        return catalogoServiceFGEM;
    }

    public OperacionServiceFGEM operacionesFGEM() {
        return operacionServiceFGEM;
    }

    public FinanzasServiceFGEM finanzasFGEM() {
        return finanzasServiceFGEM;
    }

    public DatosDemoAgroServiceFGEM datosDemoAgroFGEM() {
        return datosDemoAgroServiceFGEM;
    }

    public VisitaServiceFGEM visitasFGEM() {
        return visitaServiceFGEM;
    }

    public InformeTecnicoServiceFGEM informeTecnicoFGEM() {
        return informeTecnicoServiceFGEM;
    }

    public ValoracionServiceFGEM valoracionFGEM() {
        return valoracionServiceFGEM;
    }

    public AgriculturaServiceFGEM agriculturaFGEM() {
        return agriculturaServiceFGEM;
    }

    public GanaderiaServiceFGEM ganaderiaFGEM() {
        return ganaderiaServiceFGEM;
    }

    public PatrimonioServiceFGEM patrimonioFGEM() {
        return patrimonioServiceFGEM;
    }

    public AnalisisServiceFGEM analisisFGEM() {
        return analisisServiceFGEM;
    }

    public EstadisticaServiceFGEM estadisticaFGEM() {
        return estadisticaServiceFGEM;
    }

    public PrediccionServiceFGEM prediccionesFGEM() {
        return prediccionServiceFGEM;
    }

    public DiagnosticoServiceFGEM diagnosticoFGEM() {
        return diagnosticoServiceFGEM;
    }

    public CopiaSeguridadServiceFGEM copiasFGEM() {
        return copiaSeguridadServiceFGEM;
    }

    /** Al iniciar se aplicó una copia de seguridad restaurada desde Configuración. */
    public boolean restauracionAplicadaFGEM() {
        return restauracionAplicadaFGEM;
    }

    public InformeServiceFGEM informesFGEM() {
        return informeServiceFGEM;
    }

    public DatosDemoServiceFGEM datosDemoFGEM() {
        return datosDemoServiceFGEM;
    }

    public PreferenciasServiceFGEM preferenciasFGEM() {
        return preferenciasServiceFGEM;
    }

    public TesoreriaServiceFGEM tesoreriaFGEM() {
        return tesoreriaServiceFGEM;
    }

    public SimulacionServiceFGEM simulacionFGEM() {
        return simulacionServiceFGEM;
    }
}

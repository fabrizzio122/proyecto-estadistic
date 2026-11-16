package com.businessanalyzer;

import com.businessanalyzer.config.AppConfigFGEM;
import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.ui.AnimacionesFGEM;
import com.businessanalyzer.ui.ColorAcentoFGEM;
import com.businessanalyzer.ui.DialogosFGEM;
import com.businessanalyzer.ui.EstilosFGEM;
import com.businessanalyzer.ui.NotificadorFGEM;
import com.businessanalyzer.ui.VentanaFGEM;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ciclo de vida de la aplicacion JavaFX.
 * <ul>
 *     <li>{@link #init()} corre fuera del hilo grafico: prepara carpetas y migra la base de datos.</li>
 *     <li>{@link #start(Stage)} abre la ventana en la pantalla de acceso.</li>
 *     <li>{@link #stop()} registra el cierre de sesion al salir.</li>
 * </ul>
 */
public class BusinessAnalyzerAppFGEM extends Application {

    public static final String NOMBRE_FGEM = "Business Analyzer";
    public static final String VERSION_FGEM = com.businessanalyzer.config.VersionAppFGEM.NUMERO_FGEM;

    private static final Logger logFGEM = LoggerFactory.getLogger(BusinessAnalyzerAppFGEM.class);

    private AppContextFGEM contextoFGEM;
    private Exception errorInicioFGEM;

    @Override
    public void init() {
        try {
            contextoFGEM = AppContextFGEM.crearFGEM(AppConfigFGEM.desdeSistemaFGEM());
            logFGEM.info("{} {} iniciado. Base de datos: {}", NOMBRE_FGEM, VERSION_FGEM,
                    contextoFGEM.configFGEM().archivoBaseDatosFGEM());
            contextoFGEM.copiasFGEM().crearAutomaticaSiCorrespondeFGEM();
        } catch (Exception eFGEM) {
            logFGEM.error("No se pudo inicializar la aplicacion", eFGEM);
            errorInicioFGEM = eFGEM;
        }
    }

    @Override
    public void start(Stage stageFGEM) {
        if (errorInicioFGEM != null) {
            DialogosFGEM.errorFGEM(null, "No se pudo iniciar " + NOMBRE_FGEM,
                    "Ocurrió un problema al preparar la base de datos local.", errorInicioFGEM);
            Platform.exit();
            return;
        }

        Thread.setDefaultUncaughtExceptionHandler((hiloFGEM, errorFGEM) -> {
            logFGEM.error("Error no controlado en el hilo {}", hiloFGEM.getName(), errorFGEM);
            NotificadorFGEM.errorFGEM("Ocurrió un error inesperado. El detalle quedó registrado en el log.");
        });

        // El inicio de sesión toma el último color usado en este equipo (cada usuario aplica el suyo al entrar).
        try {
            contextoFGEM.preferenciasFGEM().ultimoColorFGEM().flatMap(ColorAcentoFGEM::desdeFGEM).ifPresent(EstilosFGEM::cambiarAcentoFGEM);
            AnimacionesFGEM.reducirFGEM(contextoFGEM.preferenciasFGEM().animacionesReducidasFGEM());
        } catch (RuntimeException eFGEM) {
            logFGEM.warn("No se pudieron leer las preferencias visuales", eFGEM);
        }

        try {
            VentanaFGEM ventanaFGEM = new VentanaFGEM(stageFGEM, contextoFGEM);
            ventanaFGEM.mostrarAccesoFGEM();
            stageFGEM.setTitle(NOMBRE_FGEM + " " + VERSION_FGEM);
            VentanaFGEM.aplicarIconoFGEM(stageFGEM);
            stageFGEM.setMinWidth(1024);
            stageFGEM.setMinHeight(680);
            stageFGEM.show();
            if (contextoFGEM.restauracionAplicadaFGEM()) {
                DialogosFGEM.informacionFGEM(stageFGEM, "Copia de seguridad restaurada",
                        "La base de datos se reemplazó por la copia elegida. La copia del estado anterior quedó "
                                + "guardada en la carpeta de copias de seguridad.", null);
            }
        } catch (Exception eFGEM) {
            logFGEM.error("No se pudo construir la ventana principal", eFGEM);
            DialogosFGEM.errorFGEM(null, "No se pudo iniciar " + NOMBRE_FGEM,
                    "No se pudo cargar la ventana principal.", eFGEM);
            Platform.exit();
        }
    }

    @Override
    public void stop() {
        if (contextoFGEM != null) {
            contextoFGEM.autenticacionFGEM().cerrarSesionFGEM();
        }
    }
}

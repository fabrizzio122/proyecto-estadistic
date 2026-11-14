package com.businessanalyzer;

import com.businessanalyzer.config.AppConfig;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.ui.Dialogos;
import com.businessanalyzer.ui.Notificador;
import com.businessanalyzer.ui.Ventana;
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
public class BusinessAnalyzerApp extends Application {

    public static final String NOMBRE = "Business Analyzer";
    public static final String VERSION = com.businessanalyzer.config.VersionApp.NUMERO;

    private static final Logger log = LoggerFactory.getLogger(BusinessAnalyzerApp.class);

    private AppContext contexto;
    private Exception errorInicio;

    @Override
    public void init() {
        try {
            contexto = AppContext.crear(AppConfig.desdeSistema());
            log.info("{} {} iniciado. Base de datos: {}", NOMBRE, VERSION,
                    contexto.config().archivoBaseDatos());
            contexto.copias().crearAutomaticaSiCorresponde();
        } catch (Exception e) {
            log.error("No se pudo inicializar la aplicacion", e);
            errorInicio = e;
        }
    }

    @Override
    public void start(Stage stage) {
        if (errorInicio != null) {
            Dialogos.error(null, "No se pudo iniciar " + NOMBRE,
                    "Ocurrió un problema al preparar la base de datos local.", errorInicio);
            Platform.exit();
            return;
        }

        Thread.setDefaultUncaughtExceptionHandler((hilo, error) -> {
            log.error("Error no controlado en el hilo {}", hilo.getName(), error);
            Notificador.error("Ocurrió un error inesperado. El detalle quedó registrado en el log.");
        });

        try {
            Ventana ventana = new Ventana(stage, contexto);
            ventana.mostrarAcceso();
            stage.setTitle(NOMBRE + " " + VERSION);
            Ventana.aplicarIcono(stage);
            stage.setMinWidth(1024);
            stage.setMinHeight(680);
            stage.show();
            if (contexto.restauracionAplicada()) {
                Dialogos.informacion(stage, "Copia de seguridad restaurada",
                        "La base de datos se reemplazó por la copia elegida. La copia del estado anterior quedó "
                                + "guardada en la carpeta de copias de seguridad.", null);
            }
        } catch (Exception e) {
            log.error("No se pudo construir la ventana principal", e);
            Dialogos.error(null, "No se pudo iniciar " + NOMBRE,
                    "No se pudo cargar la ventana principal.", e);
            Platform.exit();
        }
    }

    @Override
    public void stop() {
        if (contexto != null) {
            contexto.autenticacion().cerrarSesion();
        }
    }
}

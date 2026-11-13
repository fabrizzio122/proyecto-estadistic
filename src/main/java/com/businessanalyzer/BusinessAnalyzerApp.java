package com.businessanalyzer;

import com.businessanalyzer.config.AppConfig;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.ui.Dialogos;
import com.businessanalyzer.ui.Estilos;
import com.businessanalyzer.ui.Notificador;
import com.businessanalyzer.ui.Vistas;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ciclo de vida de la aplicacion JavaFX.
 * <ul>
 *     <li>{@link #init()} corre fuera del hilo grafico: prepara carpetas y migra la base de datos.</li>
 *     <li>{@link #start(Stage)} construye la ventana principal.</li>
 * </ul>
 */
public class BusinessAnalyzerApp extends Application {

    public static final String NOMBRE = "Business Analyzer";
    public static final String VERSION = "1.0.0";

    private static final Logger log = LoggerFactory.getLogger(BusinessAnalyzerApp.class);

    private AppContext contexto;
    private Exception errorInicio;

    @Override
    public void init() {
        try {
            contexto = AppContext.crear(AppConfig.desdeSistema());
            log.info("{} {} iniciado. Base de datos: {}", NOMBRE, VERSION,
                    contexto.config().archivoBaseDatos());
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
            Parent raiz = Vistas.cargador(Vistas.PRINCIPAL, contexto).load();
            Scene escena = new Scene(raiz, 1280, 800);
            Estilos.aplicar(escena);

            stage.setTitle(NOMBRE + " " + VERSION);
            stage.setMinWidth(1024);
            stage.setMinHeight(680);
            stage.setScene(escena);
            stage.show();
        } catch (Exception e) {
            log.error("No se pudo construir la ventana principal", e);
            Dialogos.error(null, "No se pudo iniciar " + NOMBRE,
                    "No se pudo cargar la ventana principal.", e);
            Platform.exit();
        }
    }
}

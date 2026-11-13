package com.businessanalyzer.ui;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Ejecuta trabajo lento (hash de contraseñas, consultas pesadas, cálculos) fuera del hilo
 * gráfico para que la ventana no se congele. Los callbacks se ejecutan en el hilo gráfico.
 */
public final class Tareas {

    private static final ExecutorService EJECUTOR = Executors.newCachedThreadPool(trabajo -> {
        Thread hilo = new Thread(trabajo, "tarea-segundo-plano");
        hilo.setDaemon(true);
        return hilo;
    });

    private Tareas() {
    }

    public static <T> void ejecutar(Callable<T> trabajo, Consumer<T> alTerminar, Consumer<Throwable> alFallar) {
        Task<T> tarea = new Task<>() {
            @Override
            protected T call() throws Exception {
                return trabajo.call();
            }
        };
        tarea.setOnSucceeded(e -> alTerminar.accept(tarea.getValue()));
        tarea.setOnFailed(e -> alFallar.accept(tarea.getException()));
        EJECUTOR.execute(tarea);
    }
}

package com.businessanalyzer.ui;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class TareasFGEM {

    private static final ExecutorService EJECUTOR_FGEM = Executors.newCachedThreadPool(trabajoFGEM -> {
        Thread hiloFGEM = new Thread(trabajoFGEM, "tarea-segundo-plano");
        hiloFGEM.setDaemon(true);
        return hiloFGEM;
    });

    private TareasFGEM() {
    }

    public static <T> void ejecutarFGEM(Callable<T> trabajoFGEM, Consumer<T> alTerminarFGEM, Consumer<Throwable> alFallarFGEM) {
        Task<T> tareaFGEM = new Task<>() {
            @Override
            protected T call() throws Exception {
                return trabajoFGEM.call();
            }
        };
        tareaFGEM.setOnSucceeded(eFGEM -> alTerminarFGEM.accept(tareaFGEM.getValue()));
        tareaFGEM.setOnFailed(eFGEM -> alFallarFGEM.accept(tareaFGEM.getException()));
        EJECUTOR_FGEM.execute(tareaFGEM);
    }
}

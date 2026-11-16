package com.businessanalyzer.ui;

/**
 * Contrato opcional para los controladores de modulo. El {@link NavegadorFGEM} llama a estos
 * metodos si el controlador lo implementa.
 */
public interface VistaModuloFGEM {

    /** Se llama una sola vez, justo despues de cargar el FXML. */
    default void inicializarFGEM(ModuloFGEM moduloFGEM) {
    }

    /** Se llama cada vez que el usuario abre el modulo (util para refrescar datos). */
    default void alMostrarFGEM() {
    }
}

package com.businessanalyzer.ui;

/**
 * Contrato opcional para los controladores de modulo. El {@link Navegador} llama a estos
 * metodos si el controlador lo implementa.
 */
public interface VistaModulo {

    /** Se llama una sola vez, justo despues de cargar el FXML. */
    default void inicializar(Modulo modulo) {
    }

    /** Se llama cada vez que el usuario abre el modulo (util para refrescar datos). */
    default void alMostrar() {
    }
}

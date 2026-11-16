package com.businessanalyzer.ui;

import com.businessanalyzer.model.CuentaDinero;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lista desplegable para elegir la cuenta de dinero (caja, banco, Yape…) de un registro. Puede incluir
 * una opción «ninguna» para los registros que no mueven dinero (por ejemplo, un activo que ya se tenía).
 */
public final class SelectorCuenta extends ComboBox<SelectorCuenta.Opcion> {

    /** Una cuenta, o la opción sin cuenta ({@code id == null}). */
    public record Opcion(Long id, String texto) {
        @Override
        public String toString() {
            return texto;
        }
    }

    /**
     * @param cuentas      cuentas activas de la empresa (la primera es la principal)
     * @param sinCuenta    texto de la opción «ninguna»; {@code null} si siempre hay que elegir una cuenta
     * @param seleccionada cuenta elegida al abrir; {@code null} = la opción «ninguna» o, si no existe, la principal
     */
    public SelectorCuenta(List<CuentaDinero> cuentas, String sinCuenta, Long seleccionada) {
        List<Opcion> opciones = new ArrayList<>();
        if (sinCuenta != null) {
            opciones.add(new Opcion(null, sinCuenta));
        }
        for (CuentaDinero c : cuentas) {
            opciones.add(new Opcion(c.id(), c.nombre() + "  ·  " + switch (c.tipo()) {
                case CAJA -> "efectivo";
                case BANCO -> "banco";
                case BILLETERA -> "billetera digital";
            }));
        }
        // Una cuenta ya desactivada que el registro usaba se muestra para no perderla al editar.
        if (seleccionada != null && opciones.stream().noneMatch(o -> Objects.equals(o.id(), seleccionada))) {
            opciones.add(new Opcion(seleccionada, "Cuenta desactivada"));
        }
        setItems(FXCollections.observableArrayList(opciones));
        setMaxWidth(Double.MAX_VALUE);
        setVisibleRowCount(8);
        setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Opcion o, boolean vacio) {
                super.updateItem(o, vacio);
                setText(vacio || o == null ? null : o.texto());
            }
        });
        Opcion inicial = opciones.stream().filter(o -> Objects.equals(o.id(), seleccionada)).findFirst()
                .orElse(opciones.isEmpty() ? null : opciones.get(0));
        setValue(inicial);
    }

    /** Cuenta elegida; {@code null} si se eligió «ninguna» o no hay cuentas. */
    public Long cuentaId() {
        return getValue() == null ? null : getValue().id();
    }
}

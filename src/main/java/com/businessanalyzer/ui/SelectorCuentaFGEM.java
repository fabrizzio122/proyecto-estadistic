package com.businessanalyzer.ui;

import com.businessanalyzer.model.CuentaDineroFGEM;
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
public final class SelectorCuentaFGEM extends ComboBox<SelectorCuentaFGEM.OpcionFGEM> {

    /** Una cuenta, o la opción sin cuenta ({@code id == null}). */
    public record OpcionFGEM(Long idFGEM, String textoFGEM) {
        @Override
        public String toString() {
            return textoFGEM;
        }
    }

    /**
     * @param cuentasFGEM      cuentas activas de la empresa (la primera es la principal)
     * @param sinCuentaFGEM    texto de la opción «ninguna»; {@code null} si siempre hay que elegir una cuenta
     * @param seleccionadaFGEM cuenta elegida al abrir; {@code null} = la opción «ninguna» o, si no existe, la principal
     */
    public SelectorCuentaFGEM(List<CuentaDineroFGEM> cuentasFGEM, String sinCuentaFGEM, Long seleccionadaFGEM) {
        List<OpcionFGEM> opcionesFGEM = new ArrayList<>();
        if (sinCuentaFGEM != null) {
            opcionesFGEM.add(new OpcionFGEM(null, sinCuentaFGEM));
        }
        for (CuentaDineroFGEM cFGEM : cuentasFGEM) {
            opcionesFGEM.add(new OpcionFGEM(cFGEM.idFGEM(), cFGEM.nombreFGEM() + "  ·  " + switch (cFGEM.tipoFGEM()) {
                case CAJA -> "efectivo";
                case BANCO -> "banco";
                case BILLETERA -> "billetera digital";
            }));
        }
        // Una cuenta ya desactivada que el registro usaba se muestra para no perderla al editar.
        if (seleccionadaFGEM != null && opcionesFGEM.stream().noneMatch(oFGEM -> Objects.equals(oFGEM.idFGEM(), seleccionadaFGEM))) {
            opcionesFGEM.add(new OpcionFGEM(seleccionadaFGEM, "Cuenta desactivada"));
        }
        setItems(FXCollections.observableArrayList(opcionesFGEM));
        setMaxWidth(Double.MAX_VALUE);
        setVisibleRowCount(8);
        setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(OpcionFGEM oFGEM, boolean vacioFGEM) {
                super.updateItem(oFGEM, vacioFGEM);
                setText(vacioFGEM || oFGEM == null ? null : oFGEM.textoFGEM());
            }
        });
        OpcionFGEM inicialFGEM = opcionesFGEM.stream().filter(oFGEM -> Objects.equals(oFGEM.idFGEM(), seleccionadaFGEM)).findFirst()
                .orElse(opcionesFGEM.isEmpty() ? null : opcionesFGEM.get(0));
        setValue(inicialFGEM);
    }

    /** Cuenta elegida; {@code null} si se eligió «ninguna» o no hay cuentas. */
    public Long cuentaIdFGEM() {
        return getValue() == null ? null : getValue().idFGEM();
    }
}

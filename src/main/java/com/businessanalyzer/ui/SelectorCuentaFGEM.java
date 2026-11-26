package com.businessanalyzer.ui;

import com.businessanalyzer.model.CuentaDineroFGEM;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SelectorCuentaFGEM extends ComboBox<SelectorCuentaFGEM.OpcionFGEM> {

    public record OpcionFGEM(Long idFGEM, String textoFGEM) {
        @Override
        public String toString() {
            return textoFGEM;
        }
    }

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

    public Long cuentaIdFGEM() {
        return getValue() == null ? null : getValue().idFGEM();
    }
}

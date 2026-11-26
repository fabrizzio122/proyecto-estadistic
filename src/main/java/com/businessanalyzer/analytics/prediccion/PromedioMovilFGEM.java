package com.businessanalyzer.analytics.prediccion;

public final class PromedioMovilFGEM implements MetodoPronosticoFGEM {

    private final int ventanaFGEM;

    public PromedioMovilFGEM(int ventanaFGEM) {
        if (ventanaFGEM < 1) {
            throw new IllegalArgumentException("La ventana debe ser positiva");
        }
        this.ventanaFGEM = ventanaFGEM;
    }

    @Override
    public String nombreFGEM() {
        return "Promedio móvil de " + ventanaFGEM + " meses";
    }

    @Override
    public String explicacionFGEM() {
        return "Supone que los próximos meses serán como el promedio de los últimos " + ventanaFGEM
                + ". Es el más prudente cuando las ventas suben y bajan sin una dirección clara.";
    }

    @Override
    public double[] pronosticarFGEM(double[] historiaFGEM, int horizonteFGEM) {
        int kFGEM = Math.min(ventanaFGEM, historiaFGEM.length);
        double sumaFGEM = 0;
        for (int iFGEM = historiaFGEM.length - kFGEM; iFGEM < historiaFGEM.length; iFGEM++) {
            sumaFGEM += historiaFGEM[iFGEM];
        }
        double[] pronosticoFGEM = new double[horizonteFGEM];
        java.util.Arrays.fill(pronosticoFGEM, sumaFGEM / kFGEM);
        return pronosticoFGEM;
    }
}

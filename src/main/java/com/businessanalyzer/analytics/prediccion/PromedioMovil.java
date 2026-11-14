package com.businessanalyzer.analytics.prediccion;

/** Proyecta el promedio de los últimos k meses (pronóstico plano). */
public final class PromedioMovil implements MetodoPronostico {

    private final int ventana;

    public PromedioMovil(int ventana) {
        if (ventana < 1) {
            throw new IllegalArgumentException("La ventana debe ser positiva");
        }
        this.ventana = ventana;
    }

    @Override
    public String nombre() {
        return "Promedio móvil de " + ventana + " meses";
    }

    @Override
    public String explicacion() {
        return "Supone que los próximos meses serán como el promedio de los últimos " + ventana
                + ". Es el más prudente cuando las ventas suben y bajan sin una dirección clara.";
    }

    @Override
    public double[] pronosticar(double[] historia, int horizonte) {
        int k = Math.min(ventana, historia.length);
        double suma = 0;
        for (int i = historia.length - k; i < historia.length; i++) {
            suma += historia[i];
        }
        double[] pronostico = new double[horizonte];
        java.util.Arrays.fill(pronostico, suma / k);
        return pronostico;
    }
}

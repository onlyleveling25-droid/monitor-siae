package co.monitorsiae.dominio;

public enum TonoAlarma {
    SIRENA("Sirena clásica (agudo y grave)", 880, 660, 520, 390),
    DOBLE("Doble tono (agudo y grave)", 1047, 784, 659, 523),
    SUAVE("Tono suave (agudo y grave)", 698, 523, 440, 349);

    private final String etiqueta;
    private final double oportunidadAguda;
    private final double oportunidadGrave;
    private final double problemaAguda;
    private final double problemaGrave;

    TonoAlarma(String etiqueta, double oportunidadAguda, double oportunidadGrave,
               double problemaAguda, double problemaGrave) {
        this.etiqueta = etiqueta;
        this.oportunidadAguda = oportunidadAguda;
        this.oportunidadGrave = oportunidadGrave;
        this.problemaAguda = problemaAguda;
        this.problemaGrave = problemaGrave;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public double frecuenciaAguda(boolean oportunidad) {
        return oportunidad ? oportunidadAguda : problemaAguda;
    }

    public double frecuenciaGrave(boolean oportunidad) {
        return oportunidad ? oportunidadGrave : problemaGrave;
    }
}

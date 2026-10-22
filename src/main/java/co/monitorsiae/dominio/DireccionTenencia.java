package co.monitorsiae.dominio;

/**
 * Direccion exigida por el modal de TENENCIA. Solo se usa cuando la modalidad es TENENCIA.
 */
public record DireccionTenencia(String departamento, String ciudad, String direccion) {

    public DireccionTenencia {
        departamento = Listas.texto(departamento);
        ciudad = Listas.texto(ciudad);
        direccion = direccion == null ? "" : direccion.strip();
    }

    public static DireccionTenencia vacia() {
        return new DireccionTenencia("", "", "");
    }

    public boolean completa() {
        return !departamento.isEmpty() && !ciudad.isEmpty() && !direccion.isEmpty();
    }
}
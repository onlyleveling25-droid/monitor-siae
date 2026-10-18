package co.monitorsiae.dominio;

public enum PreferenciaSonidoCliente {
    HEREDAR,
    ACTIVADO,
    DESACTIVADO;

    public boolean habilitado(boolean valorGeneral) {
        return switch (this) {
            case HEREDAR -> valorGeneral;
            case ACTIVADO -> true;
            case DESACTIVADO -> false;
        };
    }
}

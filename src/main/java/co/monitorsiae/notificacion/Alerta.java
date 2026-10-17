package co.monitorsiae.notificacion;

/**
 * Aviso para la persona que opera el programa.
 *
 * @param clienteId identifica al cliente; una alerta nueva del mismo cliente reemplaza la anterior
 * @param gravedad  tipo de alerta
 * @param titulo    linea corta para la ventana
 * @param detalle   explicacion de que paso y que hacer
 * @param textoVoz  frase corta para leer en voz alta; sin descripciones de armas, que suenan mal
 */
public record Alerta(String clienteId, Gravedad gravedad, String titulo, String detalle, String textoVoz) {
}

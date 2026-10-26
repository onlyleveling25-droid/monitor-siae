package co.monitorsiae.dominio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Comprueba que la configuracion se pueda usar para monitorear antes de que abra el inventario,
 * cuando todavia hay tiempo de corregirla.
 */
public final class ValidadorConfiguracion {

    public static final int REINTENTOS_MAXIMOS = 5;

    public List<ProblemaConfiguracion> validar(Configuracion configuracion) {
        List<ProblemaConfiguracion> problemas = new ArrayList<>();
        validarGenerales(configuracion, problemas);

        Set<String> ids = new HashSet<>();
        for (Cliente cliente : configuracion.clientes()) {
            if (!ids.add(cliente.id())) {
                problemas.add(new ProblemaConfiguracion(etiqueta(cliente), "El identificador está repetido."));
            }
            validarCliente(cliente, problemas);
        }
        return List.copyOf(problemas);
    }

    private void validarGenerales(Configuracion configuracion, List<ProblemaConfiguracion> problemas) {
        if (configuracion.intervaloConsultaSegundos() < Configuracion.INTERVALO_MINIMO_SEGUNDOS) {
            problemas.add(new ProblemaConfiguracion("General",
                    "El intervalo entre consultas debe ser de al menos "
                            + Configuracion.INTERVALO_MINIMO_SEGUNDOS + " segundos."));
        }

        if (configuracion.variacionEsperaPorcentaje() < 0
                || configuracion.variacionEsperaPorcentaje() > Configuracion.VARIACION_MAXIMA_PORCENTAJE) {
            problemas.add(new ProblemaConfiguracion("General",
                    "La variación de la espera debe estar entre 0 y " + Configuracion.VARIACION_MAXIMA_PORCENTAJE
                            + " %."));
        }

        HorarioOperacion horario = configuracion.horario();
        if (horario.restringir()) {
            if (horario.franjas().isEmpty()) {
                problemas.add(new ProblemaConfiguracion("Horario", "Seleccione al menos un día."));
            }
            horario.franjas().forEach((dia, franja) -> {
                if (!franja.esValida()) {
                    problemas.add(new ProblemaConfiguracion("Horario",
                            "En " + nombreDia(dia) + ", la hora inicial debe ser anterior a la hora final."));
                }
            });
        }
    }

    private static String nombreDia(java.time.DayOfWeek dia) {
        return dia.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.forLanguageTag("es"));
    }

    private void validarCliente(Cliente cliente, List<ProblemaConfiguracion> problemas) {
        String ubicacion = etiqueta(cliente);
        if (cliente.nombre().isEmpty()) {
            problemas.add(new ProblemaConfiguracion(ubicacion, "Falta el nombre."));
        }
        validarTramite(ubicacion, cliente.tramite(), problemas);
        validarSeleccion(ubicacion, cliente.seleccion(), cliente.modo(), problemas);
    }

    private void validarTramite(String ubicacion, ParametrosTramite tramite, List<ProblemaConfiguracion> problemas) {
        if (tramite.obtenerPermiso().isEmpty()) {
            problemas.add(new ProblemaConfiguracion(ubicacion, "Falta cómo desea obtener el permiso."));
        }
        if (tramite.tipoTramite().isEmpty()) {
            problemas.add(new ProblemaConfiguracion(ubicacion, "Falta el tipo de trámite."));
        }
        if (tramite.seccionales().isEmpty()) {
            problemas.add(new ProblemaConfiguracion(ubicacion, "Agregue al menos una seccional."));
        }

        int largo = tramite.descripcion().length();
        if (largo < ParametrosTramite.DESCRIPCION_MINIMA || largo > ParametrosTramite.DESCRIPCION_MAXIMA) {
            problemas.add(new ProblemaConfiguracion(ubicacion,
                    "La descripción debe tener entre " + ParametrosTramite.DESCRIPCION_MINIMA + " y "
                            + ParametrosTramite.DESCRIPCION_MAXIMA + " caracteres; tiene " + largo + "."));
        }
    }

    private void validarSeleccion(String ubicacion, ReglasSeleccion reglas, ModoReaccion modo,
                                  List<ProblemaConfiguracion> problemas) {
        if (reglas.objetivos().stream().anyMatch(criterio -> criterio.palabrasClave().isEmpty())
                || reglas.alternativas().stream().anyMatch(criterio -> criterio.palabrasClave().isEmpty())) {
            problemas.add(new ProblemaConfiguracion(ubicacion, "Hay un arma sin palabras clave."));
        }

        switch (reglas.siNoHayObjetivo()) {
            case SEGUIR_ESPERANDO -> {
                if (reglas.objetivos().isEmpty()) {
                    problemas.add(new ProblemaConfiguracion(ubicacion,
                            "Agregue al menos un arma objetivo o cambie la regla para cuando no haya objetivo."));
                }
            }
            case USAR_ALTERNATIVAS -> {
                if (reglas.alternativas().isEmpty()) {
                    problemas.add(new ProblemaConfiguracion(ubicacion,
                            "La regla es usar alternativas, pero no hay ninguna configurada."));
                }
            }
            case CUALQUIERA_DEL_TIPO -> {
                if (reglas.tipoComodin().isEmpty()) {
                    problemas.add(new ProblemaConfiguracion(ubicacion,
                            "La regla es tomar cualquier arma de un tipo, pero falta el tipo."));
                }
            }
            case USAR_ALTERNATIVAS_LUEGO_TIPO -> {
                if (reglas.tipoComodin().isEmpty()) {
                    problemas.add(new ProblemaConfiguracion(ubicacion,
                            "Falta el tipo de arma para el último recurso."));
                }
            }
        }

        if (reglas.reintentos() > REINTENTOS_MAXIMOS) {
            problemas.add(new ProblemaConfiguracion(ubicacion,
                    "Los reintentos no pueden ser más de " + REINTENTOS_MAXIMOS + "."));
        }

        // La dirección solo se usa al apartar: en modo ALERTA la modalidad no se ejecuta.
        if (modo == ModoReaccion.APARTAR && reglas.modalidad() == Modalidad.TENENCIA
                && !reglas.direccionTenencia().completa()) {
            problemas.add(new ProblemaConfiguracion(ubicacion,
                    "La modalidad es TENENCIA: complete departamento, ciudad y dirección."));
        }
    }

    private static String etiqueta(Cliente cliente) {
        return cliente.nombre().isEmpty() ? "Cliente sin nombre" : "Cliente '" + cliente.nombre() + "'";
    }
}

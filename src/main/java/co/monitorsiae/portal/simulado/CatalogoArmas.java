package co.monitorsiae.portal.simulado;

import java.util.List;

/**
 * Armas con descripciones al estilo de la plataforma, para que el simulador pueda mostrar un inventario
 * parecido al real y se pueda comprobar cómo elige el sistema según la configuración de cada cliente.
 */
public final class CatalogoArmas {

    private static final List<String> ARMAS = List.of(
            "Pistola Glock Mod G-43 RAIL CAL 9mm 9T 2P GLOCK 9MM",
            "Pistola Glock Mod G-26 CAL 9mm 10T 3P GLOCK 9MM",
            "Pistola Glock Mod G-19 GEN5 CAL 9mm 15T 3P GLOCK 9MM",
            "Pistola CZ P10 CAL 9 mm 2P 9T CZ 9MM",
            "Pistola CZ P10 C CAL 9 mm 15T 3P CZ 9MM",
            "Pistola Cordova Compacta Cal 9MM 9T 3P CORDOVA 9MM",
            "Pistola Jericho 941 PSL CAL 9mm 16T 3P JERICHO 9MM",
            "Pistola Taurus G3C CAL 9mm 12T 3P TAURUS 9MM",
            "Pistola Beretta APX Carry CAL 9mm 8T 2P BERETTA 9MM",
            "Pistola Sig Sauer P365 CAL 9mm 10T 3P SIG SAUER 9MM",
            "Pistola Smith & Wesson M&P Shield Plus CAL 9mm 13T 3P SMITH WESSON 9MM",
            "Pistola Canik TP9 SF Elite CAL 9mm 18T 3P CANIK 9MM",
            "Pistola Walther PDP Compact CAL 9mm 15T 3P WALTHER 9MM",
            "Pistola Ruger LCP II CAL 22 LR 10T 2P RUGER 22 LR",
            "Revolver Taurus 85 CAL 38 Especial 5T 2P TAURUS 38");

    private CatalogoArmas() {
    }

    public static List<String> armas() {
        return ARMAS;
    }
}

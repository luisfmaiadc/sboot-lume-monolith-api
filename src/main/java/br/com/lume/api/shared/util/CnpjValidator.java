package br.com.lume.api.shared.util;

import br.com.lume.api.shared.exception.BusinessRuleException;
import java.util.Locale;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class CnpjValidator {

    private static final int[] PESOS_DIGITO_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_DIGITO_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    public static boolean isValido(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) {
            return false;
        }

        String normalized = cnpj.toUpperCase(Locale.ROOT);

        boolean todosIguais = true;
        for (int i = 0; i < 14; i++) {
            char c = normalized.charAt(i);
            if (i < 12) {
                if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z'))) {
                    return false;
                }
            } else {
                if (c < '0' || c > '9') {
                    return false;
                }
            }
            if (i > 0 && c != normalized.charAt(0)) {
                todosIguais = false;
            }
        }
        if (todosIguais) {
            return false;
        }

        int soma1 = 0;
        for (int i = 0; i < 12; i++) {
            soma1 += (normalized.charAt(i) - 48) * PESOS_DIGITO_1[i];
        }
        int resto1 = soma1 % 11;
        int digito1 = resto1 < 2 ? 0 : 11 - resto1;
        if (digito1 != (normalized.charAt(12) - '0')) {
            return false;
        }

        int soma2 = 0;
        for (int i = 0; i < 12; i++) {
            soma2 += (normalized.charAt(i) - 48) * PESOS_DIGITO_2[i];
        }
        soma2 += digito1 * PESOS_DIGITO_2[12];
        int resto2 = soma2 % 11;
        int digito2 = resto2 < 2 ? 0 : 11 - resto2;
        return digito2 == (normalized.charAt(13) - '0');
    }

    public static void validar(String cnpj) {
        if (!isValido(cnpj)) {
            throw new BusinessRuleException("CNPJ informado é inválido.");
        }
    }
}
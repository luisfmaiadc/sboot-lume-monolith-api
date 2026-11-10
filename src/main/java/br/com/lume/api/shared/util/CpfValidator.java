package br.com.lume.api.shared.util;

import br.com.lume.api.shared.exception.BusinessRuleException;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class CpfValidator {

    public static boolean isValido(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return false;
        }

        boolean todosIguais = true;
        for (int i = 0; i < 11; i++) {
            char c = cpf.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
            if (i > 0 && c != cpf.charAt(0)) {
                todosIguais = false;
            }
        }
        if (todosIguais) {
            return false;
        }

        int soma1 = 0;
        for (int i = 0; i < 9; i++) {
            soma1 += (cpf.charAt(i) - '0') * (10 - i);
        }
        int digito1 = (soma1 * 10) % 11;
        if (digito1 == 10) {
            digito1 = 0;
        }
        if (digito1 != (cpf.charAt(9) - '0')) {
            return false;
        }

        int soma2 = 0;
        for (int i = 0; i < 10; i++) {
            soma2 += (cpf.charAt(i) - '0') * (11 - i);
        }
        int digito2 = (soma2 * 10) % 11;
        if (digito2 == 10) {
            digito2 = 0;
        }
        return digito2 == (cpf.charAt(10) - '0');
    }

    public static void validar(String cpf) {
        if (!isValido(cpf)) {
            throw new BusinessRuleException("CPF informado é inválido.");
        }
    }
}
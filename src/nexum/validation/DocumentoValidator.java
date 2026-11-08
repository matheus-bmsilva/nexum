package nexum.validation;

/**
 * Validador matemático de documentos brasileiros (CPF e CNPJ) conforme as regras
 * e pesos oficiais do Ministério da Fazenda / Receita Federal do Brasil (módulo 11).
 * 
 * Atende ao princípio de responsabilidade única (SRP), centralizando a lógica
 * de validação de integridade documental e formatação.
 */
public class DocumentoValidator {

    private DocumentoValidator() {
        // Construtor privado para classe utilitária
    }

    /**
     * Remove todos os caracteres não numéricos.
     */
    public static String sanitizar(String documento) {
        if (documento == null) {
            return "";
        }
        return documento.replaceAll("\\D", "");
    }

    /**
     * Valida matematicamente um CPF:
     * 1. Verifica se possui exatamente 11 dígitos numéricos;
     * 2. Rejeita sequências de dígitos idênticos repetidos (ex: 111.111.111-11, 000.000.000-00);
     * 3. Calcula e valida o 1º dígito verificador via módulo 11 (pesos 10 a 2);
     * 4. Calcula e valida o 2º dígito verificador via módulo 11 (pesos 11 a 2).
     * 
     * @param cpf CPF formatado ou contendo apenas dígitos
     * @return true se o CPF for matematicamente válido, false caso contrário
     */
    public static boolean isCpfValido(String cpf) {
        String digitos = sanitizar(cpf);
        if (digitos.length() != 11) {
            return false;
        }

        // Rejeita CPFs formados por 11 dígitos iguais (00000000000 a 99999999999)
        if (isDigitosTodosIguais(digitos)) {
            return false;
        }

        // 1º Dígito Verificador (pesos 10 a 2)
        int soma1 = 0;
        for (int i = 0; i < 9; i++) {
            soma1 += (digitos.charAt(i) - '0') * (10 - i);
        }
        int resto1 = soma1 % 11;
        int d1 = (resto1 < 2) ? 0 : 11 - resto1;
        if (d1 != (digitos.charAt(9) - '0')) {
            return false;
        }

        // 2º Dígito Verificador (pesos 11 a 2)
        int soma2 = 0;
        for (int i = 0; i < 10; i++) {
            soma2 += (digitos.charAt(i) - '0') * (11 - i);
        }
        int resto2 = soma2 % 11;
        int d2 = (resto2 < 2) ? 0 : 11 - resto2;
        return d2 == (digitos.charAt(10) - '0');
    }

    /**
     * Valida matematicamente um CNPJ:
     * 1. Verifica se possui exatamente 14 dígitos numéricos;
     * 2. Rejeita sequências de 14 dígitos idênticos repetidos;
     * 3. Calcula e valida o 1º dígito verificador (pesos 5,4,3,2,9,8,7,6,5,4,3,2);
     * 4. Calcula e valida o 2º dígito verificador (pesos 6,5,4,3,2,9,8,7,6,5,4,3,2).
     * 
     * @param cnpj CNPJ formatado ou contendo apenas dígitos
     * @return true se o CNPJ for matematicamente válido, false caso contrário
     */
    public static boolean isCnpjValido(String cnpj) {
        String digitos = sanitizar(cnpj);
        if (digitos.length() != 14) {
            return false;
        }

        // Rejeita CNPJs com todos os 14 dígitos idênticos
        if (isDigitosTodosIguais(digitos)) {
            return false;
        }

        int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int soma1 = 0;
        for (int i = 0; i < 12; i++) {
            soma1 += (digitos.charAt(i) - '0') * pesos1[i];
        }
        int resto1 = soma1 % 11;
        int d1 = (resto1 < 2) ? 0 : 11 - resto1;
        if (d1 != (digitos.charAt(12) - '0')) {
            return false;
        }

        int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int soma2 = 0;
        for (int i = 0; i < 13; i++) {
            soma2 += (digitos.charAt(i) - '0') * pesos2[i];
        }
        int resto2 = soma2 % 11;
        int d2 = (resto2 < 2) ? 0 : 11 - resto2;
        return d2 == (digitos.charAt(13) - '0');
    }

    /**
     * Formata uma cadeia de 11 dígitos no padrão ###.###.###-##.
     */
    public static String formatarCpf(String cpf) {
        String d = sanitizar(cpf);
        if (d.length() != 11) {
            return (cpf != null) ? cpf : "";
        }
        return String.format("%s.%s.%s-%s",
            d.substring(0, 3), d.substring(3, 6), d.substring(6, 9), d.substring(9, 11));
    }

    /**
     * Formata uma cadeia de 14 dígitos no padrão ##.###.###/####-##.
     */
    public static String formatarCnpj(String cnpj) {
        String d = sanitizar(cnpj);
        if (d.length() != 14) {
            return (cnpj != null) ? cnpj : "";
        }
        return String.format("%s.%s.%s/%s-%s",
            d.substring(0, 2), d.substring(2, 5), d.substring(5, 8), d.substring(8, 12), d.substring(12, 14));
    }

    private static boolean isDigitosTodosIguais(String str) {
        char primeiro = str.charAt(0);
        for (int i = 1; i < str.length(); i++) {
            if (str.charAt(i) != primeiro) {
                return false;
            }
        }
        return true;
    }
}

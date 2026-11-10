package br.com.lume.api.identidade.domain;

import br.com.lume.api.shared.security.CpfCryptoService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Converter
@Component
@RequiredArgsConstructor
public class CpfCryptoConverter implements AttributeConverter<String, String> {

    private final CpfCryptoService service;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return service.cifrar(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return service.decifrar(dbData);
    }
}
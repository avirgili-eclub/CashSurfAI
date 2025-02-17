package com.py.lawbyteia.ai.configuration;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Converter
public class PgVectorConverter implements AttributeConverter<List<Float>, PGobject> {

    @Override
    public PGobject convertToDatabaseColumn(List<Float> attribute) {
        if (attribute == null) {
            return null;
        }

        PGobject pgObject = new PGobject();
        pgObject.setType("vector");
        try {
            // Convertir la lista de floats a un string que PostgreSQL pueda interpretar como un vector
            String vectorString = "[" + attribute.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","))
                    + "]";
            pgObject.setValue(vectorString);
        } catch (SQLException e) {
            throw new RuntimeException("Error converting List<Float> to PGobject", e);
        }

        return pgObject;
    }

    @Override
    public List<Float> convertToEntityAttribute(PGobject dbData) {
        if (dbData == null || dbData.getValue() == null) {
            return null;
        }

        // Convertir el string del vector a una lista de floats
        String value = dbData.getValue();
        return Stream.of(value.replaceAll("[\\[\\]]", "")  // Eliminar los corchetes
                        .split(","))
                .map(Float::parseFloat) // Convertir a Float
                .collect(Collectors.toList());
    }
}
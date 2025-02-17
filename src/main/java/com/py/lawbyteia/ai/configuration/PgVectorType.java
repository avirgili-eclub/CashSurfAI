package com.py.lawbyteia.ai.configuration;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.postgresql.util.PGobject;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PgVectorType implements UserType<List<Float>> {

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<List<Float>> returnedClass() {
        return (Class<List<Float>>) (Class<?>) List.class;
    }

    @Override
    public boolean equals(List<Float> x, List<Float> y) {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(List<Float> x) {
        return Objects.hashCode(x);
    }

    @Override
    public List<Float> nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
            throws SQLException {
        String value = rs.getString(position);
        if (value == null) {
            return null;
        }

        // Remove brackets and split by commas
        value = value.replace("[", "").replace("]", "");
        String[] numbers = value.split(",");
        List<Float> result = new ArrayList<>();

        for (String number : numbers) {
            if (!number.trim().isEmpty()) {
                result.add(Float.parseFloat(number.trim()));
            }
        }

        return result;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, List<Float> value, int index, SharedSessionContractImplementor session)
            throws SQLException {
        if (value == null || value.isEmpty()) {
            value = Collections.nCopies(1536, 0.0f); // Asignar vector de ceros si está vacío
        }

        // Convert List<Float> to PostgreSQL vector format
        String vectorString = "[" + value.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";

        PGobject pgObject = new PGobject();
        pgObject.setType("vector");
        pgObject.setValue(vectorString);

        st.setObject(index, pgObject);

//        // Convert List<Float> to PostgreSQL vector format
//        String vectorString = "[" + String.join(",",
//                value.stream()
//                        .map(String::valueOf)
//                        .toList())
//                + "]";
//
//        PGobject pgObject = new PGobject();
//        pgObject.setType("vector");
//        pgObject.setValue(vectorString);
//
//        st.setObject(index, pgObject);
    }

    @Override
    public List<Float> deepCopy(List<Float> value) {
        if (value == null) {
            return null;
        }
        return new ArrayList<>(value);
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(List<Float> value) {
        return (Serializable) deepCopy(value);
    }

    @Override
    public List<Float> assemble(Serializable cached, Object owner) {
        return deepCopy((List<Float>) cached);
    }

    @Override
    public List<Float> replace(List<Float> detached, List<Float> managed, Object owner) {
        return deepCopy(detached);
    }
}
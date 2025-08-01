package org.tom.nettapoc;

import java.lang.reflect.*;
import java.util.*;

public class QueryBuilder {

    public static String buildSelectionSet(Class<?> clazz) {
        return "query " + buildFields(clazz, 1, new HashSet<>());
    }

    private static String buildFields(Class<?> clazz, int indentLevel, Set<Class<?>> visited) {
        if (visited.contains(clazz)) return ""; // Prevent infinite recursion
        visited.add(clazz);

        String indent = "  ".repeat(indentLevel);
        StringBuilder sb = new StringBuilder("{\n");

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isSynthetic()) continue;

            sb.append(indent).append(field.getName());

            Class<?> fieldType = field.getType();
            Type genericType = field.getGenericType();

            if (isListLike(fieldType)) {
                Class<?> elementType = getListElementType(genericType);
                if (isCustomClass(elementType)) {
                    sb.append(" ").append(buildFields(elementType, indentLevel + 1, visited));
                }
            } else if (isCustomClass(fieldType)) {
                sb.append(" ").append(buildFields(fieldType, indentLevel + 1, visited));
            }

            sb.append("\n");
        }

        sb.append("  ".repeat(indentLevel - 1)).append("}");
        return sb.toString();
    }

    private static boolean isListLike(Class<?> type) {
        return List.class.isAssignableFrom(type) || Set.class.isAssignableFrom(type);
    }

    private static Class<?> getListElementType(Type type) {
        if (!(type instanceof ParameterizedType)) return Object.class;
        Type actualType = ((ParameterizedType) type).getActualTypeArguments()[0];
        if (actualType instanceof Class<?>) return (Class<?>) actualType;
        return Object.class;
    }

    private static boolean isCustomClass(Class<?> clazz) {
        return !(clazz.isPrimitive()
                || clazz == String.class
                || Number.class.isAssignableFrom(clazz)
                || clazz == Boolean.class
                || clazz.isEnum()
                || clazz.getName().startsWith("java."));
    }

    // Example usage
    public static void main(String[] args) {
        System.out.println(buildSelectionSet(Person.class));
    }

    static class Person {
        String name;
        Integer id;
        List<Address> addresses;
        Set<Phone> phones;

        static class Address {
            String street;
            String city;
        }

        static class Phone {
            String type;
            String number;
        }
    }
}


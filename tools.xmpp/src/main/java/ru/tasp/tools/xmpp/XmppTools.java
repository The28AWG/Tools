package ru.tasp.tools.xmpp;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import ru.tasp.tools.Validator;

/**
 * Created by the28awg on 26.10.15.
 */
public class XmppTools {

    public static final String IDENTIFIER_PATTERN = "[0-9a-z]+";
    public static final String VALID_JAVA_IDENTIFIER = "(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";

    public static List<String> toList(java.lang.String list) {
        return toList(list, null);
    }

    public static List<String> toList(java.lang.String list, Validator validator) {
        Scanner scanner = new Scanner(list).useDelimiter(",");
        java.util.List<java.lang.String> result = new ArrayList<java.lang.String>();
        while (scanner.hasNext()) {
            java.lang.String aValue = scanner.next();
            if (validator != null) {
                if (validator.validate(aValue)) {
                    result.add(aValue);
                } else {
                    throw new RuntimeException("not valid: " + aValue);
                }
            } else {
                result.add(aValue);
            }
        }
        return result;
    }

    public static String toString(List<String> list) {
        return toString(list, null);
    }

    public static String toString(List<String> list, Validator validator) {
        StringBuilder builder = new StringBuilder();
        for (String aValue : list) {

            if (validator != null) {
                if (validator.validate(aValue)) {
                    builder.append(aValue).append(",");
                } else {
                    throw new RuntimeException("not valid: " + aValue);
                }
            } else {
                builder.append(aValue).append(",");
            }
        }
        if (list.size() > 0) builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    public static Class<?> forName(String sClass) {
        try {

            return Class.forName(sClass);
        } catch (ClassNotFoundException ignore) {
        }
        return null;
    }

    public static boolean isEmpty(CharSequence str) {
        if (str == null || str.length() == 0)
            return true;
        else
            return false;
    }

    public static boolean exist(List<String> list, String id) {
        return list.contains(id);
    }
}

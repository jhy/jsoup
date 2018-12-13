package org.jsoup.parser;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

public class HtmlTreeBuilderStateTest {
    @Test
    public void ensureArraysAreSorted() throws ClassNotFoundException, IllegalAccessException {
        Class constants = Class.forName("org.jsoup.parser.HtmlTreeBuilderState$Constants");
        Field[] fields = constants.getDeclaredFields();
        List<String[]> arrays = new ArrayList<>(fields.length);
        for (Field field : fields) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) && field.getType() == String[].class) {
                arrays.add((String[]) field.get(null));
            }
        }

        for (String[] array : arrays) {
            String[] copy = Arrays.copyOf(array, array.length);
            Arrays.sort(array);
            assertArrayEquals(array, copy);
        }
    }
}

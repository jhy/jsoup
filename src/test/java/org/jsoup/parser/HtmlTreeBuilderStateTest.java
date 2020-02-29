package org.jsoup.parser;

import org.jsoup.parser.HtmlTreeBuilderState.Constants;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class HtmlTreeBuilderStateTest {
    static List<Object[]> findArrays(Class<?> constantClass) {
        ArrayList<Object[]> array = new ArrayList<>();

        Field[] fields = constantClass.getDeclaredFields();

        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType().isArray()) {
                try {
                    array.add((Object[]) field.get(null));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        return array;
    }

    static void ensureSorted(List<Object[]> constants) {
        for (Object[] array : constants) {
            Object[] copy = Arrays.copyOf(array, array.length);
            Arrays.sort(array);
            assertArrayEquals(array, copy);
        }
    }

    @Test
    public void ensureArraysAreSorted() {
        List<Object[]> constants = findArrays(Constants.class);
        ensureSorted(constants);
        assertEquals(38, constants.size());
    }

}

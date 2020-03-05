package org.jsoup;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.*;
import java.util.Locale;
import java.util.stream.Stream;

public class MultiLocaleExtension implements AfterEachCallback, ArgumentsProvider {
    private final Locale defaultLocale = Locale.getDefault();

    @Override
    public void afterEach(ExtensionContext context) {
        Locale.setDefault(defaultLocale);
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
        return Stream.of(Arguments.of(Locale.ENGLISH), Arguments.arguments(new Locale("tr")));
    }


    @Documented
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ArgumentsSource(MultiLocaleExtension.class)
    @ExtendWith(MultiLocaleExtension.class)
    @ParameterizedTest
    public @interface MultiLocaleTest {
    }

}

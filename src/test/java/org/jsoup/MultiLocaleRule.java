package org.jsoup;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

public class MultiLocaleRule implements TestRule {
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MultiLocaleTest {
    }

    public Statement apply(final Statement statement, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                MultiLocaleTest annotation = description.getAnnotation(MultiLocaleTest.class);
                if (annotation == null) {
                    statement.evaluate();
                    return;
                }

                evaluateWithLocale(Locale.ENGLISH);
                evaluateWithLocale(new Locale("tr"));
            }

            private void evaluateWithLocale(Locale locale) throws Throwable {
                Locale oldLocale = Locale.getDefault();
                Locale.setDefault(locale);
                try {
                    statement.evaluate();
                } finally {
                    Locale.setDefault(oldLocale);
                }
            }
        };
    }
}

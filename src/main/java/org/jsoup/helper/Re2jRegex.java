package org.jsoup.helper;

/**
 re2j-backed Regex implementation; must only be touched when re2j is on the classpath.
 */
final class Re2jRegex extends Regex {
    private static final java.util.regex.Pattern unused = java.util.regex.Pattern.compile("");

    private final com.google.re2j.Pattern re2jPattern;

    private Re2jRegex(com.google.re2j.Pattern re2jPattern) {
        super(unused);
        this.re2jPattern = re2jPattern;
    }

    public static Regex compile(String regex) {
        try {
            return new Re2jRegex(com.google.re2j.Pattern.compile(regex));
        } catch (RuntimeException e) {
            throw new ValidationException("Pattern syntax error: " + e.getMessage());
        }
    }

    @Override
    public Matcher matcher(CharSequence input) {
        return new Re2jMatcher(re2jPattern.matcher(input));
    }

    @Override
    public String toString() {
        return re2jPattern.toString();
    }

    private static final class Re2jMatcher implements Matcher {
        private final com.google.re2j.Matcher delegate;

        Re2jMatcher(com.google.re2j.Matcher delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean find() {
            return delegate.find();
        }
    }
}

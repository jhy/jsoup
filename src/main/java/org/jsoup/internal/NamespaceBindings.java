package org.jsoup.internal;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashMap;

/** Tracks namespace prefix bindings across nested element scopes. Jsoup internal; API subject to change. */
public final class NamespaceBindings {
    private static final String XmlnsKey = "xmlns";
    private static final String XmlnsPrefix = "xmlns:";
    private static final NamespaceChange ScopeMarker = new NamespaceChange("", null);

    private final HashMap<String, String> bindings = new HashMap<>();
    private final ArrayDeque<NamespaceChange> changes = new ArrayDeque<>();

    /** Creates an empty binding set. */
    public NamespaceBindings() {}

    /** Clears all bindings and scopes for reuse. */
    public void clear() {
        bindings.clear();
        changes.clear();
    }

    /** Opens a namespace scope. */
    public void pushScope() {
        changes.push(ScopeMarker);
    }

    /** Closes the current scope and restores its parent bindings. */
    public void popScope() {
        NamespaceChange change;
        while ((change = changes.pop()) != ScopeMarker)
            change.restore(bindings);
    }

    /** Binds a prefix in the current scope, or at the root if no scope is open. */
    public void put(String prefix, String namespace) {
        String previousValue = bindings.get(prefix);
        if (namespace.equals(previousValue)) return;

        bindings.put(prefix, namespace);
        if (!changes.isEmpty())
            changes.push(new NamespaceChange(prefix, previousValue));
    }

    /** Returns the namespace URI bound to a prefix, or null if unbound. */
    public @Nullable String get(String prefix) {
        return bindings.get(prefix);
    }

    /** Applies namespace declarations from an attribute set. */
    public void applyDeclarations(Attributes attributes) {
        for (Attribute attribute : attributes) {
            @Nullable String prefix = declarationPrefix(attribute.getKey());
            if (prefix != null)
                put(prefix, attribute.getValue());
        }
    }

    /** Tests whether a key is an {@code xmlns} declaration. */
    public static boolean isDeclaration(String key) {
        return declarationPrefix(key) != null;
    }

    /** Returns the prefix declared by an {@code xmlns} key, or null if it is not a declaration. */
    public static @Nullable String declarationPrefix(String key) {
        if (key.equals(XmlnsKey)) return "";
        if (key.startsWith(XmlnsPrefix)) return key.substring(XmlnsPrefix.length());
        return null;
    }

    /** Returns the number of active bindings. */
    int bindingCount() {
        return bindings.size();
    }

    /** Returns the number of recorded scope markers and binding changes. */
    int changeCount() {
        return changes.size();
    }

    /** A previous binding recorded for scope restoration. */
    private static final class NamespaceChange {
        private final String prefix;
        private final @Nullable String previousValue;

        /** Records a prefix's previous binding. */
        private NamespaceChange(String prefix, @Nullable String previousValue) {
            this.prefix = prefix;
            this.previousValue = previousValue;
        }

        /** Restores the previous binding. */
        private void restore(HashMap<String, String> bindings) {
            if (previousValue == null) bindings.remove(prefix);
            else bindings.put(prefix, previousValue);
        }
    }
}

package org.jsoup.internal;

import org.jsoup.nodes.Attributes;
import org.junit.jupiter.api.Test;

import static org.jsoup.parser.Parser.NamespaceXml;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NamespaceBindingsTest {
    @Test void restoresBindingsAfterNestedScopes() {
        NamespaceBindings bindings = new NamespaceBindings();
        bindings.put("xml", NamespaceXml);
        bindings.put("p", "/base");

        bindings.pushScope();
        bindings.applyDeclarations(new Attributes().put("xmlns:p", "/one"));
        assertEquals("/one", bindings.get("p"));

        bindings.pushScope();
        bindings.applyDeclarations(new Attributes().put("xmlns:p", ""));
        assertEquals("", bindings.get("p"));
        bindings.put("q", "/two");
        assertEquals("/two", bindings.get("q"));

        bindings.popScope();
        assertEquals("/one", bindings.get("p"));
        assertNull(bindings.get("q"));

        bindings.popScope();
        assertEquals("/base", bindings.get("p"));
        assertEquals(NamespaceXml, bindings.get("xml"));
    }

    @Test void stateGrowsLinearlyWithScopeDepth() {
        // each scope retains one marker and only its changed bindings
        int depth = 1_000;
        NamespaceBindings bindings = new NamespaceBindings();
        bindings.put("xml", NamespaceXml);

        for (int i = 0; i < depth; i++) {
            bindings.pushScope();
            bindings.put("p" + i, "urn:" + i);
        }
        bindings.put("p999", "urn:999"); // unchanged bindings don't add change records

        assertEquals(depth + 1, bindings.bindingCount());
        assertEquals(depth * 2, bindings.changeCount());

        for (int i = 0; i < depth; i++)
            bindings.popScope();

        assertEquals(1, bindings.bindingCount());
        assertEquals(0, bindings.changeCount());
    }

    @Test void recognizesNamespaceDeclarations() {
        assertTrue(NamespaceBindings.isDeclaration("xmlns"));
        assertTrue(NamespaceBindings.isDeclaration("xmlns:p"));
        assertFalse(NamespaceBindings.isDeclaration("xml:lang"));
    }
}

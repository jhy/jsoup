package org.jsoup.integration;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class MultiReleaseJarIT {
    @Test void loadsJava11HttpClientExecutor() throws Exception {
        // verifies the packaged JAR is an mjar and contains the HttpClientExecutor

        String testJar = System.getProperty("jsoup.test.jar");
        // skip outside Failsafe if there is no packaged JAR to test
        assumeTrue(testJar != null, "Requires the packaged JAR path from Maven Failsafe");
        Path jarPath = Paths.get(testJar);
        String classEntry = "org/jsoup/helper/HttpClientExecutor.class";
        String versionedEntry = "META-INF/versions/11/" + classEntry;

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            assertEquals("true", jar.getManifest().getMainAttributes().getValue("Multi-Release"));
            assertNotNull(jar.getJarEntry(versionedEntry));
            assertNull(jar.getJarEntry(classEntry));
        }

        Class<?> executor = Class.forName("org.jsoup.helper.HttpClientExecutor");
        assertEquals(jarPath.toUri().toURL(), executor.getProtectionDomain().getCodeSource().getLocation());
        URL resource = executor.getResource("HttpClientExecutor.class");
        assertNotNull(resource);
        String resourceUrl = resource.toExternalForm();
        assertTrue(resourceUrl.endsWith("!/" + versionedEntry), resourceUrl);
    }
}

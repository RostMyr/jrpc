package com.github.rostmyr.jrpc.maven;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.io.File;

/**
 * Rostyslav Myroshnychenko
 * on 25.05.2018.
 */
public class TestProcessSourcesMojo extends AbstractMojoTestCase {

    public void testFindPluginConfiguration() throws Exception {
        // GIVEN
        File pom = getTestFile("src/test/resources/pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());
        ProcessSourcesMojo myMojo = (ProcessSourcesMojo) lookupMojo("process-source-files", pom);

        // WHEN - THEN
        assertNotNull(myMojo);
    }
}
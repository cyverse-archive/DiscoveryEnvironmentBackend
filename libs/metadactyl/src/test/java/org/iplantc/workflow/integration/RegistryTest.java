package org.iplantc.workflow.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.iplantc.workflow.integration.util.Registry;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.create.Registry.
 * 
 * @author Dennis Roberts
 */
public class RegistryTest {

    /**
     * The registry being tested.
     */
    private Registry<String> registry = new Registry<String>();

    /**
     * Verifies that we can register an object.
     */
    @Test
    public void shouldRegisterObject() {
        registry.add("foo", "bar");
        assertEquals(1, registry.size());
    }

    /**
     * Verifies that we can get a registered object.
     */
    @Test
    public void shouldGetRegisteredObject() {
        registry.add("foo", "bar");
        assertEquals("bar", registry.get("foo"));
    }

    /**
     * Verifies that null is returned if we try to get the registered object for a name that hasn't been registered.
     */
    @Test
    public void unregisteredNameShouldReturnNull() {
        registry.add("foo", "bar");
        assertNull(registry.get("baz"));
    }

    /**
     * Verifies that we can get all of the registered objects.
     */
    @Test
    public void shouldGetAllRegisteredOjbects() {
        registry.add("foo", "bar");
        registry.add("baz", "quux");
        Set<String> expected = new HashSet<String>(Arrays.asList("bar", "quux"));
        Set<String> actual = new HashSet<String>(registry.getRegisteredObjects());
        assertEquals(expected, actual);
    }
}

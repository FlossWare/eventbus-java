package org.flossware.eventbus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case tests for ServiceRegistryImpl to achieve 100% coverage.
 */
class ServiceRegistryImplEdgeCasesTest {

    private ServiceRegistryImpl registry;

    interface TestService {
        String getName();
    }

    static class TestServiceImpl implements TestService {
        private final String name;

        TestServiceImpl(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    @BeforeEach
    void setUp() {
        registry = new ServiceRegistryImpl();
    }

    @Test
    @DisplayName("Should handle unregister of non-existent service")
    void testUnregisterNonExistentService() {
        TestService impl = new TestServiceImpl("test");

        // Unregister without ever registering
        assertDoesNotThrow(() ->
                registry.unregisterService(TestService.class, impl));
    }

    @Test
    @DisplayName("Should handle unregister of different instance")
    void testUnregisterDifferentInstance() {
        TestService impl1 = new TestServiceImpl("impl1");
        TestService impl2 = new TestServiceImpl("impl2");

        registry.registerService(TestService.class, impl1);

        // Try to unregister a different instance (identity comparison should fail)
        registry.unregisterService(TestService.class, impl2);

        // impl1 should still be registered
        assertTrue(registry.getService(TestService.class).isPresent());
        assertEquals("impl1", registry.getService(TestService.class).get().getName());
    }

    @Test
    @DisplayName("Should access ServiceEntry registration time")
    void testServiceEntryRegistrationTime() throws Exception {
        TestService impl = new TestServiceImpl("test");

        long beforeRegistration = System.currentTimeMillis();
        Thread.sleep(10); // Small delay to ensure different timestamps

        registry.registerService(TestService.class, impl);

        Thread.sleep(10);
        long afterRegistration = System.currentTimeMillis();

        // Access ServiceEntry through reflection to test getRegistrationTime()
        java.lang.reflect.Field servicesField = ServiceRegistryImpl.class.getDeclaredField("services");
        servicesField.setAccessible(true);

        @SuppressWarnings("unchecked")
        java.util.Map<Class<?>, java.util.List<?>> services =
                (java.util.Map<Class<?>, java.util.List<?>>) servicesField.get(registry);

        java.util.List<?> entries = services.get(TestService.class);
        assertNotNull(entries);
        assertEquals(1, entries.size());

        Object entry = entries.get(0);

        // Get the registration time
        java.lang.reflect.Method getRegistrationTime = entry.getClass().getDeclaredMethod("getRegistrationTime");
        getRegistrationTime.setAccessible(true);
        long registrationTime = (Long) getRegistrationTime.invoke(entry);

        assertTrue(registrationTime >= beforeRegistration,
                "Registration time should be after or equal to beforeRegistration");
        assertTrue(registrationTime <= afterRegistration,
                "Registration time should be before or equal to afterRegistration");
    }

    @Test
    @DisplayName("Should handle clear with no services")
    void testClearWithNoServices() {
        assertDoesNotThrow(() -> registry.clear());
    }

    @Test
    @DisplayName("Should handle clear with multiple service types")
    void testClearWithMultipleServiceTypes() {
        interface ServiceA {}
        interface ServiceB {}

        class ServiceAImpl implements ServiceA {}
        class ServiceBImpl implements ServiceB {}

        registry.registerService(ServiceA.class, new ServiceAImpl());
        registry.registerService(ServiceA.class, new ServiceAImpl());
        registry.registerService(ServiceB.class, new ServiceBImpl());

        registry.clear();

        assertTrue(registry.getAllServices(ServiceA.class).isEmpty());
        assertTrue(registry.getAllServices(ServiceB.class).isEmpty());
    }

    @Test
    @DisplayName("Should handle getService with null in requireNonNull")
    void testGetServiceNull() {
        assertThrows(NullPointerException.class, () ->
                registry.getService(null));
    }

    @Test
    @DisplayName("Should handle getAllServices with null in requireNonNull")
    void testGetAllServicesNull() {
        assertThrows(NullPointerException.class, () ->
                registry.getAllServices(null));
    }
}

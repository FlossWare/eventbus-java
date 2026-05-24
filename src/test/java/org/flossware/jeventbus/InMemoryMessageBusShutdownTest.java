package org.flossware.jeventbus;

import org.flossware.jeventbus.api.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Field;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InMemoryMessageBus shutdown scenarios to achieve 100% coverage.
 */
class InMemoryMessageBusShutdownTest {

    private InMemoryMessageBus messageBus;

    @AfterEach
    void tearDown() {
        if (messageBus != null) {
            try {
                // Force shutdown if still running
                messageBus.shutdown();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Test
    @DisplayName("Should handle shutdownNow when timeout expires")
    void testShutdownTimeout() throws Exception {
        messageBus = new InMemoryMessageBus();

        // Create a never-ending task that will force timeout
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch blockForever = new CountDownLatch(1);

        messageBus.subscribe("test-topic", message -> {
            try {
                taskStarted.countDown();
                // Wait forever (or until interrupted)
                blockForever.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Message msg = Message.builder()
                .topic("test-topic")
                .sourceApplicationId("test-app")
                .payload("Test".getBytes())
                .build();

        // Publish message that will block
        messageBus.publish("test-topic", msg);

        // Wait for task to start
        assertTrue(taskStarted.await(5, TimeUnit.SECONDS));

        // Get the executor field to manipulate timeout behavior
        Field executorField = InMemoryMessageBus.class.getDeclaredField("dispatchExecutor");
        executorField.setAccessible(true);
        ExecutorService executor = (ExecutorService) executorField.get(messageBus);

        // Create a mock executor that will timeout
        ExecutorService mockExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()) {
            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                // Return false to simulate timeout
                return false;
            }
        };

        // Replace the executor
        executorField.set(messageBus, mockExecutor);

        // Shutdown should handle the timeout and call shutdownNow
        assertDoesNotThrow(() -> messageBus.shutdown());

        // Cleanup
        blockForever.countDown();
        executor.shutdownNow();
    }

    @Test
    @DisplayName("Should handle InterruptedException during shutdown")
    void testShutdownInterruptedException() throws Exception {
        messageBus = new InMemoryMessageBus();

        // Get the executor field
        Field executorField = InMemoryMessageBus.class.getDeclaredField("dispatchExecutor");
        executorField.setAccessible(true);
        ExecutorService originalExecutor = (ExecutorService) executorField.get(messageBus);

        // Create a mock executor that throws InterruptedException
        ExecutorService mockExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()) {
            @Override
            public void shutdown() {
                // Do nothing
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                throw new InterruptedException("Simulated interruption");
            }
        };

        // Replace the executor
        executorField.set(messageBus, mockExecutor);

        // Shutdown should handle the interruption
        assertDoesNotThrow(() -> messageBus.shutdown());

        // Thread should be interrupted
        assertTrue(Thread.interrupted()); // Also clears the flag

        // Cleanup
        originalExecutor.shutdownNow();
    }

    @Test
    @DisplayName("Should handle normal shutdown without timeout")
    void testNormalShutdown() throws Exception {
        messageBus = new InMemoryMessageBus();

        CountDownLatch latch = new CountDownLatch(1);

        messageBus.subscribe("test-topic", message -> latch.countDown());

        Message msg = Message.builder()
                .topic("test-topic")
                .sourceApplicationId("test-app")
                .payload("Test".getBytes())
                .build();

        messageBus.publish("test-topic", msg);

        // Wait for message to be delivered
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Normal shutdown should complete quickly
        long startTime = System.currentTimeMillis();
        messageBus.shutdown();
        long duration = System.currentTimeMillis() - startTime;

        // Should complete well within timeout
        assertTrue(duration < 1000, "Shutdown took " + duration + "ms, expected < 1000ms");
    }
}

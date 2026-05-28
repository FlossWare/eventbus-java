package org.flossware.eventbus.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Message class.
 */
class MessageTest {

    @Test
    @DisplayName("Should build message with all fields")
    void testBuildMessageWithAllFields() {
        String id = "test-id";
        String topic = "test-topic";
        String sourceAppId = "test-app";
        byte[] payload = "test-payload".getBytes();
        long timestamp = 1234567890L;
        Map<String, Object> headers = new HashMap<>();
        headers.put("key1", "value1");
        headers.put("key2", 123);

        Message message = Message.builder()
                .id(id)
                .topic(topic)
                .sourceApplicationId(sourceAppId)
                .payload(payload)
                .timestamp(timestamp)
                .headers(headers)
                .build();

        assertEquals(id, message.getId());
        assertEquals(topic, message.getTopic());
        assertEquals(sourceAppId, message.getSourceApplicationId());
        assertArrayEquals(payload, message.getPayload());
        assertEquals(timestamp, message.getTimestamp());
        assertEquals(2, message.getHeaders().size());
        assertEquals("value1", message.getHeaders().get("key1"));
        assertEquals(123, message.getHeaders().get("key2"));
    }

    @Test
    @DisplayName("Should auto-generate ID if not provided")
    void testAutoGenerateId() {
        Message message = Message.builder()
                .topic("test-topic")
                .sourceApplicationId("test-app")
                .payload("test".getBytes())
                .build();

        assertNotNull(message.getId());
        assertFalse(message.getId().isEmpty());
    }

    @Test
    @DisplayName("Should auto-generate timestamp if not provided")
    void testAutoGenerateTimestamp() {
        long before = System.currentTimeMillis();

        Message message = Message.builder()
                .topic("test-topic")
                .sourceApplicationId("test-app")
                .payload("test".getBytes())
                .build();

        long after = System.currentTimeMillis();

        assertTrue(message.getTimestamp() >= before);
        assertTrue(message.getTimestamp() <= after);
    }

    @Test
    @DisplayName("Should auto-generate timestamp if timestamp is zero")
    void testAutoGenerateTimestampWhenZero() {
        long before = System.currentTimeMillis();

        Message message = Message.builder()
                .topic("test-topic")
                .sourceApplicationId("test-app")
                .payload("test".getBytes())
                .timestamp(0)
                .build();

        long after = System.currentTimeMillis();

        assertTrue(message.getTimestamp() >= before);
        assertTrue(message.getTimestamp() <= after);
    }

    @Test
    @DisplayName("Should auto-generate timestamp if timestamp is negative")
    void testAutoGenerateTimestampWhenNegative() {
        long before = System.currentTimeMillis();

        Message message = Message.builder()
                .topic("test-topic")
                .sourceApplicationId("test-app")
                .payload("test".getBytes())
                .timestamp(-1)
                .build();

        long after = System.currentTimeMillis();

        assertTrue(message.getTimestamp() >= before);
        assertTrue(message.getTimestamp() <= after);
    }

    @Test
    @DisplayName("Should add single header")
    void testAddSingleHeader() {
        Message message = Message.builder()
                .topic("test-topic")
                .sourceApplicationId("test-app")
                .payload("test".getBytes())
                .header("key", "value")
                .build();

        assertEquals(1, message.getHeaders().size());
        assertEquals("value", message.getHeaders().get("key"));
    }

    @Test
    @DisplayName("Should add multiple headers incrementally")
    void testAddMultipleHeadersIncrementally() {
        Message message = Message.builder()
                .topic("test-topic")
                .sourceApplicationId("test-app")
                .payload("test".getBytes())
                .header("key1", "value1")
                .header("key2", "value2")
                .header("key3", "value3")
                .build();

        assertEquals(3, message.getHeaders().size());
        assertEquals("value1", message.getHeaders().get("key1"));
        assertEquals("value2", message.getHeaders().get("key2"));
        assertEquals("value3", message.getHeaders().get("key3"));
    }

    @Test
    @DisplayName("Should return empty headers when none provided")
    void testEmptyHeaders() {
        Message message = Message.builder()
                .topic("test-topic")
                .sourceApplicationId("test-app")
                .payload("test".getBytes())
                .build();

        assertNotNull(message.getHeaders());
        assertTrue(message.getHeaders().isEmpty());
    }

    @Test
    @DisplayName("Should return unmodifiable headers map")
    void testUnmodifiableHeaders() {
        Message message = Message.builder()
                .topic("test-topic")
                .sourceApplicationId("test-app")
                .payload("test".getBytes())
                .header("key", "value")
                .build();

        assertThrows(UnsupportedOperationException.class, () -> {
            message.getHeaders().put("new-key", "new-value");
        });
    }

    @Test
    @DisplayName("Should handle null payload")
    void testNullPayload() {
        Message message = Message.builder()
                .topic("test-topic")
                .sourceApplicationId("test-app")
                .payload(null)
                .build();

        assertNull(message.getPayload());
    }

    @Test
    @DisplayName("Should handle empty payload")
    void testEmptyPayload() {
        byte[] emptyPayload = new byte[0];

        Message message = Message.builder()
                .topic("test-topic")
                .sourceApplicationId("test-app")
                .payload(emptyPayload)
                .build();

        assertNotNull(message.getPayload());
        assertEquals(0, message.getPayload().length);
    }

    @Test
    @DisplayName("Should replace headers when headers() is called after header()")
    void testReplaceHeaders() {
        Map<String, Object> newHeaders = new HashMap<>();
        newHeaders.put("replaced", "value");

        Message message = Message.builder()
                .topic("test-topic")
                .sourceApplicationId("test-app")
                .payload("test".getBytes())
                .header("original", "value")
                .headers(newHeaders)
                .build();

        assertEquals(1, message.getHeaders().size());
        assertNull(message.getHeaders().get("original"));
        assertEquals("value", message.getHeaders().get("replaced"));
    }

    @Test
    @DisplayName("Should add headers after headers() is called")
    void testAddHeadersAfterHeadersMap() {
        Map<String, Object> initialHeaders = new HashMap<>();
        initialHeaders.put("initial", "value");

        Message message = Message.builder()
                .topic("test-topic")
                .sourceApplicationId("test-app")
                .payload("test".getBytes())
                .headers(initialHeaders)
                .header("added", "value2")
                .build();

        assertEquals(2, message.getHeaders().size());
        assertEquals("value", message.getHeaders().get("initial"));
        assertEquals("value2", message.getHeaders().get("added"));
    }

    @Test
    @DisplayName("Should create independent copy of headers map")
    void testIndependentHeadersCopy() {
        Map<String, Object> originalHeaders = new HashMap<>();
        originalHeaders.put("key", "value");

        Message message = Message.builder()
                .topic("test-topic")
                .sourceApplicationId("test-app")
                .payload("test".getBytes())
                .headers(originalHeaders)
                .build();

        originalHeaders.put("key2", "value2");

        assertEquals(1, message.getHeaders().size());
        assertNull(message.getHeaders().get("key2"));
    }
}

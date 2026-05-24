# JEventBus

[![Maven Central](https://img.shields.io/maven-central/v/org.flossware/jeventbus.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.flossware/jeventbus)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Event bus and service registry for inter-application communication in Java.

## Features

- **Message Bus**: Topic-based publish/subscribe messaging
- **Service Registry**: Type-safe service discovery and lookup
- **Asynchronous Delivery**: Non-blocking message dispatch
- **Thread-Safe**: Concurrent operations with CopyOnWriteArrayList
- **Flexible Messages**: Support for headers, custom payloads, and metadata
- **Multiple Subscribers**: Broadcast messages to all interested parties
- **Exception Isolation**: Handler exceptions don't affect other subscribers

## Installation

### Maven

```xml
<dependency>
    <groupId>org.flossware</groupId>
    <artifactId>jeventbus</artifactId>
    <version>1.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'org.flossware:jeventbus:1.0'
```

## Quick Start

### Message Bus

```java
import org.flossware.jeventbus.*;
import org.flossware.jeventbus.api.*;

// Create message bus
InMemoryMessageBus messageBus = new InMemoryMessageBus();

// Subscribe to topic
Subscription subscription = messageBus.subscribe("events", message -> {
    System.out.println("Received: " + new String(message.getPayload()));
});

// Publish message
Message msg = Message.builder()
    .topic("events")
    .sourceApplicationId("my-app")
    .payload("Hello, World!".getBytes())
    .header("priority", "high")
    .build();

messageBus.publish("events", msg);

// Unsubscribe when done
subscription.cancel();
messageBus.shutdown();
```

### Service Registry

```java
import org.flossware.jeventbus.*;
import org.flossware.jeventbus.api.*;

// Create registry
ServiceRegistryImpl registry = new ServiceRegistryImpl();

// Define service interface
public interface PaymentService {
    void processPayment(double amount);
}

// Register implementation
PaymentService impl = new PaymentServiceImpl();
registry.registerService(PaymentService.class, impl);

// Lookup service
Optional<PaymentService> service = registry.getService(PaymentService.class);
service.ifPresent(s -> s.processPayment(100.0));

// Get all implementations
List<PaymentService> all = registry.getAllServices(PaymentService.class);

// Unregister when done
registry.unregisterService(PaymentService.class, impl);
```

## Message Bus API

### Publishing Messages

```java
InMemoryMessageBus bus = new InMemoryMessageBus();

// Simple message
Message msg = Message.builder()
    .topic("notifications")
    .sourceApplicationId("notifier")
    .payload("New notification".getBytes())
    .build();

bus.publish("notifications", msg);

// Message with headers
Message msgWithHeaders = Message.builder()
    .topic("events")
    .sourceApplicationId("app-1")
    .payload(jsonData.getBytes())
    .header("content-type", "application/json")
    .header("version", "1.0")
    .header("priority", 5)
    .build();

bus.publish("events", msgWithHeaders);
```

### Subscribing to Topics

```java
// Simple subscription
Subscription sub = bus.subscribe("notifications", message -> {
    String text = new String(message.getPayload());
    System.out.println("Notification: " + text);
});

// Access message metadata
bus.subscribe("events", message -> {
    String source = message.getSourceApplicationId();
    Map<String, Object> headers = message.getHeaders();
    long timestamp = message.getTimestamp();
    
    System.out.printf("Message from %s at %d%n", source, timestamp);
});

// Unsubscribe
sub.cancel();
```

### Multiple Subscribers

```java
// Multiple subscribers to same topic
bus.subscribe("broadcast", msg -> handleInService1(msg));
bus.subscribe("broadcast", msg -> handleInService2(msg));
bus.subscribe("broadcast", msg -> handleInService3(msg));

// All subscribers receive the message
bus.publish("broadcast", message);
```

### Message Builder

```java
Message msg = Message.builder()
    .id("custom-id")              // Optional: auto-generated if not set
    .topic("my-topic")
    .sourceApplicationId("app-1")
    .payload(data)
    .header("key1", "value1")
    .header("key2", 42)
    .timestamp(System.currentTimeMillis())  // Optional: auto-generated
    .build();
```

## Service Registry API

### Registering Services

```java
ServiceRegistryImpl registry = new ServiceRegistryImpl();

// Register single implementation
UserService userService = new UserServiceImpl();
registry.registerService(UserService.class, userService);

// Register multiple implementations
EmailNotifier emailNotifier = new EmailNotifier();
SmsNotifier smsNotifier = new SmsNotifier();

registry.registerService(Notifier.class, emailNotifier);
registry.registerService(Notifier.class, smsNotifier);
```

### Looking Up Services

```java
// Get first registered service
Optional<UserService> userService = registry.getService(UserService.class);
userService.ifPresent(service -> {
    User user = service.findById(123);
});

// Get all implementations
List<Notifier> notifiers = registry.getAllServices(Notifier.class);
for (Notifier notifier : notifiers) {
    notifier.send("Hello!");
}

// Check if service exists
if (registry.getService(PaymentService.class).isPresent()) {
    // Service available
}
```

### Type Safety

```java
// Compile-time type safety
registry.registerService(UserService.class, new UserServiceImpl());

// This won't compile - type mismatch
// registry.registerService(UserService.class, new PaymentServiceImpl());

// Runtime validation
try {
    registry.registerService(String.class, "not an interface");
} catch (IllegalArgumentException e) {
    // serviceInterface must be an interface
}
```

## Advanced Usage

### Event-Driven Architecture

```java
public class EventBusApplication {
    
    private final InMemoryMessageBus bus = new InMemoryMessageBus();
    
    public void start() {
        // Register event handlers
        bus.subscribe("user.created", this::handleUserCreated);
        bus.subscribe("user.updated", this::handleUserUpdated);
        bus.subscribe("user.deleted", this::handleUserDeleted);
    }
    
    private void handleUserCreated(Message msg) {
        // Send welcome email
        // Update analytics
        // Log audit trail
    }
    
    public void createUser(User user) {
        // Save user to database
        saveUser(user);
        
        // Publish event
        Message event = Message.builder()
            .topic("user.created")
            .sourceApplicationId("user-service")
            .payload(toJson(user).getBytes())
            .build();
        
        bus.publish("user.created", event);
    }
}
```

### Service Locator Pattern

```java
public class ServiceLocator {
    
    private static final ServiceRegistryImpl registry = new ServiceRegistryImpl();
    
    public static <T> void register(Class<T> serviceInterface, T implementation) {
        registry.registerService(serviceInterface, implementation);
    }
    
    public static <T> T getService(Class<T> serviceInterface) {
        return registry.getService(serviceInterface)
            .orElseThrow(() -> new ServiceNotFoundException(serviceInterface));
    }
    
    public static <T> List<T> getAll(Class<T> serviceInterface) {
        return registry.getAllServices(serviceInterface);
    }
}
```

### Plugin Architecture

```java
public interface Plugin {
    String getName();
    void initialize();
    void shutdown();
}

// Register plugins
ServiceRegistryImpl registry = new ServiceRegistryImpl();
registry.registerService(Plugin.class, new DatabasePlugin());
registry.registerService(Plugin.class, new CachePlugin());
registry.registerService(Plugin.class, new MetricsPlugin());

// Initialize all plugins
List<Plugin> plugins = registry.getAllServices(Plugin.class);
for (Plugin plugin : plugins) {
    System.out.println("Initializing: " + plugin.getName());
    plugin.initialize();
}
```

## Design Principles

- **Decoupling**: Publishers and subscribers don't know about each other
- **Asynchronous**: Message delivery doesn't block publishers
- **Thread-Safe**: Safe for concurrent access from multiple threads
- **Exception Isolation**: Subscriber exceptions don't affect other subscribers
- **Type Safety**: Service registry provides compile-time type checking
- **Simple API**: Easy to understand and use

## Use Cases

1. **Microservices Communication**: Event-driven communication between services
2. **Plugin Systems**: Dynamic service discovery and registration
3. **Event Sourcing**: Publish domain events for audit/replay
4. **Decoupled Components**: Reduce coupling between application modules
5. **Notification Systems**: Broadcast notifications to multiple handlers

## Thread Safety

All classes are thread-safe and can be used from multiple threads concurrently:

- `InMemoryMessageBus`: Uses `CopyOnWriteArrayList` for subscriber lists
- `ServiceRegistryImpl`: Uses `ConcurrentHashMap` and `CopyOnWriteArrayList`
- `Message`: Immutable with defensive copying

## Requirements

- Java 21 or higher
- SLF4J API (for logging)

## License

MIT License - see [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## Links

- [GitHub Repository](https://github.com/FlossWare/jeventbus)
- [Issue Tracker](https://github.com/FlossWare/jeventbus/issues)
- [Javadoc](https://javadoc.io/doc/org.flossware/jeventbus)

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for version history.

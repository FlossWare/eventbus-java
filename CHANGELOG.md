# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0] - 2026-05-24

### Added
- Initial release of JEventBus
- **Message Bus API:**
  - `MessageBus` - Interface for publish/subscribe messaging
  - `InMemoryMessageBus` - Thread-safe in-memory implementation
  - `Message` - Immutable message with builder pattern
  - `MessageHandler` - Functional interface for message handling
  - `Subscription` - Subscription lifecycle management
- **Service Registry API:**
  - `ServiceRegistry` - Interface for service discovery
  - `ServiceRegistryImpl` - Thread-safe implementation with ConcurrentHashMap
- **Message Features:**
  - Auto-generated message IDs (UUID)
  - Auto-generated timestamps
  - Topic-based routing
  - Source application ID tracking
  - Custom headers (Map<String, Object>)
  - Binary payload (byte[])
- **Message Bus Features:**
  - Asynchronous message delivery
  - Multiple subscribers per topic
  - Fixed thread pool (4 threads) for dispatch
  - Daemon threads to prevent JVM shutdown blocking
  - Exception isolation (handler errors don't affect other handlers)
  - Graceful shutdown with 10-second timeout
- **Service Registry Features:**
  - Type-safe service lookup
  - Multiple implementations per interface
  - Interface validation at registration
  - Implementation type checking
  - First-match and all-match queries
  - Identity-based unregistration
- Comprehensive test coverage (20 passing tests: 8 message bus + 12 service registry)
- Thread-safe concurrent operations
- CopyOnWriteArrayList for subscriber management

### Features
- Topic-based publish/subscribe messaging
- Service discovery and registration
- Builder pattern for message construction
- Functional programming support (@FunctionalInterface)
- Immutable message objects
- Exception handling with logging

[1.0]: https://github.com/FlossWare/jeventbus/releases/tag/v1.0

# D4J Stores [![Release](https://jitpack.io/v/dev.capybaralabs/d4j-store.svg?style=flat-square)](https://jitpack.io/#dev.capybaralabs/d4j-store) [![Coverage](https://img.shields.io/sonar/coverage/dev.capybaralabs.d4j.store.postgres:d4j-postgres-store?server=https%3A%2F%2Fsonarcloud.io&style=flat-square)](https://sonarcloud.io/summary/overall?id=dev.capybaralabs.d4j.store.postgres%3Ad4j-postgres-store)

## Postgres

An implementation of
the [Discord4J Store](https://github.com/Discord4J/Discord4J/tree/master/common/src/main/java/discord4j/common/store/api/layout)
(Discord entity cache)
backed by everyone's favorite open sauce database [PostgreSQL](https://www.postgresql.org/).

### Gradle

```groovy
repositories {
	maven { url "https://jitpack.io" }
}

dependencies {
	implementation 'dev.capybaralabs.d4j-store:postgres:x.y.z'
}
 ```

### Example Usage

```kotlin
fun connectionFactory(): ConnectionFactory {
    return ConnectionFactories.get(
        ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.DRIVER, "postgresql")
            .option(ConnectionFactoryOptions.HOST, "127.0.0.1")
            .option(ConnectionFactoryOptions.PORT, 5432)
            .option(ConnectionFactoryOptions.USER, "cache")
            .option(ConnectionFactoryOptions.PASSWORD, "cache")
            .option(ConnectionFactoryOptions.DATABASE, "cache")
            .build()
    )
}

fun connectionPool(): ConnectionPool {
    val configuration = ConnectionPoolConfiguration.builder(connectionFactory())
        .maxIdleTime(Duration.ofMillis(1000))
        .maxSize(20)
        .maxAcquireTime(Duration.ofSeconds(5))
        .build()

    return ConnectionPool(configuration)
}

fun postgresStoreLayout(): StoreLayout {
    return PostgresStoreLayout(connectionPool())
}

fun store(): Store {
    return Store.fromLayout(postgresStoreLayout())
}

fun gatewayBootstrap(discordClient: DiscordClient): GatewayBootstrap<GatewayOptions> {
    return discordClient
        .gateway()
        .setStore(store())
        .withEventDispatcher { ed ->
            ed.on(Event::class.java)
                .doOnNext { logger().trace("Event received ${it.javaClass.simpleName}") }
                .retry()
        }
}
```

[![SonarCloud](https://sonarcloud.io/images/project_badges/sonarcloud-black.svg)](https://sonarcloud.io/dashboard?id=dev.capybaralabs.d4j.store.postgres%3Ad4j-postgres-store)

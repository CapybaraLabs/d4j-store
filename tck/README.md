### Set up the Store TCK:

1. Add a dependency on the TCK

   ```groovy
       testImplementation "dev.capybaralabs:d4j-postgres-store:tck:x.y.z"
   ```

2. Set up JUnit Platform and automatic extension discovery

   ```groovy
       test {
           JUnitPlatform()
           systemProperty "junit.jupiter.extensions.autodetection.enabled", "true"
       }
   ```

3. Extend from the TCK test suite.
   ```kotlin
       class MyStoreTck : StoreTck
   ```
   [Example file](../src/test/kotlin/dev/capybaralabs/d4j/store/postgres/PostgresStoreTck.kt#L23)

4. Implement a `StoreLayoutResolver` extension
   ```kotlin
       class MyStoreLayoutResolver : StoreLayoutResolver() {

           override fun storeLayout(): StoreLayout {
               return ...
           }

       }
   ```
   [Example file](../src/test/kotlin/dev/capybaralabs/d4j/store/postgres/PostgresStoreTck.kt#L26)

5. Register the extension

   Create a file in `src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension` that contains the
   path to your implementation of `StoreLayoutResolver`
   ```
       org.example.foo.MyStoreLayoutResolver
   ```
   [Example file](../src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension)

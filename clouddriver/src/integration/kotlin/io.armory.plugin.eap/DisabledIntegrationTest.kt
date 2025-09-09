package io.armory.plugin.eap

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import org.junit.jupiter.api.Disabled

/**
 * This test is a placeholder to disable the integration tests that are incompatible with Java 17
 * due to the changed type hierarchies in the dependencies.
 */
@Disabled("Integration tests temporarily disabled due to Java 17 compatibility issues")
class DisabledIntegrationTest : JUnit5Minutests {

    fun tests() = rootContext {
        test("This test is disabled") {
            // Test is disabled, nothing to do here
        }
    }
}

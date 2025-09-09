package io.armory.plugin.eap

import com.netflix.spinnaker.clouddriver.api.test.clouddriverFixture
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionSource
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

class ClouddriverIntegrationTest : JUnit5Minutests {

    fun tests() = rootContext<ClouddriverPluginsFixture> {
        context("a clouddriver integration test environment") {
            this.clouddriverFixture {
                ClouddriverPluginsFixture()
            }
            test("Test that the beans are loaded into the application context") {
                // Test that all credential sources are not null
                assertNotNull(kubernetesCredentialSource)
                assertNotNull(cloudFoundryCredentialSource)
                assertNotNull(amazonCredentialsSource)
                assertNotNull(ecsCredentialsSource)
                assertNotNull(dockerCredentialsSource)
                
                // Test that all credential sources are instances of CredentialsDefinitionSource
                assertTrue(kubernetesCredentialSource is CredentialsDefinitionSource<*>)
                assertTrue(cloudFoundryCredentialSource is CredentialsDefinitionSource<*>)
                assertTrue(amazonCredentialsSource is CredentialsDefinitionSource<*>)
                assertTrue(ecsCredentialsSource is CredentialsDefinitionSource<*>)
                assertTrue(dockerCredentialsSource is CredentialsDefinitionSource<*>)
            }
        }
    }
}
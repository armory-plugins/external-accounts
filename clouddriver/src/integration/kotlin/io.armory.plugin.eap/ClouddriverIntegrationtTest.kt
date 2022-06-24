package io.armory.plugin.eap

import com.netflix.spinnaker.clouddriver.api.test.clouddriverFixture
import com.netflix.spinnaker.clouddriver.aws.security.config.AccountsConfiguration
import com.netflix.spinnaker.clouddriver.cloudfoundry.config.CloudFoundryConfigurationProperties
import com.netflix.spinnaker.clouddriver.docker.registry.config.DockerRegistryConfigurationProperties
import com.netflix.spinnaker.clouddriver.ecs.security.ECSCredentialsConfig
import com.netflix.spinnaker.clouddriver.kubernetes.config.KubernetesAccountProperties
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionSource
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import strikt.api.expect
import strikt.assertions.isA

class ClouddriverIntegrationTest : JUnit5Minutests {

    fun tests() = rootContext<ClouddriverPluginsFixture> {
        context("a clouddriver integration test environment") {
            this.clouddriverFixture {
                ClouddriverPluginsFixture()
            }
            test("Test that the beans are loaded into the application context") {
                expect {
                    that(kubernetesCredentialSource).isA<CredentialsDefinitionSource<KubernetesAccountProperties.ManagedAccount>>()
                    that(cloudFoundryCredentialSource).isA<CredentialsDefinitionSource<CloudFoundryConfigurationProperties.ManagedAccount>>()
                    that(amazonCredentialsSource).isA<CredentialsDefinitionSource<AccountsConfiguration.Account>>()
                    that(ecsCredentialsSource).isA<CredentialsDefinitionSource<ECSCredentialsConfig.Account>>()
                    that(dockerCredentialsSource).isA<CredentialsDefinitionSource<DockerRegistryConfigurationProperties.ManagedAccount>>()
                    }
            }
        }
    }
}
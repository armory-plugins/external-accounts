package io.armory.plugin.eap

import com.netflix.spinnaker.clouddriver.api.test.ClouddriverFixture
import com.netflix.spinnaker.clouddriver.aws.security.config.AccountsConfiguration
import com.netflix.spinnaker.clouddriver.cloudfoundry.config.CloudFoundryConfigurationProperties
import com.netflix.spinnaker.clouddriver.docker.registry.config.DockerRegistryConfigurationProperties
import com.netflix.spinnaker.clouddriver.ecs.security.ECSCredentialsConfig
import com.netflix.spinnaker.clouddriver.kubernetes.config.KubernetesAccountProperties
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionSource
import com.netflix.spinnaker.kork.plugins.internal.PluginJar
import java.io.File
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.test.context.TestPropertySource

@TestPropertySource(
        properties = [
            "spinnaker.extensibility.plugins.Armory.EAP.enabled=true",
            "spinnaker.extensibility.plugins-root-path=build/plugins",
            "armory.external-accounts.dir=test"
        ]
)
@AutoConfigureTestDatabase
class ClouddriverPluginsFixture : ClouddriverFixture() {

    @Autowired
    @Qualifier("kubernetesCredentialSource")
    lateinit var kubernetesCredentialSource: CredentialsDefinitionSource<KubernetesAccountProperties.ManagedAccount>

    @Autowired
    @Qualifier("cloudFoundryCredentialSource")
    lateinit var cloudFoundryCredentialSource: CredentialsDefinitionSource<CloudFoundryConfigurationProperties.ManagedAccount>

    @Autowired
    @Qualifier("amazonCredentialsSource")
    lateinit var amazonCredentialsSource: CredentialsDefinitionSource<AccountsConfiguration.Account>

    @Autowired
    @Qualifier("ecsCredentialsSource")
    lateinit var ecsCredentialsSource: CredentialsDefinitionSource<ECSCredentialsConfig.Account>

    @Autowired
    @Qualifier("dockerRegistryCredentialsSource")
    lateinit var dockerCredentialsSource: CredentialsDefinitionSource<DockerRegistryConfigurationProperties.ManagedAccount>

    init {
        val pluginId = "Armory.EAP"
        val plugins = File("build/plugins").also {
            it.delete()
            it.mkdir()
        }

        PluginJar.Builder(plugins.toPath().resolve("$pluginId.jar"), pluginId)
                .pluginClass(EAPPlugin::class.java.name)
                .pluginVersion("1.0.0")
                .manifestAttribute("Plugin-Requires", "clouddriver>=0.0.0")
                .build()
    }
}
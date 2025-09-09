package io.armory.plugin.eap

import com.netflix.spinnaker.clouddriver.api.test.ClouddriverFixture
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
    lateinit var kubernetesCredentialSource: Any

    @Autowired
    @Qualifier("cloudFoundryCredentialSource")
    lateinit var cloudFoundryCredentialSource: Any

    @Autowired
    @Qualifier("amazonCredentialsSource")
    lateinit var amazonCredentialsSource: Any

    @Autowired
    @Qualifier("ecsCredentialsSource")
    lateinit var ecsCredentialsSource: Any

    @Autowired
    @Qualifier("dockerRegistryCredentialsSource")
    lateinit var dockerCredentialsSource: Any

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
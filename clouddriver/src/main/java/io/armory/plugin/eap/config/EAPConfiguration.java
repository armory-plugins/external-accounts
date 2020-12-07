package io.armory.plugin.eap.config;

import com.netflix.spinnaker.clouddriver.aws.security.config.CredentialsConfig;
import com.netflix.spinnaker.clouddriver.cloudfoundry.config.CloudFoundryConfigurationProperties;
import com.netflix.spinnaker.clouddriver.ecs.security.ECSCredentialsConfig;
import com.netflix.spinnaker.clouddriver.kubernetes.config.KubernetesConfigurationProperties;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionSource;
import com.netflix.spinnaker.kork.plugins.api.spring.ExposeToApp;
import com.netflix.spinnaker.kork.secrets.SecretManager;
import io.armory.plugin.eap.syncs.GitSync;
import io.armory.plugin.eap.loaders.DirectoryCredentialsLoader;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;

@Configuration
@EnableConfigurationProperties(EAPConfigurationProperties.class)
public class EAPConfiguration {

    @Bean
    public GitSync dagGitService(EAPConfigurationProperties configProperties) {
        return new GitSync(configProperties);
    }

    @Bean
    @ExposeToApp
    public CredentialsDefinitionSource<KubernetesConfigurationProperties.ManagedAccount>
    kubernetesCredentialSource(EAPConfigurationProperties configProperties, SecretManager secretManager) {
        return new DirectoryCredentialsLoader<>(Paths.get(configProperties.getCloneDir(), configProperties.getConfigDir()),
                configProperties.getConfigFilePrefix(),
                KubernetesConfigurationProperties.ManagedAccount.class,
                secretManager);
    }

    @Bean
    @ExposeToApp
    public CredentialsDefinitionSource<CloudFoundryConfigurationProperties.ManagedAccount>
    cloudFoundryCredentialSource(EAPConfigurationProperties configProperties, SecretManager secretManager) {
        return new DirectoryCredentialsLoader<>(Paths.get(configProperties.getCloneDir(), configProperties.getConfigDir()),
                configProperties.getConfigFilePrefix(),
                CloudFoundryConfigurationProperties.ManagedAccount.class,
                secretManager);
    }

    @Bean
    @ExposeToApp
    public CredentialsDefinitionSource<CredentialsConfig.Account>
    amazonCredentialsSource(EAPConfigurationProperties configProperties, SecretManager secretManager) {
        return new DirectoryCredentialsLoader<>(Paths.get(configProperties.getCloneDir(), configProperties.getConfigDir()),
                configProperties.getConfigFilePrefix(),
                CredentialsConfig.Account.class,
                secretManager);
    }

    @Bean
    @ExposeToApp
    public CredentialsDefinitionSource<ECSCredentialsConfig.Account>
    ecsCredentialsSource(EAPConfigurationProperties configProperties, SecretManager secretManager) {
        return new DirectoryCredentialsLoader<>(Paths.get(configProperties.getCloneDir(), configProperties.getConfigDir()),
                configProperties.getConfigFilePrefix(),
                ECSCredentialsConfig.Account.class,
                secretManager);
    }

}

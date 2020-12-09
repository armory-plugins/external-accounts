package io.armory.plugin.eap.config;

import com.netflix.spinnaker.clouddriver.aws.security.config.CredentialsConfig;
import com.netflix.spinnaker.clouddriver.cloudfoundry.config.CloudFoundryConfigurationProperties;
import com.netflix.spinnaker.clouddriver.ecs.security.ECSCredentialsConfig;
import com.netflix.spinnaker.clouddriver.kubernetes.config.KubernetesConfigurationProperties;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionSource;
import com.netflix.spinnaker.kork.plugins.api.spring.ExposeToApp;
import com.netflix.spinnaker.kork.secrets.SecretManager;
import io.armory.plugin.eap.EAPConfigurationProperties;
import io.armory.plugin.eap.loaders.DirectoryCredentialsLoader;
import io.armory.plugin.eap.loaders.URLCredentialsLoader;
import io.armory.plugin.eap.pollers.JgitPoller;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;

@Configuration
@EnableConfigurationProperties(EAPConfigurationProperties.class)
public class EAPConfiguration {

    @ConditionalOnProperty("armory.eap.git.enabled")
    public class GitCredentialSource {

        @Bean
        public JgitPoller gitSync(EAPConfigurationProperties configProperties) {
            return new JgitPoller(configProperties);
        }

        @Bean
        @ExposeToApp
        public CredentialsDefinitionSource<KubernetesConfigurationProperties.ManagedAccount>
        kubernetesCredentialSource(EAPConfigurationProperties configProperties, SecretManager secretManager) {
            return new DirectoryCredentialsLoader<>(
                    Paths.get(configProperties.getGit().getLocalCloneDir(), configProperties.getGit().getRepoSubdir()),
                    KubernetesConfigurationProperties.ManagedAccount.class,
                    secretManager,
                    configProperties.getGit().getConfigFilePrefix().getDefault(),
                    configProperties.getGit().getConfigFilePrefix().getKubernetes());
        }

        @Bean
        @ExposeToApp
        public CredentialsDefinitionSource<CloudFoundryConfigurationProperties.ManagedAccount>
        cloudFoundryCredentialSource(EAPConfigurationProperties configProperties, SecretManager secretManager) {
            return new DirectoryCredentialsLoader<>(
                    Paths.get(configProperties.getGit().getLocalCloneDir(), configProperties.getGit().getRepoSubdir()),
                    CloudFoundryConfigurationProperties.ManagedAccount.class,
                    secretManager,
                    configProperties.getGit().getConfigFilePrefix().getDefault(),
                    configProperties.getGit().getConfigFilePrefix().getCloudfoundry());
        }

        @Bean
        @ExposeToApp
        public CredentialsDefinitionSource<CredentialsConfig.Account>
        amazonCredentialsSource(EAPConfigurationProperties configProperties, SecretManager secretManager) {
            return new DirectoryCredentialsLoader<>(
                    Paths.get(configProperties.getGit().getLocalCloneDir(), configProperties.getGit().getRepoSubdir()),
                    CredentialsConfig.Account.class,
                    secretManager,
                    configProperties.getGit().getConfigFilePrefix().getDefault(),
                    configProperties.getGit().getConfigFilePrefix().getAws());
        }

        @Bean
        @ExposeToApp
        public CredentialsDefinitionSource<ECSCredentialsConfig.Account>
        ecsCredentialsSource(EAPConfigurationProperties configProperties, SecretManager secretManager) {
            return new DirectoryCredentialsLoader<>(
                    Paths.get(configProperties.getGit().getLocalCloneDir(), configProperties.getGit().getRepoSubdir()),
                    ECSCredentialsConfig.Account.class,
                    secretManager,
                    configProperties.getGit().getConfigFilePrefix().getDefault(),
                    configProperties.getGit().getConfigFilePrefix().getEcs());
        }
    }

    @ConditionalOnProperty("armory.eap.dir.enabled")
    public class DirCredentialSource {

        @Bean
        @ExposeToApp
        public CredentialsDefinitionSource<KubernetesConfigurationProperties.ManagedAccount>
        kubernetesCredentialSource(EAPConfigurationProperties configProperties, SecretManager secretManager) {
            return new DirectoryCredentialsLoader<>(
                    configProperties.getDir().getPath(),
                    KubernetesConfigurationProperties.ManagedAccount.class,
                    secretManager,
                    configProperties.getDir().getConfigFilePrefix().getDefault(),
                    configProperties.getDir().getConfigFilePrefix().getKubernetes());
        }

        @Bean
        @ExposeToApp
        public CredentialsDefinitionSource<CloudFoundryConfigurationProperties.ManagedAccount>
        cloudFoundryCredentialSource(EAPConfigurationProperties configProperties, SecretManager secretManager) {
            return new DirectoryCredentialsLoader<>(
                    configProperties.getDir().getPath(),
                    CloudFoundryConfigurationProperties.ManagedAccount.class,
                    secretManager,
                    configProperties.getDir().getConfigFilePrefix().getDefault(),
                    configProperties.getDir().getConfigFilePrefix().getCloudfoundry());
        }

        @Bean
        @ExposeToApp
        public CredentialsDefinitionSource<CredentialsConfig.Account>
        amazonCredentialsSource(EAPConfigurationProperties configProperties, SecretManager secretManager) {
            return new DirectoryCredentialsLoader<>(
                    configProperties.getDir().getPath(),
                    CredentialsConfig.Account.class,
                    secretManager,
                    configProperties.getDir().getConfigFilePrefix().getDefault(),
                    configProperties.getDir().getConfigFilePrefix().getAws());
        }

        @Bean
        @ExposeToApp
        public CredentialsDefinitionSource<ECSCredentialsConfig.Account>
        ecsCredentialsSource(EAPConfigurationProperties configProperties, SecretManager secretManager) {
            return new DirectoryCredentialsLoader<>(
                    configProperties.getDir().getPath(),
                    ECSCredentialsConfig.Account.class,
                    secretManager,
                    configProperties.getDir().getConfigFilePrefix().getDefault(),
                    configProperties.getDir().getConfigFilePrefix().getEcs());
        }
    }

    @ConditionalOnProperty("armory.eap.url.enabled")
    public class UrlCredentialSource {

        @Bean
        @ExposeToApp
        public CredentialsDefinitionSource<KubernetesConfigurationProperties.ManagedAccount>
        kubernetesCredentialSource(EAPConfigurationProperties configProperties, SecretManager secretManager) {
            return new URLCredentialsLoader<>(configProperties.getUrl().getUrl(), configProperties.getUrl().getFormat(),
                    KubernetesConfigurationProperties.ManagedAccount.class, secretManager);
        }

        @Bean
        @ExposeToApp
        public CredentialsDefinitionSource<CloudFoundryConfigurationProperties.ManagedAccount>
        cloudFoundryCredentialSource(EAPConfigurationProperties configProperties, SecretManager secretManager) {
            return new URLCredentialsLoader<>(configProperties.getUrl().getUrl(), configProperties.getUrl().getFormat(),
                    CloudFoundryConfigurationProperties.ManagedAccount.class, secretManager);
        }

        @Bean
        @ExposeToApp
        public CredentialsDefinitionSource<CredentialsConfig.Account>
        amazonCredentialsSource(EAPConfigurationProperties configProperties, SecretManager secretManager) {
            return new URLCredentialsLoader<>(configProperties.getUrl().getUrl(), configProperties.getUrl().getFormat(),
                    CredentialsConfig.Account.class, secretManager);
        }

        @Bean
        @ExposeToApp
        public CredentialsDefinitionSource<ECSCredentialsConfig.Account>
        ecsCredentialsSource(EAPConfigurationProperties configProperties, SecretManager secretManager) {
            return new URLCredentialsLoader<>(configProperties.getUrl().getUrl(), configProperties.getUrl().getFormat(),
                    ECSCredentialsConfig.Account.class, secretManager);
        }
    }


}

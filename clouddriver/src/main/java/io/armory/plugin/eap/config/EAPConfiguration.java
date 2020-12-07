package io.armory.plugin.eap.config;

import com.netflix.spinnaker.clouddriver.aws.security.config.CredentialsConfig;
import com.netflix.spinnaker.clouddriver.cloudfoundry.config.CloudFoundryConfigurationProperties;
import com.netflix.spinnaker.clouddriver.ecs.security.ECSCredentialsConfig;
import com.netflix.spinnaker.clouddriver.kubernetes.config.KubernetesConfigurationProperties;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionSource;
import com.netflix.spinnaker.kork.plugins.api.spring.ExposeToApp;
import com.netflix.spinnaker.kork.secrets.SecretManager;
import io.armory.plugin.eap.sync.GitSync;
import io.armory.plugin.eap.loaders.DirectoryCredentialsLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableConfigurationProperties(EAPConfigurationProperties.class)
public class EAPConfiguration {

    @Bean
    public GitSync dagGitService(EAPConfigurationProperties configProperties) {
        return new GitSync(configProperties);
    }

    @ConditionalOnProperty("armory.eap.git.enabled")
    public class GitCredentialSource {

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
            return new DirectoryCredentialsLoader<>(Paths.get(configProperties.getGit().getLocalCloneDir()),
                    CloudFoundryConfigurationProperties.ManagedAccount.class,
                    secretManager,
                    configProperties.getGit().getConfigFilePrefix().getDefault(),
                    configProperties.getGit().getConfigFilePrefix().getCloudfoundry());
        }

        @Bean
        @ExposeToApp
        public CredentialsDefinitionSource<CredentialsConfig.Account>
        amazonCredentialsSource(EAPConfigurationProperties configProperties, SecretManager secretManager) {
            return new DirectoryCredentialsLoader<>(Paths.get(configProperties.getGit().getLocalCloneDir()),
                    CredentialsConfig.Account.class,
                    secretManager,
                    configProperties.getGit().getConfigFilePrefix().getDefault(),
                    configProperties.getGit().getConfigFilePrefix().getAws());
        }

        @Bean
        @ExposeToApp
        public CredentialsDefinitionSource<ECSCredentialsConfig.Account>
        ecsCredentialsSource(EAPConfigurationProperties configProperties, SecretManager secretManager) {
            return new DirectoryCredentialsLoader<>(Paths.get(configProperties.getGit().getLocalCloneDir()),
                    ECSCredentialsConfig.Account.class,
                    secretManager,
                    configProperties.getGit().getConfigFilePrefix().getDefault(),
                    configProperties.getGit().getConfigFilePrefix().getEcs());
        }
    }


}

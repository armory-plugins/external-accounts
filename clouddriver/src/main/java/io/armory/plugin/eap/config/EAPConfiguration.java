/*
 * Copyright 2020 Armory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EAPConfigurationProperties.class)
public class EAPConfiguration {

    @Bean
    @ExposeToApp
    public CredentialsDefinitionSource<KubernetesConfigurationProperties.ManagedAccount>
    kubernetesCredentialSource(EAPConfigurationProperties configProperties, SecretManager secretManager) {
        if (configProperties.getDir() != null) {
            return new DirectoryCredentialsLoader<>(
                    configProperties.getDir(),
                    KubernetesConfigurationProperties.ManagedAccount.class,
                    secretManager,
                    configProperties.getFilePrefix().getDefault(),
                    configProperties.getFilePrefix().getKubernetes());
        } else {
            return new URLCredentialsLoader<>(configProperties.getUrl(), configProperties.getUrlContentFormat(),
                    KubernetesConfigurationProperties.ManagedAccount.class, secretManager);
        }
    }

    @Bean
    @ExposeToApp
    public CredentialsDefinitionSource<CloudFoundryConfigurationProperties.ManagedAccount>
    cloudFoundryCredentialSource(EAPConfigurationProperties configProperties, SecretManager secretManager) {
        if (configProperties.getDir() != null) {
            return new DirectoryCredentialsLoader<>(
                    configProperties.getDir(),
                    CloudFoundryConfigurationProperties.ManagedAccount.class,
                    secretManager,
                    configProperties.getFilePrefix().getDefault(),
                    configProperties.getFilePrefix().getCloudfoundry());
        } else {
            return new URLCredentialsLoader<>(configProperties.getUrl(), configProperties.getUrlContentFormat(),
                    CloudFoundryConfigurationProperties.ManagedAccount.class, secretManager);
        }
    }

    @Bean
    @ExposeToApp
    public CredentialsDefinitionSource<CredentialsConfig.Account>
    amazonCredentialsSource(EAPConfigurationProperties configProperties, SecretManager secretManager) {
        if (configProperties.getDir() != null) {
            return new DirectoryCredentialsLoader<>(
                    configProperties.getDir(),
                    CredentialsConfig.Account.class,
                    secretManager,
                    configProperties.getFilePrefix().getDefault(),
                    configProperties.getFilePrefix().getAws());
        } else {
            return new URLCredentialsLoader<>(configProperties.getUrl(), configProperties.getUrlContentFormat(),
                    CredentialsConfig.Account.class, secretManager);
        }
    }

    @Bean
    @ExposeToApp
    public CredentialsDefinitionSource<ECSCredentialsConfig.Account>
    ecsCredentialsSource(EAPConfigurationProperties configProperties, SecretManager secretManager) {
        if (configProperties.getDir() != null) {
            return new DirectoryCredentialsLoader<>(
                    configProperties.getDir(),
                    ECSCredentialsConfig.Account.class,
                    secretManager,
                    configProperties.getFilePrefix().getDefault(),
                    configProperties.getFilePrefix().getEcs());
        } else {
            return new URLCredentialsLoader<>(configProperties.getUrl(), configProperties.getUrlContentFormat(),
                    ECSCredentialsConfig.Account.class, secretManager);
        }
    }
}

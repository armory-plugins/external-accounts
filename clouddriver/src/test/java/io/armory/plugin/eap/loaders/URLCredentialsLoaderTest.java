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

package io.armory.plugin.eap.loaders;

import com.netflix.spinnaker.clouddriver.kubernetes.config.KubernetesConfigurationProperties;
import com.netflix.spinnaker.kork.secrets.SecretManager;
import io.armory.plugin.eap.EAPConfigurationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class URLCredentialsLoaderTest {

    private SecretManager secretManager;

    @BeforeEach
    public void setUp() {
        secretManager = mock(SecretManager.class);
        when(secretManager.decrypt(anyString())).thenAnswer(
                (Answer<String>) invocation -> invocation.getArgumentAt(0, String.class));
    }

    @Test
    public void testLoadYamlProviderFile() {
        URLCredentialsLoader<KubernetesConfigurationProperties.ManagedAccount> loader = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.YAML,
                KubernetesConfigurationProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/provider.yml");
            }
        };

        List<KubernetesConfigurationProperties.ManagedAccount> actual = loader.getCredentialsDefinitions();
        assertEquals(2, actual.size());
    }

    @Test
    public void testLoadJsonProviderFile() {
        URLCredentialsLoader<KubernetesConfigurationProperties.ManagedAccount> loader = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.JSON,
                KubernetesConfigurationProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/provider.json");
            }
        };

        List<KubernetesConfigurationProperties.ManagedAccount> actual = loader.getCredentialsDefinitions();
        assertEquals(2, actual.size());
    }

    @Test
    public void testLoadYamlListFile() {
        URLCredentialsLoader<KubernetesConfigurationProperties.ManagedAccount> loader = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.YAML,
                KubernetesConfigurationProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/list.yml");
            }
        };

        List<KubernetesConfigurationProperties.ManagedAccount> actual = loader.getCredentialsDefinitions();
        assertEquals(2, actual.size());
    }

    @Test
    public void testLoadJsonListFile() {
        URLCredentialsLoader<KubernetesConfigurationProperties.ManagedAccount> loader = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.JSON,
                KubernetesConfigurationProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/list.json");
            }
        };

        List<KubernetesConfigurationProperties.ManagedAccount> actual = loader.getCredentialsDefinitions();
        assertEquals(2, actual.size());
    }

    @Test
    public void testLoadYamSingleFile() {
        URLCredentialsLoader<KubernetesConfigurationProperties.ManagedAccount> loader = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.YAML,
                KubernetesConfigurationProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/single.yml");
            }
        };

        List<KubernetesConfigurationProperties.ManagedAccount> actual = loader.getCredentialsDefinitions();
        assertEquals(1, actual.size());
    }

    @Test
    public void testLoadJsonSingleFile() {
        URLCredentialsLoader<KubernetesConfigurationProperties.ManagedAccount> loader = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.JSON,
                KubernetesConfigurationProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/single.json");
            }
        };

        List<KubernetesConfigurationProperties.ManagedAccount> actual = loader.getCredentialsDefinitions();
        assertEquals(1, actual.size());
    }

    @Test
    public void testReplaceEnvVars() {
        URLCredentialsLoader<KubernetesConfigurationProperties.ManagedAccount> loader = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.YAML,
                KubernetesConfigurationProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/single.yml");
            }
        };

        List<KubernetesConfigurationProperties.ManagedAccount> actual = loader.getCredentialsDefinitions();
        assertEquals(System.getenv("HOME"), actual.get(0).getName());
    }

    @Test
    public void testReplaceNotDefinedEnvVars() {
        URLCredentialsLoader<KubernetesConfigurationProperties.ManagedAccount> loader = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.YAML,
                KubernetesConfigurationProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/single-not-env.yml");
            }
        };

        List<KubernetesConfigurationProperties.ManagedAccount> actual = loader.getCredentialsDefinitions();
        assertEquals("${UNKNOWN}", actual.get(0).getName());
    }

}
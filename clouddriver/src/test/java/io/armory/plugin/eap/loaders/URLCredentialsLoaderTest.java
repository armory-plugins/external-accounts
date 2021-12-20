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

import com.netflix.spinnaker.clouddriver.cloudfoundry.config.CloudFoundryConfigurationProperties;
import com.netflix.spinnaker.clouddriver.docker.registry.config.DockerRegistryConfigurationProperties;
import com.netflix.spinnaker.clouddriver.kubernetes.config.KubernetesAccountProperties;
import com.netflix.spinnaker.kork.secrets.SecretManager;
import io.armory.plugin.eap.EAPConfigurationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class URLCredentialsLoaderTest {

    private SecretManager secretManager;

    @BeforeEach
    public void setUp() {
        secretManager = mock(SecretManager.class);
        when(secretManager.decrypt(anyString())).thenAnswer(
                (Answer<String>) invocation -> invocation.getArgument(0, String.class));
    }

    @Test
    public void testLoadYamlProviderFile() {
        URLCredentialsLoader<KubernetesAccountProperties.ManagedAccount> loader = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.YAML,
                KubernetesAccountProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/provider.yml");
            }
        };

        List<KubernetesAccountProperties.ManagedAccount> actual = loader.getCredentialsDefinitions();
        assertEquals(2, actual.size());
    }

    @Test
    public void testLoadJsonProviderFile() {
        URLCredentialsLoader<KubernetesAccountProperties.ManagedAccount> loader = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.JSON,
                KubernetesAccountProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/provider.json");
            }
        };

        List<KubernetesAccountProperties.ManagedAccount> actual = loader.getCredentialsDefinitions();
        assertEquals(2, actual.size());
    }

    @Test
    public void testLoadYamlListFile() {
        URLCredentialsLoader<KubernetesAccountProperties.ManagedAccount> loader = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.YAML,
                KubernetesAccountProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/list.yml");
            }
        };

        List<KubernetesAccountProperties.ManagedAccount> actual = loader.getCredentialsDefinitions();
        assertEquals(2, actual.size());
    }

    @Test
    public void testLoadJsonListFile() {
        URLCredentialsLoader<KubernetesAccountProperties.ManagedAccount> loader = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.JSON,
                KubernetesAccountProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/list.json");
            }
        };

        List<KubernetesAccountProperties.ManagedAccount> actual = loader.getCredentialsDefinitions();
        assertEquals(2, actual.size());
    }

    @Test
    public void testLoadYamSingleFile() {
        URLCredentialsLoader<KubernetesAccountProperties.ManagedAccount> loader = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.YAML,
                KubernetesAccountProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/single.yml");
            }
        };

        List<KubernetesAccountProperties.ManagedAccount> actual = loader.getCredentialsDefinitions();
        assertEquals(1, actual.size());
    }

    @Test
    public void testLoadJsonSingleFile() {
        URLCredentialsLoader<KubernetesAccountProperties.ManagedAccount> loader = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.JSON,
                KubernetesAccountProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/single.json");
            }
        };

        List<KubernetesAccountProperties.ManagedAccount> actual = loader.getCredentialsDefinitions();
        assertEquals(1, actual.size());
    }

    @Test
    public void testReplaceEnvVars() {
        URLCredentialsLoader<KubernetesAccountProperties.ManagedAccount> loader = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.YAML,
                KubernetesAccountProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/single.yml");
            }
        };

        List<KubernetesAccountProperties.ManagedAccount> actual = loader.getCredentialsDefinitions();
        assertEquals(System.getenv("HOME"), actual.get(0).getName());
    }

    @Test
    public void testReplaceNotDefinedEnvVars() {
        URLCredentialsLoader<KubernetesAccountProperties.ManagedAccount> loader = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.YAML,
                KubernetesAccountProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/single-not-env.yml");
            }
        };

        List<KubernetesAccountProperties.ManagedAccount> actual = loader.getCredentialsDefinitions();
        assertEquals("${UNKNOWN}", actual.get(0).getName());
    }

    @Test
    public void testMixedProviderAccounts() {
        URLCredentialsLoader<KubernetesAccountProperties.ManagedAccount> cdl = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.YAML,
                KubernetesAccountProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/clouddriver-mixed.yml");
            }
        };
        List<KubernetesAccountProperties.ManagedAccount> cda = cdl.getCredentialsDefinitions();
        assertTrue(cda.size() == 1 && "kube".equals(cda.get(0).getName()));

        URLCredentialsLoader<CloudFoundryConfigurationProperties.ManagedAccount> cfl = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.YAML,
                CloudFoundryConfigurationProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/clouddriver-mixed.yml");
            }
        };
        List<CloudFoundryConfigurationProperties.ManagedAccount> cfa = cfl.getCredentialsDefinitions();
        assertTrue(cfa.size() == 1 && "cf".equals(cfa.get(0).getName()));

        URLCredentialsLoader<DockerRegistryConfigurationProperties.ManagedAccount> dockerCredentialsLoader = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.YAML,
                DockerRegistryConfigurationProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/clouddriver-mixed.yml");
            }
        };
        List<DockerRegistryConfigurationProperties.ManagedAccount> dockerCredentialsList = dockerCredentialsLoader.getCredentialsDefinitions();

        assertEquals(1, dockerCredentialsList.size());
        assertEquals("dockerhub", dockerCredentialsList.get(0).getName());
        assertEquals("https://index.docker.io", dockerCredentialsList.get(0).getAddress());
        assertEquals("fake.email@spinnaker.io", dockerCredentialsList.get(0).getEmail());
        assertEquals(30, dockerCredentialsList.get(0).getCacheIntervalSeconds());
        assertEquals(60000, dockerCredentialsList.get(0).getClientTimeoutMillis());
        assertEquals(1, dockerCredentialsList.get(0).getCacheThreads());
        assertEquals(100, dockerCredentialsList.get(0).getPaginateSize());
        assertFalse(dockerCredentialsList.get(0).getSortTagsByDate());
        assertFalse(dockerCredentialsList.get(0).getTrackDigests());
        assertFalse(dockerCredentialsList.get(0).isInsecureRegistry());
        assertEquals("library/nginx", dockerCredentialsList.get(0).getRepositories().get(0));
    }

    @Test
    public void testCloudFoundryAccounts() {
        URLCredentialsLoader<CloudFoundryConfigurationProperties.ManagedAccount> cfl = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.YAML,
                CloudFoundryConfigurationProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/cf-multiple.yml");
            }
        };
        List<CloudFoundryConfigurationProperties.ManagedAccount> cas = cfl.getCredentialsDefinitions();
        assertTrue(cas.size() == 2 &&
                cas.stream().anyMatch(ca -> "cf1".equals(ca.getName())) &&
                cas.stream().anyMatch(ca -> "cf2".equals(ca.getName())));
    }

    @Test
    public void testLoadDockerRegistrySingleYmlFile() {
        URLCredentialsLoader<DockerRegistryConfigurationProperties.ManagedAccount> loader = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.YAML,
                DockerRegistryConfigurationProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/docker/docker-registry.yml");
            }
        };

        List<DockerRegistryConfigurationProperties.ManagedAccount> actual = loader.getCredentialsDefinitions();

        assertEquals(1, actual.size());
        assertEquals("dockerhub", actual.get(0).getName());
        assertEquals("https://index.docker.io", actual.get(0).getAddress());
        assertEquals("fake.email@spinnaker.io", actual.get(0).getEmail());
        assertEquals(30, actual.get(0).getCacheIntervalSeconds());
        assertEquals(60000, actual.get(0).getClientTimeoutMillis());
        assertEquals(1, actual.get(0).getCacheThreads());
        assertEquals(100, actual.get(0).getPaginateSize());
        assertFalse(actual.get(0).getSortTagsByDate());
        assertFalse(actual.get(0).getTrackDigests());
        assertFalse(actual.get(0).isInsecureRegistry());
        assertEquals("library/nginx", actual.get(0).getRepositories().get(0));
    }

    @Test
    public void testLoadDockerRegistrySingleJsonFile() {
        URLCredentialsLoader<DockerRegistryConfigurationProperties.ManagedAccount> loader = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.JSON,
                DockerRegistryConfigurationProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/docker/docker-registry.json");
            }
        };

        List<DockerRegistryConfigurationProperties.ManagedAccount> actual = loader.getCredentialsDefinitions();

        assertEquals(1, actual.size());
        assertEquals("dockerhub", actual.get(0).getName());
        assertEquals("https://index.docker.io", actual.get(0).getAddress());
        assertEquals("fake.email@spinnaker.io", actual.get(0).getEmail());
        assertEquals(30, actual.get(0).getCacheIntervalSeconds());
        assertEquals(60000, actual.get(0).getClientTimeoutMillis());
        assertEquals(1, actual.get(0).getCacheThreads());
        assertEquals(100, actual.get(0).getPaginateSize());
        assertFalse(actual.get(0).getSortTagsByDate());
        assertFalse(actual.get(0).getTrackDigests());
        assertFalse(actual.get(0).isInsecureRegistry());
        assertEquals("library/nginx", actual.get(0).getRepositories().get(0));
    }

    @Test
    public void testDockerRegistryMultipleAccounts() {
        URLCredentialsLoader<DockerRegistryConfigurationProperties.ManagedAccount> loader = new URLCredentialsLoader<>(
                null,
                EAPConfigurationProperties.FileFormat.YAML,
                DockerRegistryConfigurationProperties.ManagedAccount.class,
                secretManager) {
            @Override
            protected InputStream getInputStream() {
                return URLCredentialsLoaderTest.class.getResourceAsStream("/docker/docker-registry-multiple.yml");
            }
        };

        List<DockerRegistryConfigurationProperties.ManagedAccount> dockerAccounts = loader.getCredentialsDefinitions();

        assertEquals(2, dockerAccounts.size());
        assertTrue(
                dockerAccounts.stream().anyMatch(da -> "dockerhub".equals(da.getName())) &&
                        dockerAccounts.stream().anyMatch(da -> "ecr".equals(da.getName())));

        // First account
        assertEquals("https://index.docker.io", dockerAccounts.get(0).getAddress());
        assertEquals("fake.email@spinnaker.io", dockerAccounts.get(0).getEmail());
        assertEquals(30, dockerAccounts.get(0).getCacheIntervalSeconds());
        assertEquals(60000, dockerAccounts.get(0).getClientTimeoutMillis());
        assertEquals(1, dockerAccounts.get(0).getCacheThreads());
        assertEquals(100, dockerAccounts.get(0).getPaginateSize());
        assertFalse(dockerAccounts.get(0).getSortTagsByDate());
        assertFalse(dockerAccounts.get(0).getTrackDigests());
        assertFalse(dockerAccounts.get(0).isInsecureRegistry());
        assertEquals("library/nginx", dockerAccounts.get(0).getRepositories().get(0));

        // Second account
        assertEquals("https://index.docker.io", dockerAccounts.get(1).getAddress());
        assertEquals("fake.email.2@spinnaker.io", dockerAccounts.get(1).getEmail());
        assertEquals(20, dockerAccounts.get(1).getCacheIntervalSeconds());
        assertEquals(75000, dockerAccounts.get(1).getClientTimeoutMillis());
        assertEquals(1, dockerAccounts.get(1).getCacheThreads());
        assertEquals(50, dockerAccounts.get(1).getPaginateSize());
        assertFalse(dockerAccounts.get(1).getSortTagsByDate());
        assertFalse(dockerAccounts.get(1).getTrackDigests());
        assertFalse(dockerAccounts.get(1).isInsecureRegistry());
        assertEquals("library/nginx", dockerAccounts.get(1).getRepositories().get(0));
    }

}
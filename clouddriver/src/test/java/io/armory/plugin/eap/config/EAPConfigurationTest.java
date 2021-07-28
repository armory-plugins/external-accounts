package io.armory.plugin.eap.config;


import com.netflix.spinnaker.clouddriver.kubernetes.config.KubernetesConfigurationProperties;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionSource;
import com.netflix.spinnaker.kork.secrets.SecretManager;
import io.armory.plugin.eap.EAPConfigurationProperties;
import io.armory.plugin.eap.EAPException;
import io.armory.plugin.eap.loaders.DirectoryCredentialsLoader;
import io.armory.plugin.eap.loaders.URLCredentialsLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.*;

class EAPConfigurationTest {

    private SecretManager secretManager;

    @BeforeEach
    void setUp() {
        secretManager = mock(SecretManager.class);
    }

    @Test
    public void shouldCreateURLLoader() throws MalformedURLException {
        EAPConfigurationProperties config = new EAPConfigurationProperties();
        config.setUrl(new URL("https://myserver.com"));
        config.setUrlContentFormat(EAPConfigurationProperties.FileFormat.JSON);
        config.init();

        CredentialsDefinitionSource<KubernetesConfigurationProperties.ManagedAccount> source =
                new EAPConfiguration().kubernetesCredentialSource(config, secretManager);

        assertTrue(source instanceof URLCredentialsLoader, "Expected URLCredentialsLoader to be created for a given URL config");
    }

    @Test
    public void shouldCreateDirLoader() {
        EAPConfigurationProperties config = new EAPConfigurationProperties();
        config.setDir("/tmp");
        config.init();

        CredentialsDefinitionSource<KubernetesConfigurationProperties.ManagedAccount> source =
                new EAPConfiguration().kubernetesCredentialSource(config, secretManager);

        assertTrue(source instanceof DirectoryCredentialsLoader, "Expected DirectoryCredentialsLoader to be created for a given directory path");
    }

    @Test
    public void shouldThrowExceptionOnDirAndUrlConfig() throws MalformedURLException {
        EAPConfigurationProperties config = new EAPConfigurationProperties();
        config.setDir("/tmp");
        config.setUrl(new URL("https://myserver.com"));

        assertThrows(EAPException.class,
                config::init,
                "Providing both directory and url config should throw an exception");
    }

}
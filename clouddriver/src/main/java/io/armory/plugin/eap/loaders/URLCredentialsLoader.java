package io.armory.plugin.eap.loaders;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableMap;
import com.netflix.spinnaker.clouddriver.aws.security.config.CredentialsConfig;
import com.netflix.spinnaker.clouddriver.cloudfoundry.config.CloudFoundryConfigurationProperties;
import com.netflix.spinnaker.clouddriver.ecs.security.ECSCredentialsConfig;
import com.netflix.spinnaker.clouddriver.kubernetes.config.KubernetesConfigurationProperties;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionSource;
import com.netflix.spinnaker.kork.secrets.SecretManager;
import io.armory.plugin.eap.EAPException;
import io.armory.plugin.eap.config.EAPConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads account credentials from a URL, which can reference a remote or local file.
 */
@Slf4j
public class URLCredentialsLoader<T extends CredentialsDefinition> implements CredentialsDefinitionSource<T> {

    private static final Map<Class<? extends CredentialsDefinition>, String> PROVIDER_NAME_BY_CLASS = ImmutableMap.of(
            KubernetesConfigurationProperties.ManagedAccount.class, "kubernetes",
            CloudFoundryConfigurationProperties.ManagedAccount.class, "cloudfoundry",
            CredentialsConfig.Account.class, "aws",
            ECSCredentialsConfig.Account.class, "ecs"
    );
    private static final String ACCOUNTS_KEY = "accounts";
    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("^.*\\$\\{(.*)}.*$");

    private final URL url;
    private final EAPConfigurationProperties.FileFormat format;
    private final Class<T> classType;
    private final String providerName;
    private final JavaType listJavaType;
    private ObjectMapper mapper;

    public URLCredentialsLoader(URL url, EAPConfigurationProperties.FileFormat format,
                                Class<T> classType, SecretManager secretManager) {
        this.url = url;
        this.format = format;
        this.classType = classType;
        this.providerName = PROVIDER_NAME_BY_CLASS.get(classType);
        if (this.providerName == null) {
            throw new EAPException("Unknown provider name for class " + classType);
        }
        initMapper(secretManager);
        listJavaType = mapper.getTypeFactory().constructCollectionType(List.class, classType);
    }

    private void initMapper(SecretManager secretManager) {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new StringDeserializer() {
            @Override
            public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String text = p.getText();
                String value = secretManager.decrypt(text);
                Matcher matcher = ENV_VAR_PATTERN.matcher(value);
                if (!matcher.matches()) {
                    return value;
                }
                log.debug("Property value {} will be replaced with env var {}", value, matcher.group(1));
                value = value.replaceAll("\\$\\{.*}", System.getenv(matcher.group(1)));
                return secretManager.decrypt(value);
            }
        });
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .registerModule(module);
    }

    protected InputStream getInputStream() throws IOException {
        return url.openStream();
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public List<T> getCredentialsDefinitions() {
        try (InputStream is = getInputStream()) {
            Reader reader = new BufferedReader(new InputStreamReader(is, Charset.defaultCharset()));
            Map<String, Object> configMap;
            JavaType javaType = mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
            switch (format) {
                case JSON:
                    JsonNode jsonNode = mapper.readTree(reader);
                    if (jsonNode.isArray()) {
                        return (List<T>) Optional.ofNullable(mapper.convertValue(jsonNode, listJavaType)).orElse(new ArrayList<>());
                    }
                    configMap = mapper.convertValue(jsonNode, javaType);
                    return convertMap(configMap);
                case YAML:
                    Yaml yaml = new Yaml(new SafeConstructor());
                    Object root = yaml.load(reader);
                    if (root instanceof Collection) {
                        return (List<T>) Optional.ofNullable(mapper.convertValue(root, listJavaType)).orElse(new ArrayList<>());
                    }
                    configMap = mapper.convertValue(root, javaType);
                    return convertMap(configMap);
                default:
                    throw new EAPException("Unknown format: " + format);
            }
        } catch (IOException e) {
            throw new EAPException("Unable to load configuration from " + url, e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<T> convertMap(Map<String, Object> map) {
        if (!map.containsKey(providerName)) {
            if (map.containsKey("name")) { // single account defined in a file
                T account = mapper.convertValue(map, classType);
                return account != null ? List.of(account) : new ArrayList<>();
            } else { // no account for desired provider
                return new ArrayList<>();
            }
        }
        Map<String, Object> providerMap = (Map<String, Object>) map.get(providerName);
        Object accountsList = providerMap.get(ACCOUNTS_KEY);
        if (accountsList == null) {
            throw new EAPException("Provider " + providerName + " doesn't have \"" + ACCOUNTS_KEY + "\" entry");
        }
        return (List<T>) Optional.ofNullable(mapper.convertValue(accountsList, listJavaType)).orElse(new ArrayList<>());
    }
}

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

import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionSource;
import com.netflix.spinnaker.kork.secrets.SecretManager;
import io.armory.plugin.eap.EAPConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads account credentials from a directory recursively.
 */
@Slf4j
public class DirectoryCredentialsLoader<T extends CredentialsDefinition> implements CredentialsDefinitionSource<T> {

    private final Path dir;
    private final Set<String> filePrefixes;
    private final Class<T> classType;
    private final SecretManager secretManager;

    public DirectoryCredentialsLoader(Path dir, Class<T> classType, SecretManager secretManager, String... filePrefixes) {
        this.dir = dir;
        this.classType = classType;
        this.secretManager = secretManager;
        this.filePrefixes = Arrays.stream(filePrefixes).collect(Collectors.toSet());
    }

    @NotNull
    @Override
    public List<T> getCredentialsDefinitions() {
        List<T> result = new ArrayList<>();
        if (!dir.toFile().exists()) {
            log.warn("Unable to open directory {} because it doesn't exist. " +
                    dir.toFile().getAbsolutePath());
            return result;
        }
        FileUtils.listFiles(dir.toFile(), new String[]{"yml", "yaml", "json"}, true)
                .stream()
                .filter(f -> filePrefixes.stream().anyMatch(p -> f.getName().startsWith(p)))
                .forEach(f -> addCredentials(f, result));
        log.debug("Loaded {} credentials of type {}", result.size(), classType.getCanonicalName());
        return result;
    }

    private void addCredentials(File file, List<T> credentials) {
        try {
            EAPConfigurationProperties.FileFormat format;
            if (file.getName().endsWith("json")) {
                format = EAPConfigurationProperties.FileFormat.JSON;
            } else {
                format = EAPConfigurationProperties.FileFormat.YAML;
            }
            URLCredentialsLoader<T> loader = new URLCredentialsLoader<>(
                    file.toURI().toURL(), format, classType, secretManager);
            credentials.addAll(loader.getCredentialsDefinitions());
        } catch (MalformedURLException e) {
            log.error("Error loading credentials from file {}", file.getAbsolutePath(), e);
        }
    }
}

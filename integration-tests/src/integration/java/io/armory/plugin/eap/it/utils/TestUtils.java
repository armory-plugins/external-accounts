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

package io.armory.plugin.eap.it.utils;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.fail;

public abstract class TestUtils {

    private static final int SLEEP_STEP_SECONDS = 5;

    public static TestResourceFile loadYaml(String file) {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        try {
            InputStream is = resourceLoader.getResource(file).getInputStream();
            Yaml yaml = new Yaml(new SafeConstructor());
            Map<String, Object> map = yaml.load(is);
            return new TestResourceFile(map);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load yaml file " + file, e);
        }
    }

    public static TestResourceFile loadJson(String file) {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        try {
            InputStream is = resourceLoader.getResource(file).getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            JavaType type = mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
            Map<String, Object> map = mapper.readValue(is, type);
            return new TestResourceFile(map);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load yaml file " + file, e);
        }
    }

    public static void repeatUntilTrue(
            BooleanSupplier func, long duration, TimeUnit unit, String errorMsg)
            throws InterruptedException {
        long durationSeconds = unit.toSeconds(duration);
        for (int i = 0; i < (durationSeconds / SLEEP_STEP_SECONDS); i++) {
            if (!func.getAsBoolean()) {
                Thread.sleep(TimeUnit.SECONDS.toMillis(SLEEP_STEP_SECONDS));
            } else {
                return;
            }
        }
        fail(errorMsg);
    }

    public static class TestResourceFile {

        private Map<String, Object> content;

        public TestResourceFile(Map<String, Object> content) {
            this.content = content;
        }

        public Map<String, Object> asMap() {
            return content;
        }

        @SuppressWarnings("unchecked")
        public TestResourceFile withValue(String path, Object value) {
            List<String> parts = Splitter.on('.').splitToList(path);

            Map<String, Object> entry = content;
            for (int i = 0; i < parts.size(); i++) {
                if (parts.get(i).matches("^.*\\[[0-9]*]$")) {
                    String key = parts.get(i).substring(0, parts.get(i).indexOf('['));
                    int index =
                            Integer.parseInt(parts.get(i)
                                    .substring(parts.get(i).indexOf('[') + 1, parts.get(i).indexOf(']')));
                    List<Map<String, Object>> list = (List<Map<String, Object>>) entry.get(key);
                    if (i == parts.size() - 1) {
                        list.add(index, (Map<String, Object>) value);
                        break;
                    }
                    entry = list.get(index);
                } else if (i == parts.size() - 1) {
                    entry.put(parts.get(i), value);
                    break;
                } else if (!entry.containsKey(parts.get(i))) {
                    entry.put(parts.get(i), new HashMap<>());
                    entry = (Map<String, Object>) entry.get(parts.get(i));
                } else {
                    entry = (Map<String, Object>) entry.get(parts.get(i));
                }
            }

            return this;
        }
    }
}

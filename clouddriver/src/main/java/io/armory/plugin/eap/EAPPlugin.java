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

package io.armory.plugin.eap;

import com.netflix.spinnaker.kork.plugins.api.spring.SpringLoaderPlugin;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import java.util.Collections;
import java.util.List;

public class EAPPlugin extends SpringLoaderPlugin {
    /**
     * Constructor to be used by plugin manager for plugin instantiation. Your plugins have to provide
     * constructor with this exact signature to be successfully loaded by manager.
     *
     * @param wrapper
     */
    public EAPPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public List<String> getPackagesToScan() {
        return Collections.singletonList("io.armory.plugin.eap");
    }

    @Override
    public void start() {
        log.info("Starting EAP plugin...");
    }

    @Override
    public void stop() {
        log.info("Stopping EAP plugin...");
    }

    @Override
    public void registerBeanDefinitions(BeanDefinitionRegistry registry) {
        super.registerBeanDefinitions(registry);
        registry.getBeanDefinition("cloudDriverConfig").
                setDependsOn("Armory.EAP.com.netflix.spinnaker.kork.plugins.api.spring.SpringLoader");
    }
}

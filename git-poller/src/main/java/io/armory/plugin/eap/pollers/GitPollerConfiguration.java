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

package io.armory.plugin.eap.pollers;

import io.armory.plugin.eap.EAPConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GitPollerConfigurationProperties.class)
public class GitPollerConfiguration {

    @ConditionalOnProperty("armory.external-accounts.git-poller.enabled")
    @Bean
    public GitPoller gitPoller(GitPollerConfigurationProperties gitConfig, EAPConfigurationProperties globalConfig) {
        return new GitPoller(gitConfig, globalConfig.getDir().toAbsolutePath());
    }
}

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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.armory.plugin.eap.EAPException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GitPoller implements Runnable {

    private final GitPollerConfigurationProperties configProperties;
    private final Path targetDir;
    private final GitStrategy strategy;
    private boolean initialized = false;

    public interface GitStrategy {
        void cloneRepo() throws EAPException;
        void pullChanges() throws EAPException;
    }

    public enum AuthType {
        HTTP,
        TOKEN,
        SSH,
        NONE
    }

    public GitPoller(GitPollerConfigurationProperties configProperties, Path targetDir) {
        this.configProperties = configProperties;
        this.targetDir = targetDir;
        AuthType authType;
        if (!StringUtils.isEmpty(this.configProperties.getUsername()) &&
                !StringUtils.isEmpty(this.configProperties.getPassword())) {
            authType = AuthType.HTTP;
        } else if (!StringUtils.isEmpty(this.configProperties.getToken())) {
            authType = AuthType.TOKEN;
        } else if (!StringUtils.isEmpty(this.configProperties.getSshPrivateKeyFilePath())) {
            authType = AuthType.SSH;
        } else {
            authType = AuthType.NONE;
        }

        ShellGitStrategy.ShellResult shellResult = ShellGitStrategy.execShellCommand(
                "git --version", new File(System.getProperty("java.io.tmpdir")));
        if (shellResult.getExitValue() == 0) {
            log.info("Git binary detected on path, using it");
            this.strategy = new ShellGitStrategy(configProperties, targetDir, authType);
        } else {
            log.info("Git binary NOT detected on path, using jgit");
            this.strategy = new JgitStrategy(configProperties, targetDir, authType);
        }

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat(GitPoller.class.getSimpleName() + "-%d")
                        .build());
        executor.scheduleAtFixedRate(this, 0,
                configProperties.getSyncIntervalSecs(), TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        try {
            if (!initialized) {
                if (targetDir.toFile().exists()) {
                    FileUtils.deleteDirectory(targetDir.toFile());
                }
                FileUtils.forceMkdir(targetDir.toFile());
                log.info("Cloning git repository {} into {}",
                        this.configProperties.getRepo(),
                        this.targetDir.toAbsolutePath());
                strategy.cloneRepo();
                initialized = true;
            }
            log.debug("Pulling latest changes from repo {}", configProperties.getRepo());
            strategy.pullChanges();
        } catch (Throwable t) {
            log.error("Exception cloning or refreshing git repo " + configProperties.getRepo(), t);
        }
    }
}

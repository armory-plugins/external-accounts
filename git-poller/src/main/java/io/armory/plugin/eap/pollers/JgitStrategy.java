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

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.armory.plugin.eap.EAPException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public class JgitStrategy implements GitPoller.GitStrategy {

    private final GitPollerConfigurationProperties configProperties;
    private final Path targetDir;
    private final GitPoller.AuthType authType;

    public JgitStrategy(GitPollerConfigurationProperties configProperties, Path targetDir, GitPoller.AuthType authType) {
        this.configProperties = configProperties;
        this.targetDir = targetDir;
        this.authType = authType;
    }

    @Override
    public void cloneRepo() throws EAPException {
        try (@SuppressWarnings("unused") Git localRepository = addAuthentication(Git.cloneRepository())
                .setURI(configProperties.getRepo())
                .setDirectory(targetDir.toFile())
                .setBranch(configProperties.getBranch())
                .call()) {
            log.info("GitConfig repository cloned");
        } catch (GitAPIException e) {
            throw new EAPException(
                    "Failed to clone git repository " + configProperties.getRepo() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void pullChanges() throws EAPException {
        try (Git localRepository = Git.open(targetDir.toFile())) {
            addAuthentication(localRepository.pull()).call();
        } catch (IOException | GitAPIException e) {
            throw new EAPException(
                    "Failed to do \"git pull\" of repository " + configProperties.getRepo() + ": " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <C extends GitCommand, T> C addAuthentication(TransportCommand<C, T> command) {
        switch (authType) {
            case HTTP:
                return command.setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(this.configProperties.getUsername(),
                                this.configProperties.getPassword()));
            case TOKEN:
                return command.setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(this.configProperties.getToken(), ""));
            case SSH:
                return configureSshAuth(command);
            default:
                return (C) command;
        }
    }

    private <C extends GitCommand, T> C configureSshAuth(TransportCommand<C, T> command) {
        return command.setTransportConfigCallback(
                (Transport transport) -> {
                    if (!(transport instanceof SshTransport)) {
                        return;
                    }
                    SshTransport sshTransport = (SshTransport) transport;

                    sshTransport.setSshSessionFactory(new JschConfigSessionFactory() {
                        @Override
                        protected void configure(OpenSshConfig.Host hc, Session session) {
                            if (StringUtils.isEmpty(configProperties.getSshKnownHostsFilePath()) &&
                                    configProperties.isSshTrustUnknownHosts()) {
                                session.setConfig("StrictHostKeyChecking", "no");
                            }
                        }

                        @Override
                        protected JSch createDefaultJSch(FS fs) throws JSchException {
                            JSch defaultJSch = super.createDefaultJSch(fs);
                            if (!StringUtils.isEmpty(configProperties.getSshPrivateKeyPassphrase())) {
                                defaultJSch.addIdentity(configProperties.getSshPrivateKeyFilePath(),
                                        configProperties.getSshPrivateKeyPassphrase());
                            } else {
                                defaultJSch.addIdentity(configProperties.getSshPrivateKeyFilePath());
                            }

                            if (!StringUtils.isEmpty(configProperties.getSshKnownHostsFilePath()) &&
                                    configProperties.isSshTrustUnknownHosts()) {
                                log.warn("SSH known_hosts file path supplied, ignoring 'sshTrustUnknownHosts' option");
                            }

                            if (!StringUtils.isEmpty(configProperties.getSshKnownHostsFilePath())) {
                                defaultJSch.setKnownHosts(configProperties.getSshKnownHostsFilePath());
                            }

                            return defaultJSch;
                        }
                    });
                });
    }

}

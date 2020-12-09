package io.armory.plugin.eap.pollers;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.armory.plugin.eap.EAPConfigurationProperties;
import io.armory.plugin.eap.EAPException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class JgitPoller implements Runnable {

    private final EAPConfigurationProperties configProperties;
    private final AuthType authType;
    private boolean initialized = false;

    private enum AuthType {
        HTTP,
        TOKEN,
        SSH,
        NONE
    }

    public JgitPoller(EAPConfigurationProperties configProperties) {
        this.configProperties = configProperties;
        if (!StringUtils.isEmpty(this.configProperties.getJGitPoller().getUsername()) &&
                !StringUtils.isEmpty(this.configProperties.getJGitPoller().getPassword())) {
            authType = AuthType.HTTP;
        } else if (!StringUtils.isEmpty(this.configProperties.getJGitPoller().getToken())) {
            authType = AuthType.TOKEN;
        } else if (!StringUtils.isEmpty(this.configProperties.getJGitPoller().getSshPrivateKeyFilePath())) {
            authType = AuthType.SSH;
        } else {
            authType = AuthType.NONE;
        }
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat(JgitPoller.class.getSimpleName() + "-%d")
                        .build());
        executor.scheduleAtFixedRate(this, 0,
                configProperties.getJGitPoller().getSyncIntervalSecs(), TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        try {
            if (!initialized) {
                if (configProperties.getDir().toFile().exists()) {
                    FileUtils.deleteDirectory(configProperties.getDir().toFile());
                    FileUtils.forceMkdir(configProperties.getDir().toFile());
                }
                cloneRepo();
                initialized = true;
            }
            pullChanges();
        } catch (Throwable t) {
            log.error("Exception cloning or refreshing git repo " + configProperties.getJGitPoller().getRepo(), t);
        }
    }

    private void cloneRepo() {
        log.info("Cloning git repository {} into {}",
                this.configProperties.getJGitPoller().getRepo(),
                this.configProperties.getDir().toAbsolutePath());

        try (@SuppressWarnings("unused") Git localRepository = addAuthentication(Git.cloneRepository())
                .setURI(configProperties.getJGitPoller().getRepo())
                .setDirectory(configProperties.getDir().toFile())
                .setBranch(configProperties.getJGitPoller().getBranch())
                .call()) {
            log.info("GitConfig repository cloned");
        } catch (GitAPIException e) {
            throw new EAPException(
                    "Failed to clone git repository " + configProperties.getJGitPoller().getRepo() + ": " + e.getMessage(), e);
        }
    }

    private void pullChanges() {
        log.debug("Pulling latest changes from repo {}", configProperties.getJGitPoller().getRepo());
        try (Git localRepository = Git.open(configProperties.getDir().toFile())) {
            addAuthentication(localRepository.pull()).call();
        } catch (IOException | GitAPIException e) {
            throw new EAPException(
                    "Failed to do \"git pull\" of repository " + configProperties.getJGitPoller().getRepo() + ": " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <C extends GitCommand, T> C addAuthentication(TransportCommand<C, T> command) {
        switch (authType) {
            case HTTP:
                return command.setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(this.configProperties.getJGitPoller().getUsername(),
                                this.configProperties.getJGitPoller().getPassword()));
            case TOKEN:
                return command.setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(this.configProperties.getJGitPoller().getToken(), ""));
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
                            if (StringUtils.isEmpty(configProperties.getJGitPoller().getSshKnownHostsFilePath()) &&
                                    configProperties.getJGitPoller().isSshTrustUnknownHosts()) {
                                session.setConfig("StrictHostKeyChecking", "no");
                            }
                        }

                        @Override
                        protected JSch createDefaultJSch(FS fs) throws JSchException {
                            JSch defaultJSch = super.createDefaultJSch(fs);
                            if (!StringUtils.isEmpty(configProperties.getJGitPoller().getSshPrivateKeyPassphrase())) {
                                defaultJSch.addIdentity(configProperties.getJGitPoller().getSshPrivateKeyFilePath(),
                                        configProperties.getJGitPoller().getSshPrivateKeyPassphrase());
                            } else {
                                defaultJSch.addIdentity(configProperties.getJGitPoller().getSshPrivateKeyFilePath());
                            }

                            if (!StringUtils.isEmpty(configProperties.getJGitPoller().getSshKnownHostsFilePath()) &&
                                    configProperties.getJGitPoller().isSshTrustUnknownHosts()) {
                                log.warn("SSH known_hosts file path supplied, ignoring 'sshTrustUnknownHosts' option");
                            }

                            if (!StringUtils.isEmpty(configProperties.getJGitPoller().getSshKnownHostsFilePath())) {
                                defaultJSch.setKnownHosts(configProperties.getJGitPoller().getSshKnownHostsFilePath());
                            }

                            return defaultJSch;
                        }
                    });
                });
    }

}

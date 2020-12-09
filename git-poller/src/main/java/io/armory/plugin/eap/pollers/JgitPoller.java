package io.armory.plugin.eap.pollers;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.armory.plugin.eap.EAPConfigurationProperties;
import io.armory.plugin.eap.EAPException;
import lombok.extern.slf4j.Slf4j;
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

    private enum AuthType {
        HTTP,
        TOKEN,
        SSH,
        NONE
    }

    public JgitPoller(EAPConfigurationProperties configProperties) {
        this.configProperties = configProperties;
        if (!StringUtils.isEmpty(this.configProperties.getGit().getUsername()) &&
                !StringUtils.isEmpty(this.configProperties.getGit().getPassword())) {
            authType = AuthType.HTTP;
        } else if (!StringUtils.isEmpty(this.configProperties.getGit().getToken())) {
            authType = AuthType.TOKEN;
        } else if (!StringUtils.isEmpty(this.configProperties.getGit().getSshPrivateKeyFilePath())) {
            authType = AuthType.SSH;
        } else {
            authType = AuthType.NONE;
        }
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat(JgitPoller.class.getSimpleName() + "-%d")
                        .build());
        executor.scheduleAtFixedRate(this, 0,
                configProperties.getGit().getSyncIntervalSecs(), TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        try {
            File[] targetFiles = configProperties.getGit().getLocalClonePath().toFile().listFiles();
            if (targetFiles == null || targetFiles.length == 0) {
                cloneRepo();
            } else {
                pullChanges();
            }
        } catch (Throwable t) {
            log.error("Exception cloning or refreshing git repo " + configProperties.getGit().getRepo(), t);
        }
    }

    private void cloneRepo() {
        log.info("Cloning git repository {} into {}",
                this.configProperties.getGit().getRepo(),
                this.configProperties.getGit().getLocalClonePath().toAbsolutePath());

        try (@SuppressWarnings("unused") Git localRepository = addAuthentication(Git.cloneRepository())
                .setURI(configProperties.getGit().getRepo())
                .setDirectory(configProperties.getGit().getLocalClonePath().toFile())
                .setBranch(configProperties.getGit().getBranch())
                .call()) {
            log.info("GitConfig repository cloned");
        } catch (GitAPIException e) {
            throw new EAPException(
                    "Failed to clone git repository " + configProperties.getGit().getRepo() + ": " + e.getMessage(), e);
        }
    }

    private void pullChanges() {
        log.debug("Pulling latest changes from repo {}", configProperties.getGit().getRepo());
        try (Git localRepository = Git.open(configProperties.getGit().getLocalClonePath().toFile())) {
            addAuthentication(localRepository.pull()).call();
        } catch (IOException | GitAPIException e) {
            throw new EAPException(
                    "Failed to do \"git pull\" of repository " + configProperties.getGit().getRepo() + ": " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <C extends GitCommand, T> C addAuthentication(TransportCommand<C, T> command) {
        switch (authType) {
            case HTTP:
                return command.setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(this.configProperties.getGit().getUsername(),
                                this.configProperties.getGit().getPassword()));
            case TOKEN:
                return command.setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(this.configProperties.getGit().getToken(), ""));
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
                            if (StringUtils.isEmpty(configProperties.getGit().getSshKnownHostsFilePath()) &&
                                    configProperties.getGit().isSshTrustUnknownHosts()) {
                                session.setConfig("StrictHostKeyChecking", "no");
                            }
                        }

                        @Override
                        protected JSch createDefaultJSch(FS fs) throws JSchException {
                            JSch defaultJSch = super.createDefaultJSch(fs);
                            if (!StringUtils.isEmpty(configProperties.getGit().getSshPrivateKeyPassphrase())) {
                                defaultJSch.addIdentity(configProperties.getGit().getSshPrivateKeyFilePath(),
                                        configProperties.getGit().getSshPrivateKeyPassphrase());
                            } else {
                                defaultJSch.addIdentity(configProperties.getGit().getSshPrivateKeyFilePath());
                            }

                            if (!StringUtils.isEmpty(configProperties.getGit().getSshKnownHostsFilePath()) &&
                                    configProperties.getGit().isSshTrustUnknownHosts()) {
                                log.warn("SSH known_hosts file path supplied, ignoring 'sshTrustUnknownHosts' option");
                            }

                            if (!StringUtils.isEmpty(configProperties.getGit().getSshKnownHostsFilePath())) {
                                defaultJSch.setKnownHosts(configProperties.getGit().getSshKnownHostsFilePath());
                            }

                            return defaultJSch;
                        }
                    });
                });
    }

}

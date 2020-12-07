package io.armory.plugin.eap.syncs;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.armory.plugin.eap.EAPException;
import io.armory.plugin.eap.config.EAPConfigurationProperties;
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
public class GitSync implements Runnable {

    private final EAPConfigurationProperties configProperties;
    private final AuthType authType;

    private enum AuthType {
        HTTP,
        TOKEN,
        SSH,
        NONE
    }

    public GitSync(EAPConfigurationProperties configProperties) {
        this.configProperties = configProperties;
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
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat(GitSync.class.getSimpleName() + "-%d")
                        .build());
        executor.scheduleAtFixedRate(this, 0,
                configProperties.getRefreshIntervalSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        try {
            File[] targetFiles = configProperties.getClonePath().toFile().listFiles();
            if (targetFiles == null || targetFiles.length == 0) {
                cloneRepo();
            } else {
                pullChanges();
            }
        } catch (Throwable t) {
            log.error("Exception cloning or refreshing git repo " + configProperties.getRepo(), t);
        }
    }

    private void cloneRepo() {
        log.info("Cloning git repository {} into {}",
                this.configProperties.getRepo(),
                this.configProperties.getClonePath().toAbsolutePath());

        try (@SuppressWarnings("unused") Git localRepository = addAuthentication(Git.cloneRepository())
                .setURI(configProperties.getRepo())
                .setDirectory(configProperties.getClonePath().toFile())
                .setBranch(configProperties.getBranch())
                .call()) {
            log.info("Git repository cloned");
        } catch (GitAPIException e) {
            throw new EAPException(
                    "Failed to clone git repository " + configProperties.getRepo() + ": " + e.getMessage(), e);
        }
    }

    private void pullChanges() {
        log.debug("Pulling latest changes from repo {}", configProperties.getRepo());
        try (Git localRepository = Git.open(configProperties.getClonePath().toFile())) {
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

                            if (!StringUtils.isEmpty(configProperties.getSshKnownHostsFilePath()) && configProperties.isSshTrustUnknownHosts()) {
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

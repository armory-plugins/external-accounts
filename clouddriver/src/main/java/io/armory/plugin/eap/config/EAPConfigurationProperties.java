package io.armory.plugin.eap.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@ConfigurationProperties("armory.dag")
@Data
@Slf4j
public class EAPConfigurationProperties {
    private String repo;                             // Git repository to clone
    private String branch = "master";                // Can be specified as ref name (refs/heads/master), branch name (master) or tag name (v1.2.3)
    private String cloneDir;                         // Target directory where git repo will be downloaded
    private String configDir = "";                   // Path in the repo where config files are located
    private String configFilePrefix = "clouddriver"; // File name prefix for config files

    private String username;
    private String password;
    private String token;
    private String sshPrivateKeyFilePath;
    private String sshPrivateKeyPassphrase;
    private String sshKnownHostsFilePath;
    private boolean sshTrustUnknownHosts = false;

    private int refreshIntervalSeconds = 60;

    @PostConstruct
    public void init() throws IOException {
        if (cloneDir == null || cloneDir.isEmpty()) {
            cloneDir = System.getProperty("java.io.tmpdir") + UUID.randomUUID().toString();
            log.debug("targetDir not defined, using {}", cloneDir);
        }
        if (getClonePath().toFile().exists()) {
            FileUtils.forceDelete(getClonePath().toFile());
        }
    }

    public Path getClonePath() {
        return Paths.get(cloneDir).normalize();
    }

    public Path getConfigPath() {
        return Paths.get(configDir);
    }
}

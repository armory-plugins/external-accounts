package io.armory.plugin.eap.pollers;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("armory.external-accounts.git-poller")
@Data
@Slf4j
public class GitPollerConfigurationProperties {
    private boolean enabled = false;
    @JsonProperty("sync-interval-secs")
    private int syncIntervalSecs = 60;
    private String repo;                             // Repository to clone
    private String branch = "master";                // Can be specified as ref name (refs/heads/master), branch name (master) or tag name (v1.2.3)

    // auth
    private String username;
    private String password;
    private String token;
    @JsonProperty("ssh-private-key-file-path")
    private String sshPrivateKeyFilePath;
    @JsonProperty("ssh-private-key-passphrase")
    private String sshPrivateKeyPassphrase;
    @JsonProperty("ssh-known-hosts-file-path")
    private String sshKnownHostsFilePath;
    @JsonProperty("ssh-trust-unknown-hosts")
    private boolean sshTrustUnknownHosts = false;
}

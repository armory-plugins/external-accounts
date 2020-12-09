package io.armory.plugin.eap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

@ConfigurationProperties("armory.eap")
@Data
@Slf4j
public class EAPConfigurationProperties {

    private String dir;
    private ConfigFilePrefix filePrefix = new ConfigFilePrefix();
    private URL url;
    private FileFormat urlContentFormat;
    private GitConfig jGitPoller = new GitConfig();

    public enum FileFormat {
        YAML("yaml", "yml"), JSON("json");
        String[] extensions;
        FileFormat(String... extensions) {
            this.extensions = extensions;
        }
    }

    @Data
    public static class ConfigFilePrefix {
        @JsonIgnore
        private String itsDefault = "clouddriver";
        private String kubernetes = "kube";
        private String cloudfoundry = "cf";
        private String aws = "aws";
        private String ecs = "ecs";

        @JsonProperty("default")
        public String getDefault() {
            return itsDefault;
        }

        @JsonProperty("default")
        public void setDefault(String aDefault) {
            this.itsDefault = aDefault;
        }
    }

    @Data
    public static class GitConfig {
        private boolean enabled = false;
        private int syncIntervalSecs = 60;
        private String repo;                             // GitConfig repository to clone
        private String branch = "master";                // Can be specified as ref name (refs/heads/master), branch name (master) or tag name (v1.2.3)
        private String repoSubdir = "";

        // auth
        private String username;
        private String password;
        private String token;
        private String sshPrivateKeyFilePath;
        private String sshPrivateKeyPassphrase;
        private String sshKnownHostsFilePath;
        private boolean sshTrustUnknownHosts = false;

        public Path getRepoSubdirPath() {
            return Paths.get(repoSubdir);
        }
    }

    @PostConstruct
    public void init() {
        if (StringUtils.isEmpty(url) && StringUtils.isEmpty(dir)) {
            throw new EAPException("Either \"dir\" or \"url\" should not be supplied");
        }
        if (url != null && urlContentFormat == null) {
            throw new EAPException("If \"url\" is defined, \"urlContentFormat\" must be defined as well");
        }
    }

    public Path getDir() {
        return Paths.get(dir);
    }
}

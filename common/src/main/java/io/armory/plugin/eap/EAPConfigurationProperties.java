package io.armory.plugin.eap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

@ConfigurationProperties("armory.eap")
@Data
@Slf4j
public class EAPConfigurationProperties {

    private UrlConfig url = new UrlConfig();
    private DirConfig dir = new DirConfig();
    private GitConfig git = new GitConfig();

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
    public static class UrlConfig {
        private boolean enabled = false;
        private String url;
        private String format;
        @JsonIgnore
        private URL _url;
        @JsonIgnore
        private FileFormat _format;

        public void validate() {
            if (!enabled) {
                return;
            }
            try {
                _url = new URL(url);
            } catch(Exception e) {
                throw new EAPException("URL " + url + " is not valid", e);
            }
            try {
                _format = FileFormat.valueOf(format);
            } catch (IllegalArgumentException e) {
                throw new EAPException("Unsupported file format: " + format + " valid ones are " +
                        Arrays.toString(FileFormat.values()), e);
            }
        }

        public URL getUrl() {
            return _url;
        }

        public FileFormat getFormat() {
            return _format;
        }
    }

    @Data
    public static class DirConfig {
        private boolean enabled = false;
        private String path;
        private ConfigFilePrefix configFilePrefix = new ConfigFilePrefix();

        public void validate() {
            if (!enabled) {
                return;
            }
            if (StringUtils.isEmpty(path)) {
                throw new EAPException("\"path\" should not be empty");
            }
        }

        public Path getPath() {
            return Paths.get(path);
        }
    }

    @Data
    public static class GitConfig {
        private boolean enabled = false;
        private int syncIntervalSecs = 60;
        private String repo;                             // GitConfig repository to clone
        private String branch = "master";                // Can be specified as ref name (refs/heads/master), branch name (master) or tag name (v1.2.3)
        private String localCloneDir;                         // Target directory where git repo will be downloaded
        private String repoSubdir = "";
        private ConfigFilePrefix configFilePrefix = new ConfigFilePrefix();

        // auth
        private String username;
        private String password;
        private String token;
        private String sshPrivateKeyFilePath;
        private String sshPrivateKeyPassphrase;
        private String sshKnownHostsFilePath;
        private boolean sshTrustUnknownHosts = false;

        public void init() throws IOException {
            if (!enabled) {
                return;
            }
            if (localCloneDir == null || localCloneDir.isEmpty()) {
                localCloneDir = System.getProperty("java.io.tmpdir") + File.separator + UUID.randomUUID().toString();
                log.info("localCloneDir not defined, using {}", localCloneDir);
            }
            if (getLocalClonePath().toFile().exists()) {
                FileUtils.forceDelete(getLocalClonePath().toFile());
            }
        }

        public Path getLocalClonePath() {
            return Paths.get(localCloneDir).normalize();
        }

        public Path getRepoSubdirPath() {
            return Paths.get(repoSubdir);
        }
    }

    @PostConstruct
    public void init() throws IOException {
        url.validate();
        dir.validate();
        git.init();
    }

}

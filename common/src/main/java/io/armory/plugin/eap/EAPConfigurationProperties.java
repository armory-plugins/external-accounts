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

@ConfigurationProperties("armory.external-accounts")
@Data
@Slf4j
public class EAPConfigurationProperties {

    private String dir;
    @JsonProperty("file-prefix")
    private ConfigFilePrefix filePrefix = new ConfigFilePrefix();
    private URL url;
    @JsonProperty("url-content-format")
    private FileFormat urlContentFormat;

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
        private String dockerRegistry = "docker";

        @JsonProperty("default")
        public String getDefault() {
            return itsDefault;
        }

        @JsonProperty("default")
        public void setDefault(String aDefault) {
            this.itsDefault = aDefault;
        }
    }

    @PostConstruct
    public void init() {
        if (StringUtils.isEmpty(url) && StringUtils.isEmpty(dir)) {
            throw new EAPException("Either \"dir\" or \"url\" should be supplied");
        }
        if (url != null && urlContentFormat == null) {
            throw new EAPException("If \"url\" is defined, \"urlContentFormat\" must be defined as well");
        }
    }

    public Path getDir() {
        if (dir == null) {
            return null;
        }
        return Paths.get(dir);
    }
}

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

import io.armory.plugin.eap.EAPException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ShellGitStrategy implements GitPoller.GitStrategy {

    private static final int CMD_TIMEOUT_SEC = 30;

    private final GitPollerConfigurationProperties configProperties;
    private final Path targetDir;
    private final GitPoller.AuthType authType;

    @Data
    @RequiredArgsConstructor
    public static class ShellResult {
        private final int exitValue;
        private final String output;
    }

    public ShellGitStrategy(GitPollerConfigurationProperties configProperties, Path targetDir, GitPoller.AuthType authType) {
        this.configProperties = configProperties;
        this.targetDir = targetDir;
        this.authType = authType;
    }

    @Override
    public void cloneRepo() throws EAPException {
        String prefix = buildCmdPrefix();
        String cloneUrl = buildCloneUrl();
        ShellResult shellResult = execShellCommand(
                String.format("%s git clone --branch %s --depth 1 %s %s",
                        prefix, configProperties.getBranch(), cloneUrl, (!prefix.equals("") ? "'" : "")));
        if (shellResult.exitValue != 0) {
            throw new EAPException(
                    "Failed to clone git repository " + configProperties.getRepo() + ": " + shellResult.output);
        }
    }

    @Override
    public void pullChanges() throws EAPException {
        ShellResult shellResult = execShellCommand("cd */ && git pull");
        if (shellResult.exitValue != 0) {
            throw new EAPException(
                    "Failed to do \"git pull\" of repository " + configProperties.getRepo() + ": " + shellResult.output);
        }
    }

    private ShellResult execShellCommand(String command) {
        return execShellCommand(command, targetDir.toFile(), new HashMap<>(), null);
    }

    public static ShellResult execShellCommand(String command, File dir) {
        return execShellCommand(command, dir, new HashMap<>(), null);
    }

    public ShellResult execShellCommand(String command, String input) {
        return execShellCommand(command, targetDir.toFile(), new HashMap<>(), input);
    }

    public ShellResult execShellCommand(String command, Map<String, String> env) {
        return execShellCommand(command, targetDir.toFile(), env, null);
    }

    public ShellResult execShellCommand(String command, Map<String, String> env, String input) {
        return execShellCommand(command, targetDir.toFile(), env, input);
    }

    public static ShellResult execShellCommand(String command, File dir, Map<String, String> env, String input) {
        try {
            ProcessBuilder builder = new ProcessBuilder();
            Map<String, String> processEnv = builder.environment();
            processEnv.putAll(env);
            List<String> cmd = new ArrayList<>();
            cmd.add("bash");
            cmd.add("-c");
            cmd.add(command);
            builder.command(cmd);
            builder.directory(dir);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            if (input != null) {
                OutputStream os = process.getOutputStream();
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, UTF_8))) {
                    writer.write(input);
                    writer.flush();
                }
            }
            try (Reader reader = new InputStreamReader(process.getInputStream(), UTF_8)) {
                String output = FileCopyUtils.copyToString(reader);
                if (!process.waitFor(CMD_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    throw new EAPException("Timeout waiting for command " + command + " to finish");
                }
                return new ShellResult(process.exitValue(), output);
            }
        } catch (IOException | InterruptedException e) {
            throw new EAPException("Exception executing command " + command, e);
        }
    }

    private String buildCmdPrefix() {
        if (authType != GitPoller.AuthType.SSH) {
            return "";
        }
        String prefix = "ssh-agent bash -c '";
        if (!StringUtils.isEmpty(configProperties.getSshPrivateKeyPassphrase())) {
            prefix += String.format("echo \"%s\" | ", configProperties.getSshPrivateKeyPassphrase());
        }
        return prefix + String.format("ssh-add %s ; ", configProperties.getSshPrivateKeyFilePath());
    }

    private String buildCloneUrl() {
        try {
            URL url;
            switch (authType) {
                case HTTP:
                    url = new URL(configProperties.getRepo());
                    return String.format("%s://%s:%s@%s%s%s",
                            url.toURI().getScheme(),
                            encodeURIComponent(configProperties.getUsername()),
                            encodeURIComponent(configProperties.getPassword()),
                            url.getHost(),
                            (url.toURI().getPort() != -1 ? ":" + url.toURI().getPort() : ""),
                            url.toURI().getRawPath());
                case TOKEN:
                    url = new URL(configProperties.getRepo());
                    return String.format("%s://%s@%s%s%s",
                            url.toURI().getScheme(),
                            encodeURIComponent(configProperties.getToken()),
                            url.getHost(),
                            (url.toURI().getPort() != -1 ? ":" + url.toURI().getPort() : ""),
                            url.toURI().getRawPath());
                case SSH:
                case NONE:
                default:
                    return configProperties.getRepo();
            }
        } catch (MalformedURLException | URISyntaxException e) {
            throw new EAPException("Invalid repository url " + configProperties.getRepo(), e);
        }
    }

    private static String encodeURIComponent(String s) {
        String result;
        result = URLEncoder.encode(s, UTF_8)
                .replaceAll("\\+", "%20")
                .replaceAll("\\*", "%2A")
                .replaceAll("\\%21", "!")
                .replaceAll("\\%27", "'")
                .replaceAll("\\%28", "(")
                .replaceAll("\\%29", ")")
                .replaceAll("\\%7E", "~");
        return result;
    }
}

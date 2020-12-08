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

package io.armory.plugin.eap.it.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class GitContainer extends GenericContainer<GitContainer> {

    private static final String DOCKER_IMAGE = "gitea/gitea:1.12.6";
    private static final String REPO_PATH = "/test_repo";
    private final String authType;

    public GitContainer(String authType) {
        super(DOCKER_IMAGE);
        this.authType = authType;
        withExposedPorts(3000, 22)
                .withCopyFileToContainer(MountableFile.forClasspathResource("gitea_data"),
                        "/data")
                .withCopyFileToContainer(MountableFile.forClasspathResource("test_repo"),
                        REPO_PATH)
                .withCopyFileToContainer(MountableFile.forClasspathResource("ssh"),
                        "/root/.ssh");
    }

    @Override
    public void start() {
        super.start();
        String repoUrl = "";
        switch (authType) {
            case "ssh":
                repoUrl = "ssh://git@" + this.getContainerIpAddress() + ":" + this.getMappedPort(22) + "/test/test_repo.git";
                break;
            case "http":
                repoUrl = "http://" + this.getContainerIpAddress() + ":" + this.getMappedPort(3000) + "/test/test_repo.git";
                break;
        }
        System.setProperty("armory.eap.git.repo", repoUrl);
        initRepo();
    }

    private void initRepo() {
        ExecResult execResult = null;
        try {
            String cmd = "chmod 400 /root/.ssh/id_test_rsa";
            execResult = execInContainer("sh", "-c", cmd);
            System.out.println(cmd + ": " + execResult);
            assertThat(execResult.getExitCode()).isEqualTo(0);
            cmd = "cd " + REPO_PATH + " && git init";
            execResult = execInContainer("sh", "-c", cmd);
            System.out.println(cmd + ": " + execResult);
            assertThat(execResult.getExitCode()).isEqualTo(0);
            cmd = "cd " + REPO_PATH + " && git remote add origin git@localhost:test/test_repo.git";
            execResult = execInContainer("sh", "-c", cmd);
            System.out.println(cmd + ": " + execResult);
            assertThat(execResult.getExitCode()).isEqualTo(0);
            cmd = "git config --global user.email \"test@test.com\"";
            execResult = execInContainer("sh", "-c", cmd);
            System.out.println(cmd + ": " + execResult);
            assertThat(execResult.getExitCode()).isEqualTo(0);
            cmd = "git config --global user.name \"test\"";
            execResult = execInContainer("sh", "-c", cmd);
            System.out.println(cmd + ": " + execResult);
            assertThat(execResult.getExitCode()).isEqualTo(0);
            pushChanges();
        } catch (Exception e) {
            fail("Exception Initializing repo.\nStderr: " +
                    (execResult != null ? execResult.getStderr() : "") + "\nStdout: " +
                    (execResult != null ? execResult.getStdout() : ""), e);
        }
    }

    private void pushChanges() {
        ExecResult execResult = null;
        try {
            String cmd = "cd " + REPO_PATH + " && git add .";
            execResult = execInContainer("sh", "-c", cmd);
            System.out.println(cmd + ": " + execResult);
            assertThat(execResult.getExitCode()).isEqualTo(0);
            cmd = "cd " + REPO_PATH + " && git status --porcelain";
            execResult = execInContainer("sh", "-c", cmd);
            System.out.println(cmd + ": " + execResult);
            assertThat(execResult.getExitCode()).isEqualTo(0);
            if (!execResult.getStdout().isEmpty()) {
                cmd = "cd " + REPO_PATH + " && git commit -m \"test\"";
                execResult = execInContainer("sh", "-c", cmd);
                System.out.println(cmd + ": " + execResult);
                assertThat(execResult.getExitCode()).isEqualTo(0);
                cmd = "ssh-agent bash -c 'echo \"PiqPb2m_FodJmV.L\" | ssh-add /root/.ssh/id_test_rsa ; cd " + REPO_PATH + " && git push --set-upstream origin master'";
                execResult = execInContainer("sh", "-c", cmd);
                System.out.println(cmd + ": " + execResult);
                assertThat(execResult.getExitCode()).isEqualTo(0);
            }
        } catch (Exception e) {
            fail("Exception pushing changes to repo.\nStderr: " +
                    (execResult != null ? execResult.getStderr() : "") + "\nStdout: " +
                    (execResult != null ? execResult.getStdout() : ""), e);
        }
    }

    public void addFileContentsToRepo(Map<String, Object> fileContents, String dirInRepo, String fileName) throws IOException {
        Path filePath = Paths.get(System.getenv("BUILD_DIR"), "tmp", fileName);
        FileWriter fileWriter = new FileWriter(filePath.toFile());
        if (fileName.endsWith("json")) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(fileWriter, fileContents);
        } else {
            Yaml yaml = new Yaml(new SafeConstructor());
            yaml.dump(fileContents, fileWriter);
        }
        fileWriter.flush();
        fileWriter.close();
        addHostFileToRepo(filePath.toString(), dirInRepo, fileName);
    }

    public void addHostFileToRepo(String filePath, String dirInRepo, String fileName) {
        copyFileToContainer(MountableFile.forHostPath(filePath),
                String.format("%s/%s", REPO_PATH, (dirInRepo != null ? dirInRepo + "/" : "") + fileName));
        pushChanges();
    }

    public void addClasspathFileToRepo(String filePath, String dirInRepo, String fileName) {
        copyFileToContainer(MountableFile.forClasspathResource(filePath),
                String.format("%s/%s", REPO_PATH, (dirInRepo != null ? dirInRepo + "/" : "") + fileName));
        pushChanges();
    }

    public void removeFileFromRepo(String filePath) {
        try {
            String cmd = "rm " + REPO_PATH + "/" + filePath;
            ExecResult execResult = execInContainer("sh", "-c", cmd);
            System.out.println(cmd + ": " + execResult);
            assertThat(execResult.getExitCode()).isEqualTo(0);
            pushChanges();
        } catch (Exception e) {
           fail("Exception deleting file from repo", e);
        }
    }

    public void emptyRepo() {
        try {
            String cmd = "rm -rf " + REPO_PATH + "/*";
            ExecResult execResult = execInContainer("sh", "-c", cmd);
            System.out.println(cmd + ": " + execResult);
            assertThat(execResult.getExitCode()).isEqualTo(0);
            copyFileToContainer(MountableFile.forClasspathResource("test_repo"), REPO_PATH);
            pushChanges();
        } catch (Exception e) {
            fail("Exception resetting repo", e);
        }
    }
}

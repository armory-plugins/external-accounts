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

package io.armory.plugin.eap.it;

import io.armory.plugin.eap.it.utils.TestUtils;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;

public class CrudTest extends BaseTest {

    @DisplayName(".\n===\n"
            + "Given one kubernetes account in a file\n"
            + "  And a new account file is added to the same dir\n"
            + "When sending GET /credentials request\n"
            + "Then it should return two kubernetes accounts\n===")
    @Test
    public void shouldAddNewAccount() throws IOException, InterruptedException {
        // given
        Map<String, Object> fileContents = TestUtils.loadYaml("test_files/kube-single.yml")
                .withValue("kubernetes.accounts[0].name", "kube-1")
                .asMap();
        TestUtils.addFileContentsToTestsDir(fileContents, "kubernetes", "kube-single-1.yml");
        TestUtils.repeatUntilTrue(() -> {
            System.out.println("> GET /credentials");
            Response response = given().get(baseUrl() + "/credentials");
            response.prettyPrint();
            JsonPath jsonPath = response.jsonPath();
            List<String> credNames = jsonPath.getList("name");
            return credNames.size() == 1 && credNames.contains("kube-1");
        }, ACCOUNTS_REGISTERED_TIMEOUT_SEC, TimeUnit.SECONDS, "Waited " + ACCOUNTS_REGISTERED_TIMEOUT_SEC +
                " seconds for account \"kube-1\" to show in /credentials endpoint");

        fileContents = TestUtils.loadYaml("test_files/kube-single.yml")
                .withValue("kubernetes.accounts[0].name", "kube-2")
                .asMap();
        TestUtils.addFileContentsToTestsDir(fileContents, "kubernetes", "kube-single-2.yml");

        TestUtils.repeatUntilTrue(() -> {
            // when
            System.out.println("> GET /credentials");
            Response response = given().get(baseUrl() + "/credentials");
            response.prettyPrint();
            JsonPath jsonPath = response.jsonPath();
            List<String> credNames = jsonPath.getList("name");

            // then
            return credNames.size() == 2 && credNames.contains("kube-1") && credNames.contains("kube-2");
        }, ACCOUNTS_REGISTERED_TIMEOUT_SEC, TimeUnit.SECONDS, "Waited " + ACCOUNTS_REGISTERED_TIMEOUT_SEC +
                " seconds for accounts \"kube-1\" and \"kube-2\" to show in /credentials endpoint");
    }

    @DisplayName(".\n===\n"
            + "Given one dockerRegistry account in a file\n"
            + "  And a new account file is added to the same dir\n"
            + "When sending GET /credentials request\n"
            + "Then it should return two dockerRegistry accounts\n===")
    @Test
    public void shouldAddNewDockerRegistryAccount() throws IOException, InterruptedException {
        // given
        Map<String, Object> fileContents = TestUtils.loadYaml("test_files/docker-registry-single.yml")
                .withValue("dockerRegistry.accounts[0].name", "docker-1")
                .asMap();
        TestUtils.addFileContentsToTestsDir(fileContents, "dockerRegistry", "docker-registry-single-1.yml");
        TestUtils.repeatUntilTrue(() -> {
            System.out.println("> GET /credentials");
            Response response = given().get(baseUrl() + "/credentials");
            response.prettyPrint();
            JsonPath jsonPath = response.jsonPath();
            List<String> credNames = jsonPath.getList("name");
            return credNames.size() == 1 && credNames.contains("docker-1");
        }, ACCOUNTS_REGISTERED_TIMEOUT_SEC, TimeUnit.SECONDS, "Waited " + ACCOUNTS_REGISTERED_TIMEOUT_SEC +
                " seconds for account \"docker-1\" to show in /credentials endpoint");

        fileContents = TestUtils.loadYaml("test_files/docker-registry-single.yml")
                .withValue("dockerRegistry.accounts[0].name", "docker-2")
                .asMap();
        TestUtils.addFileContentsToTestsDir(fileContents, "dockerRegistry", "docker-registry-single-2.yml");

        TestUtils.repeatUntilTrue(() -> {
            // when
            System.out.println("> GET /credentials");
            Response response = given().get(baseUrl() + "/credentials");
            response.prettyPrint();
            JsonPath jsonPath = response.jsonPath();
            List<String> credNames = jsonPath.getList("name");

            // then
            return credNames.size() == 2 && credNames.contains("docker-1") && credNames.contains("docker-2");
        }, ACCOUNTS_REGISTERED_TIMEOUT_SEC, TimeUnit.SECONDS, "Waited " + ACCOUNTS_REGISTERED_TIMEOUT_SEC +
                " seconds for accounts \"docker-1\" and \"docker-2\" to show in /credentials endpoint");
    }

    @DisplayName(".\n===\n"
            + "Given two kubernetes accounts defined in two files\n"
            + "  And one file is deleted\n"
            + "When sending GET /credentials request\n"
            + "Then it should return one account\n===")
    @Test
    public void shouldDeleteAccount() throws IOException, InterruptedException {
        // given
        Map<String, Object> fileContents = TestUtils.loadYaml("test_files/kube-single.yml")
                .withValue("kubernetes.accounts[0].name", "kube-1")
                .asMap();
        TestUtils.addFileContentsToTestsDir(fileContents, "kubernetes", "kube-single-1.yml");
        fileContents = TestUtils.loadYaml("test_files/kube-single.yml")
                .withValue("kubernetes.accounts[0].name", "kube-2")
                .asMap();
        TestUtils.addFileContentsToTestsDir(fileContents, "kubernetes", "kube-single-2.yml");

        TestUtils.repeatUntilTrue(() -> {
            System.out.println("> GET /credentials");
            Response response = given().get(baseUrl() + "/credentials");
            response.prettyPrint();
            JsonPath jsonPath = response.jsonPath();
            List<String> credNames = jsonPath.getList("name");
            return credNames.size() == 2 && credNames.contains("kube-1") && credNames.contains("kube-2");
        }, ACCOUNTS_REGISTERED_TIMEOUT_SEC, TimeUnit.SECONDS, "Waited " + ACCOUNTS_REGISTERED_TIMEOUT_SEC +
                " seconds for accounts \"kube-1\" and \"kube-2\" to show in /credentials endpoint");

        TestUtils.deleteFileFromTestsDir("kubernetes/kube-single-2.yml");

        TestUtils.repeatUntilTrue(() -> {
            // when
            System.out.println("> GET /credentials");
            Response response = given().get(baseUrl() + "/credentials");
            response.prettyPrint();
            JsonPath jsonPath = response.jsonPath();
            List<String> credNames = jsonPath.getList("name");

            // then
            return credNames.size() == 1 && credNames.contains("kube-1");
        }, ACCOUNTS_REGISTERED_TIMEOUT_SEC, TimeUnit.SECONDS, "Waited " + ACCOUNTS_REGISTERED_TIMEOUT_SEC +
                " seconds for account \"kube-1\" to show in /credentials endpoint");
    }

    @DisplayName(".\n===\n"
            + "Given two dockerRegistry accounts defined in two files\n"
            + "  And one file is deleted\n"
            + "When sending GET /credentials request\n"
            + "Then it should return one account\n===")
    @Test
    public void shouldDeleteDockerRegistryAccount() throws IOException, InterruptedException {
        // given
        Map<String, Object> fileContents = TestUtils.loadYaml("test_files/docker-registry-single.yml")
                .withValue("dockerRegistry.accounts[0].name", "docker-1")
                .asMap();
        TestUtils.addFileContentsToTestsDir(fileContents, "dockerRegistry", "docker-registry-single-1.yml");
        fileContents = TestUtils.loadYaml("test_files/docker-registry-single.yml")
                .withValue("dockerRegistry.accounts[0].name", "docker-2")
                .asMap();
        TestUtils.addFileContentsToTestsDir(fileContents, "dockerRegistry", "docker-registry-single-2.yml");

        TestUtils.repeatUntilTrue(() -> {
            System.out.println("> GET /credentials");
            Response response = given().get(baseUrl() + "/credentials");
            response.prettyPrint();
            JsonPath jsonPath = response.jsonPath();
            List<String> credNames = jsonPath.getList("name");
            return credNames.size() == 2 && credNames.contains("docker-1") && credNames.contains("docker-2");
        }, ACCOUNTS_REGISTERED_TIMEOUT_SEC, TimeUnit.SECONDS, "Waited " + ACCOUNTS_REGISTERED_TIMEOUT_SEC +
                " seconds for accounts \"docker-1\" and \"docker-2\" to show in /credentials endpoint");

        TestUtils.deleteFileFromTestsDir("dockerRegistry/docker-registry-single-2.yml");

        TestUtils.repeatUntilTrue(() -> {
            // when
            System.out.println("> GET /credentials");
            Response response = given().get(baseUrl() + "/credentials");
            response.prettyPrint();
            JsonPath jsonPath = response.jsonPath();
            List<String> credNames = jsonPath.getList("name");

            // then
            return credNames.size() == 1 && credNames.contains("docker-1");
        }, ACCOUNTS_REGISTERED_TIMEOUT_SEC, TimeUnit.SECONDS, "Waited " + ACCOUNTS_REGISTERED_TIMEOUT_SEC +
                " seconds for account \"docker-1\" to show in /credentials endpoint");
    }

    @DisplayName(".\n===\n"
            + "Given one kubernetes account in a file\n"
            + "  And account definition is changed\n"
            + "When sending GET /credentials request\n"
            + "Then it should return updated account definition\n===")
    @Test
    public void shouldUpdateAccount() throws IOException, InterruptedException {
        // given
        Map<String, Object> fileContents = TestUtils.loadYaml("test_files/kube-single.yml")
                .withValue("kubernetes.accounts[0].name", "kube-1")
                .withValue("kubernetes.accounts[0].cacheThreads", "1")
                .asMap();
        TestUtils.addFileContentsToTestsDir(fileContents, "kubernetes", "kube-single.yml");

        TestUtils.repeatUntilTrue(() -> {
            System.out.println("> GET /credentials");
            Response response = given().get(baseUrl() + "/credentials?expand=true");
            response.prettyPrint();
            JsonPath jsonPath = response.jsonPath();
            List<Map<String, Object>> creds = jsonPath.getList("");
            return creds.size() == 1 &&
                    creds.get(0).get("name").equals("kube-1") &&
                    creds.get(0).get("cacheThreads").equals(1);
        }, ACCOUNTS_REGISTERED_TIMEOUT_SEC, TimeUnit.SECONDS, "Waited " + ACCOUNTS_REGISTERED_TIMEOUT_SEC +
                " seconds for account \"kube-1\" to show in /credentials endpoint having cacheThreads: 1");
        fileContents = TestUtils.loadYaml("test_files/kube-single.yml")
                .withValue("kubernetes.accounts[0].name", "kube-1")
                .withValue("kubernetes.accounts[0].cacheThreads", "2")
                .asMap();
        TestUtils.addFileContentsToTestsDir(fileContents, "kubernetes", "kube-single.yml");

        TestUtils.repeatUntilTrue(() -> {
            // when
            System.out.println("> GET /credentials");
            Response response = given().get(baseUrl() + "/credentials?expand=true");
            response.prettyPrint();
            JsonPath jsonPath = response.jsonPath();
            List<Map<String, Object>> creds = jsonPath.getList("");

            // then
            return creds.size() == 1 &&
                    creds.get(0).get("name").equals("kube-1") &&
                    creds.get(0).get("cacheThreads").equals(2);
        }, ACCOUNTS_REGISTERED_TIMEOUT_SEC, TimeUnit.SECONDS, "Waited " + ACCOUNTS_REGISTERED_TIMEOUT_SEC +
                " seconds for account \"kube-1\" to show in /credentials endpoint having cacheThreads: 2");
    }

    @DisplayName(".\n===\n"
            + "Given one dockerRegistry account in a file\n"
            + "  And account definition is changed\n"
            + "When sending GET /credentials request\n"
            + "Then it should return updated account definition\n===")
    @Test
    public void shouldUpdateDockerRegistryAccount() throws IOException, InterruptedException {
        // given
        Map<String, Object> fileContents = TestUtils.loadYaml("test_files/docker-registry-single.yml")
                .withValue("dockerRegistry.accounts[0].name", "docker-1")
                .withValue("dockerRegistry.accounts[0].cacheThreads", "1")
                .asMap();
        TestUtils.addFileContentsToTestsDir(fileContents, "dockerRegistry", "docker-registry-single.yml");

        TestUtils.repeatUntilTrue(() -> {
            System.out.println("> GET /credentials");
            Response response = given().get(baseUrl() + "/credentials?expand=true");
            response.prettyPrint();
            JsonPath jsonPath = response.jsonPath();
            List<Map<String, Object>> creds = jsonPath.getList("");
            return creds.size() == 1 &&
                    creds.get(0).get("name").equals("docker-1") &&
                    creds.get(0).get("cacheThreads").equals(1);
        }, ACCOUNTS_REGISTERED_TIMEOUT_SEC, TimeUnit.SECONDS, "Waited " + ACCOUNTS_REGISTERED_TIMEOUT_SEC +
                " seconds for account \"docker-1\" to show in /credentials endpoint having cacheThreads: 1");
        fileContents = TestUtils.loadYaml("test_files/docker-registry-single.yml")
                .withValue("dockerRegistry.accounts[0].name", "docker-1")
                .withValue("dockerRegistry.accounts[0].cacheThreads", "2")
                .asMap();
        TestUtils.addFileContentsToTestsDir(fileContents, "dockerRegistry", "docker-registry-single.yml");

        TestUtils.repeatUntilTrue(() -> {
            // when
            System.out.println("> GET /credentials");
            Response response = given().get(baseUrl() + "/credentials?expand=true");
            response.prettyPrint();
            JsonPath jsonPath = response.jsonPath();
            List<Map<String, Object>> creds = jsonPath.getList("");

            // then
            return creds.size() == 1 &&
                    creds.get(0).get("name").equals("docker-1") &&
                    creds.get(0).get("cacheThreads").equals(2);
        }, ACCOUNTS_REGISTERED_TIMEOUT_SEC, TimeUnit.SECONDS, "Waited " + ACCOUNTS_REGISTERED_TIMEOUT_SEC +
                " seconds for account \"docker-1\" to show in /credentials endpoint having cacheThreads: 2");
    }

    @DisplayName(".\n===\n"
            + "Given one kubernetes account in a file\n"
            + "  And account having a env var placeholder ${}\n"
            + "When sending GET /credentials request\n"
            + "Then it should return one kubernetes account with the placeholder replaced with the env var value\n===")
    @Test
    public void shouldReplaceEnvVar() throws IOException, InterruptedException {
        // given
        Map<String, Object> fileContents = TestUtils.loadYaml("test_files/kube-single.yml")
                .withValue("kubernetes.accounts[0].name", "${HOME}")
                .asMap();
        TestUtils.addFileContentsToTestsDir(fileContents, "kubernetes", "kube-single-1.yml");

        TestUtils.repeatUntilTrue(() -> {
            // when
            System.out.println("> GET /credentials");
            Response response = given().get(baseUrl() + "/credentials");
            response.prettyPrint();
            JsonPath jsonPath = response.jsonPath();
            List<String> credNames = jsonPath.getList("name");

            // then
            return credNames.size() == 1 && credNames.contains(System.getenv("HOME"));
        }, ACCOUNTS_REGISTERED_TIMEOUT_SEC, TimeUnit.SECONDS, "Waited " + ACCOUNTS_REGISTERED_TIMEOUT_SEC +
                " seconds for account \"" + System.getenv("HOME") + "\" to show in /credentials endpoint");
    }

}

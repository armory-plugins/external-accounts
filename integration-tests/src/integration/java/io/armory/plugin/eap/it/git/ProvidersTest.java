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

package io.armory.plugin.eap.it.git;

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

public class ProvidersTest extends BaseTest {

    @DisplayName(".\n===\n"
            + "Given two kubernetes accounts in one file defined in git\n"
            + "  And ssh git authentication\n"
            + "When sending GET /credentials request\n"
            + "Then it should return two kubernetes accounts\n===")
    @Test
    public void shouldLoadKubeAccountsFromSingleFile() throws IOException, InterruptedException {
        // given
        Map<String, Object> fileContents = TestUtils.loadYaml("test_files/kube-multiple.yml")
                .withValue("kubernetes.accounts[0].name", "kube-1")
                .withValue("kubernetes.accounts[1].name", "kube-2")
                .asMap();
        gitContainer.addFileContentsToRepo(fileContents, "kubernetes", "kube-multiple.yml");

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
            + "Given two kubernetes accounts in two files in git\n"
            + "  And ssh git authentication\n"
            + "When sending GET /credentials request\n"
            + "Then it should return two kubernetes accounts\n===")
    @Test
    public void shouldLoadKubeAccountsFromMultipleFiles() throws IOException, InterruptedException {
        // given
        Map<String, Object> fileContents = TestUtils.loadYaml("test_files/kube-single.yml")
                .withValue("kubernetes.accounts[0].name", "kube-1")
                .asMap();
        gitContainer.addFileContentsToRepo(fileContents, "kubernetes", "kube-single-1.yml");

        fileContents = TestUtils.loadYaml("test_files/kube-single.yml")
                .withValue("kubernetes.accounts[0].name", "kube-2")
                .asMap();
        gitContainer.addFileContentsToRepo(fileContents, "kubernetes", "kube-single-2.yml");

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
            + "Given two CF accounts in one file defined in git\n"
            + "  And ssh git authentication\n"
            + "When sending GET /credentials request\n"
            + "Then it should return two CF accounts\n===")
    @Test
    public void shouldLoadCfAccountsFromSingleFile() throws IOException, InterruptedException {
        // given
        Map<String, Object> fileContents = TestUtils.loadYaml("test_files/cf-multiple.yml")
                .withValue("cloudfoundry.accounts[0].name", "cf-1")
                .withValue("cloudfoundry.accounts[1].name", "cf-2")
                .asMap();
        gitContainer.addFileContentsToRepo(fileContents, "cloudfoundry", "cf-multiple.yml");

        TestUtils.repeatUntilTrue(() -> {
            // when
            System.out.println("> GET /credentials");
            Response response = given().get(baseUrl() + "/credentials");
            response.prettyPrint();
            JsonPath jsonPath = response.jsonPath();
            List<String> credNames = jsonPath.getList("name");

            // then
            return credNames.size() == 2 && credNames.contains("cf-1") && credNames.contains("cf-2");
        }, ACCOUNTS_REGISTERED_TIMEOUT_SEC, TimeUnit.SECONDS, "Waited " + ACCOUNTS_REGISTERED_TIMEOUT_SEC +
                " seconds for accounts \"cf-1\" and \"cf-2\" to show in /credentials endpoint");
    }

    @DisplayName(".\n===\n"
            + "Given one kubernetes account and one CF account in one file defined in git\n"
            + "  And ssh git authentication\n"
            + "When sending GET /credentials request\n"
            + "Then it should return one kubernetes account and one CF account\n===")
    @Test
    public void shouldLoadMixedAccountsFromSingleFile() throws IOException, InterruptedException {
        // given
        Map<String, Object> fileContents = TestUtils.loadYaml("test_files/clouddriver-mixed.yml")
                .withValue("kubernetes.accounts[0].name", "kube-1")
                .withValue("cloudfoundry.accounts[0].name", "cf-1")
                .asMap();
        gitContainer.addFileContentsToRepo(fileContents, null, "clouddriver-mixed.yml");

        TestUtils.repeatUntilTrue(() -> {
            // when
            System.out.println("> GET /credentials");
            Response response = given().get(baseUrl() + "/credentials");
            response.prettyPrint();
            JsonPath jsonPath = response.jsonPath();
            List<String> credNames = jsonPath.getList("name");

            // then
            return credNames.size() == 2 && credNames.contains("kube-1") && credNames.contains("cf-1");
        }, ACCOUNTS_REGISTERED_TIMEOUT_SEC, TimeUnit.SECONDS, "Waited " + ACCOUNTS_REGISTERED_TIMEOUT_SEC +
                " seconds for accounts \"kube-1\" and \"cf-1\" to show in /credentials endpoint");
    }

}

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

import com.netflix.spinnaker.clouddriver.Main;
import io.armory.plugin.eap.it.utils.GitContainer;
import io.armory.plugin.eap.it.utils.TestUtils;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;

@SpringBootTest(
        classes = {Main.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"spring.config.location = classpath:clouddriver-git.yml"})
public class GitTest {

    public static final int ACCOUNTS_REGISTERED_TIMEOUT_SEC = 20;
    public static GitContainer gitContainer = new GitContainer("ssh");

    static {
        gitContainer.start();
        String tmpDir = System.getProperty("java.io.tmpdir") + File.separator + UUID.randomUUID().toString();
        System.setProperty("armory.external-accounts.dir", tmpDir);
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @LocalServerPort
    int port;

    public String baseUrl() {
        return "http://localhost:" + port;
    }

    @BeforeEach
    public void setUp() {
        gitContainer.emptyRepo();
    }

    @DisplayName(".\n===\n"
            + "Given one kubernetes account in git\n"
            + "  And git ssh authentication\n"
            + "  And a new account file is added to git\n"
            + "When sending GET /credentials request\n"
            + "Then it should return two kubernetes accounts\n===")
    @Test
    public void shouldAddNewAccount() throws IOException, InterruptedException {
        // given
        Map<String, Object> fileContents = TestUtils.loadYaml("test_files/kube-single.yml")
                .withValue("kubernetes.accounts[0].name", "kube-1")
                .asMap();
        gitContainer.addFileContentsToRepo(fileContents, "kubernetes", "kube-single-1.yml");
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

}

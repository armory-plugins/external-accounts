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

package io.armory.plugin.eap.it.dir;

import com.netflix.spinnaker.clouddriver.Main;
import io.armory.plugin.eap.it.utils.TestUtils;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;

@SpringBootTest(
        classes = {Main.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"spring.config.location = classpath:clouddriver-dir.yml"})
public class DirTest {

    public static final int ACCOUNTS_REGISTERED_TIMEOUT_SEC = 20;

    static {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @LocalServerPort
    int port;

    public String baseUrl() {
        return "http://localhost:" + port;
    }

    @BeforeEach
    public void setUp() throws IOException {
        FileUtils.deleteDirectory(Paths.get(System.getenv("BUILD_DIR"), "tmp", "dirtests").toFile());
    }

    @DisplayName(".\n===\n"
            + "Given one kubernetes account defined in a file in a directory\n"
            + "  And a new account file is added to the directory\n"
            + "When sending GET /credentials request\n"
            + "Then it should return two kubernetes accounts\n===")
    @Test
    public void shouldAddNewAccount() throws IOException, InterruptedException {
        // given
        String fileContents = TestUtils.loadYaml("test_files/kube-single.yml")
                .withValue("kubernetes.accounts[0].name", "kube-1")
                .asString();
        FileUtils.writeStringToFile(
                Paths.get(System.getenv("BUILD_DIR"), "tmp", "dirtests", "kube-acc-1.yml").toFile(),
                fileContents,
                Charset.defaultCharset());

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
                .asString();
        FileUtils.writeStringToFile(
                Paths.get(System.getenv("BUILD_DIR"), "tmp", "dirtests", "kube-acc-2.yml").toFile(),
                fileContents,
                Charset.defaultCharset());

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

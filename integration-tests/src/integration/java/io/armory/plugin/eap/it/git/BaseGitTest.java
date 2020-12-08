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

import com.netflix.spinnaker.clouddriver.Main;
import io.armory.plugin.eap.it.utils.GitContainer;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(
        classes = {Main.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"spring.config.location = classpath:clouddriver-git.yml"})
public abstract class BaseGitTest {

    public static final int ACCOUNTS_REGISTERED_TIMEOUT_SEC = 20;
    public static GitContainer gitContainer = new GitContainer("ssh");

    static {
        gitContainer.start();
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
}

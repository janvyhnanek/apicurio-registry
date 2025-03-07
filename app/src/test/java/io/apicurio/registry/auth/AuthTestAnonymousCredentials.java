/*
 * Copyright 2021 Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apicurio.registry.auth;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.apicurio.registry.AbstractResourceTestBase;
import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.RegistryClientFactory;
import io.apicurio.registry.rest.v2.beans.ArtifactSearchResults;
import io.apicurio.registry.types.ArtifactType;
import io.apicurio.registry.utils.tests.ApicurioTestTags;
import io.apicurio.registry.utils.tests.AuthTestProfileAnonymousCredentials;
import io.apicurio.rest.client.auth.Auth;
import io.apicurio.rest.client.auth.OidcAuth;
import io.apicurio.rest.client.auth.exception.NotAuthorizedException;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/**
 * @author eric.wittmann@gmail.com
 */
@QuarkusTest
@TestProfile(AuthTestProfileAnonymousCredentials.class)
@Tag(ApicurioTestTags.DOCKER)
public class AuthTestAnonymousCredentials extends AbstractResourceTestBase {

    @ConfigProperty(name = "registry.auth.token.endpoint")
    String authServerUrl;

    String noRoleClientId = "registry-api-no-role";

    final String groupId = getClass().getSimpleName() + "Group";

    private RegistryClient createClient(Auth auth) {
        return RegistryClientFactory.create(registryV2ApiUrl, Collections.emptyMap(), auth);
    }

    @Override
    protected RegistryClient createRestClientV2() {
        Auth auth = new OidcAuth(authServerUrl, noRoleClientId, "test1", Optional.empty());
        return this.createClient(auth);
    }

    @Test
    public void testWrongCreds() throws Exception {
        Auth auth = new OidcAuth(authServerUrl, noRoleClientId, "test55", Optional.empty());
        RegistryClient client = createClient(auth);
        Assertions.assertThrows(NotAuthorizedException.class, () -> {
            client.listArtifactsInGroup(groupId);
        });
    }

    @Test
    public void testNoCredentials() throws Exception {
        RegistryClient client = RegistryClientFactory.create(registryV2ApiUrl, Collections.emptyMap(), null);
        // Read-only operation should work without any credentials.
        ArtifactSearchResults results = client.searchArtifacts(groupId, null, null, null, null, null, null, null, null);
        Assertions.assertTrue(results.getCount() >= 0);

        // Write operation should fail without any credentials
        InputStream data = new ByteArrayInputStream(("{\r\n" +
                "    \"type\" : \"record\",\r\n" +
                "    \"name\" : \"userInfo\",\r\n" +
                "    \"namespace\" : \"my.example\",\r\n" +
                "    \"fields\" : [{\"name\" : \"age\", \"type\" : \"int\"}]\r\n" +
                "}").getBytes(StandardCharsets.UTF_8));
        Assertions.assertThrows(NotAuthorizedException.class, () -> {
            client.createArtifact(groupId, "testNoCredentials", ArtifactType.AVRO, data);
        });
    }
}

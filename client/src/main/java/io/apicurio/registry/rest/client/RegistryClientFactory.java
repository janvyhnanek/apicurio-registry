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

package io.apicurio.registry.rest.client;

import io.apicurio.registry.rest.client.config.ClientConfig;
import io.apicurio.registry.rest.client.impl.ErrorHandler;
import io.apicurio.registry.rest.client.impl.RegistryClientImpl;
import io.apicurio.rest.client.auth.Auth;
import io.apicurio.rest.client.config.ApicurioClientConfig;
import io.apicurio.rest.client.spi.ApicurioHttpClient;
import io.apicurio.rest.client.spi.ApicurioHttpClientProvider;
import io.apicurio.rest.client.spi.ApicurioHttpClientServiceLoader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Carles Arnal 'carnalca@redhat.com'
 */
public class RegistryClientFactory {

    private static final AtomicReference<ApicurioHttpClientProvider> providerReference = new AtomicReference<>();
    private static final ApicurioHttpClientServiceLoader serviceLoader = new ApicurioHttpClientServiceLoader();

    private static final Map<String, String> KEYS_TO_TRANSLATE;
    private static final String BASE_PATH = "apis/registry/v2/";

    static {
        Map<String, String> map = new HashMap<>();
        map.put(ClientConfig.REGISTRY_REQUEST_HEADERS_PREFIX, ApicurioClientConfig.APICURIO_REQUEST_HEADERS_PREFIX);
        map.put(ClientConfig.REGISTRY_REQUEST_TRUSTSTORE_PREFIX, ApicurioClientConfig.APICURIO_REQUEST_HEADERS_PREFIX);
        map.put(ClientConfig.REGISTRY_REQUEST_TRUSTSTORE_LOCATION, ApicurioClientConfig.APICURIO_REQUEST_HEADERS_PREFIX);
        map.put(ClientConfig.REGISTRY_REQUEST_TRUSTSTORE_TYPE, ApicurioClientConfig.APICURIO_REQUEST_HEADERS_PREFIX);
        map.put(ClientConfig.REGISTRY_REQUEST_TRUSTSTORE_PASSWORD, ApicurioClientConfig.APICURIO_REQUEST_HEADERS_PREFIX);
        map.put(ClientConfig.REGISTRY_REQUEST_KEYSTORE_PREFIX, ApicurioClientConfig.APICURIO_REQUEST_HEADERS_PREFIX);
        map.put(ClientConfig.REGISTRY_REQUEST_KEYSTORE_LOCATION, ApicurioClientConfig.APICURIO_REQUEST_HEADERS_PREFIX);
        map.put(ClientConfig.REGISTRY_REQUEST_KEYSTORE_TYPE, ApicurioClientConfig.APICURIO_REQUEST_HEADERS_PREFIX);
        map.put(ClientConfig.REGISTRY_REQUEST_KEYSTORE_PASSWORD, ApicurioClientConfig.APICURIO_REQUEST_HEADERS_PREFIX);
        map.put(ClientConfig.REGISTRY_REQUEST_KEY_PASSWORD, ApicurioClientConfig.APICURIO_REQUEST_HEADERS_PREFIX);
        map.put(ClientConfig.REGISTRY_CLIENT_DISABLE_AUTO_BASE_PATH_APPEND, ApicurioClientConfig.APICURIO_REQUEST_HEADERS_PREFIX);
        map.put(ClientConfig.REGISTRY_CLIENT_AUTO_BASE_PATH, ApicurioClientConfig.APICURIO_CLIENT_AUTO_BASE_PATH);

        KEYS_TO_TRANSLATE = Collections.unmodifiableMap(map);
    }

    public static RegistryClient create(ApicurioHttpClient apicurioHttpClient) {
        return new RegistryClientImpl(apicurioHttpClient);
    }

    public static RegistryClient create(String basePath) {
        return create(basePath, Collections.emptyMap(), null);
    }

    public static RegistryClient create(String baseUrl, Map<String, Object> configs) {
        return create(baseUrl, configs, null);
    }

    public static RegistryClient create(String baseUrl, Map<String, Object> configs, Auth auth) {
        ApicurioHttpClientProvider p = providerReference.get();
        if (p == null) {
            providerReference.compareAndSet(null, resolveProviderInstance());
            p = providerReference.get();
        }

        if (configs.isEmpty()) {
            configs = Map.of(ClientConfig.REGISTRY_CLIENT_AUTO_BASE_PATH, BASE_PATH);
        } else if (!configs.containsKey(ClientConfig.REGISTRY_CLIENT_AUTO_BASE_PATH)) {
            configs.put(ClientConfig.REGISTRY_CLIENT_AUTO_BASE_PATH, BASE_PATH);
        }

        Map<String, Object> processedConfigs = processConfiguration(configs);

        return new RegistryClientImpl(p.create(baseUrl, processedConfigs, auth, new ErrorHandler()));
    }

    private static Map<String, Object> processConfiguration(Map<String, Object> configs) {
        final Map<String, Object> processedConfigs = new HashMap<>();

        configs.forEach((key, value) -> processedConfigs.put(KEYS_TO_TRANSLATE.getOrDefault(key, key), value));
        return processedConfigs;
    }

    public static boolean setProvider(ApicurioHttpClientProvider provider) {
        return providerReference.compareAndSet(null, provider);
    }

    private static ApicurioHttpClientProvider resolveProviderInstance() {
        return serviceLoader.providers(true)
                .next();
    }

}

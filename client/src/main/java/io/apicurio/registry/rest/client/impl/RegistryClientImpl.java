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

package io.apicurio.registry.rest.client.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.apicurio.registry.rest.Headers;
import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.exception.InvalidArtifactIdException;
import io.apicurio.registry.rest.client.exception.RestClientException;
import io.apicurio.registry.rest.client.request.Parameters;
import io.apicurio.registry.rest.client.request.provider.AdminRequestsProvider;
import io.apicurio.registry.rest.client.request.provider.GroupRequestsProvider;
import io.apicurio.registry.rest.client.request.provider.IdRequestsProvider;
import io.apicurio.registry.rest.client.request.provider.SearchRequestsProvider;
import io.apicurio.registry.rest.client.request.provider.UsersRequestsProvider;
import io.apicurio.registry.rest.v2.beans.ArtifactMetaData;
import io.apicurio.registry.rest.v2.beans.ArtifactSearchResults;
import io.apicurio.registry.rest.v2.beans.EditableMetaData;
import io.apicurio.registry.rest.v2.beans.Error;
import io.apicurio.registry.rest.v2.beans.IfExists;
import io.apicurio.registry.rest.v2.beans.LogConfiguration;
import io.apicurio.registry.rest.v2.beans.NamedLogConfiguration;
import io.apicurio.registry.rest.v2.beans.RoleMapping;
import io.apicurio.registry.rest.v2.beans.Rule;
import io.apicurio.registry.rest.v2.beans.SortBy;
import io.apicurio.registry.rest.v2.beans.SortOrder;
import io.apicurio.registry.rest.v2.beans.UpdateState;
import io.apicurio.registry.rest.v2.beans.UserInfo;
import io.apicurio.registry.rest.v2.beans.VersionMetaData;
import io.apicurio.registry.rest.v2.beans.VersionSearchResults;
import io.apicurio.registry.types.ArtifactType;
import io.apicurio.registry.types.RoleType;
import io.apicurio.registry.types.RuleType;
import io.apicurio.registry.utils.ArtifactIdValidator;
import io.apicurio.rest.client.spi.ApicurioHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Carles Arnal 'carnalca@redhat.com'
 */
public class RegistryClientImpl implements RegistryClient {

    private final ApicurioHttpClient apicurioHttpClient;
    private static final Logger logger = LoggerFactory.getLogger(RegistryClientImpl.class);

    public RegistryClientImpl(ApicurioHttpClient apicurioHttpClient) {
        this.apicurioHttpClient = apicurioHttpClient;
    }

    @Override
    public InputStream getLatestArtifact(String groupId, String artifactId) {
        return apicurioHttpClient.sendRequest(GroupRequestsProvider.getLatestArtifact(normalizeGid(groupId), artifactId));
    }

    @Override
    public ArtifactMetaData updateArtifact(String groupId, String artifactId, String version, String artifactName, String artifactDescription, String contentType, InputStream data) {
        Map<String, String> headers = headersFrom(version, artifactName, artifactDescription, contentType);
        return apicurioHttpClient.sendRequest(GroupRequestsProvider.updateArtifact(normalizeGid(groupId), artifactId, headers, data));
    }

    @Override
    public void deleteArtifact(String groupId, String artifactId) {
        apicurioHttpClient.sendRequest(GroupRequestsProvider.deleteArtifact(normalizeGid(groupId), artifactId));
    }

    @Override
    public ArtifactMetaData getArtifactMetaData(String groupId, String artifactId) {
        return apicurioHttpClient.sendRequest(GroupRequestsProvider.getArtifactMetaData(normalizeGid(groupId), artifactId));
    }

    @Override
    public void updateArtifactMetaData(String groupId, String artifactId, EditableMetaData data) {
        try {
            apicurioHttpClient.sendRequest(GroupRequestsProvider.updateArtifactMetaData(normalizeGid(groupId), artifactId, data));
        } catch (JsonProcessingException e) {
            throw new RestClientException(new Error());
        }
    }

    @Override
    public VersionMetaData getArtifactVersionMetaDataByContent(String groupId, String artifactId, Boolean canonical, String contentType, InputStream data) {
        final Map<String, List<String>> queryParams = canonical != null ? Map.of(Parameters.CANONICAL, Collections.singletonList(String.valueOf(canonical))) : Collections.emptyMap();
        final Map<String, String> headers = contentType != null ? Map.of(Headers.CONTENT_TYPE, contentType) : Collections.emptyMap();
        return apicurioHttpClient.sendRequest(GroupRequestsProvider.getArtifactVersionMetaDataByContent(normalizeGid(groupId), artifactId, headers, queryParams, data));
    }

    @Override
    public List<RuleType> listArtifactRules(String groupId, String artifactId) {
        return apicurioHttpClient.sendRequest(GroupRequestsProvider.listArtifactRules(normalizeGid(groupId), artifactId));
    }

    @Override
    public void createArtifactRule(String groupId, String artifactId, Rule data) {
        try {
            apicurioHttpClient.sendRequest(GroupRequestsProvider.createArtifactRule(normalizeGid(groupId), artifactId, data));
        } catch (JsonProcessingException e) {
            throw parseSerializationError(e);
        }
    }

    @Override
    public void deleteArtifactRules(String groupId, String artifactId) {
        apicurioHttpClient.sendRequest(GroupRequestsProvider.deleteArtifactRules(normalizeGid(groupId), artifactId));
    }

    @Override
    public Rule getArtifactRuleConfig(String groupId, String artifactId, RuleType rule) {
        return apicurioHttpClient.sendRequest(GroupRequestsProvider.getArtifactRuleConfig(normalizeGid(groupId), artifactId, rule));
    }

    @Override
    public Rule updateArtifactRuleConfig(String groupId, String artifactId, RuleType rule, Rule data) {
        try {
            return apicurioHttpClient.sendRequest(GroupRequestsProvider.updateArtifactRuleConfig(normalizeGid(groupId), artifactId, rule, data));
        } catch (JsonProcessingException e) {
            throw parseSerializationError(e);
        }
    }

    @Override
    public void deleteArtifactRule(String groupId, String artifactId, RuleType rule) {
        apicurioHttpClient.sendRequest(GroupRequestsProvider.deleteArtifactRule(normalizeGid(groupId), artifactId, rule));
    }

    @Override
    public void updateArtifactState(String groupId, String artifactId, UpdateState data) {
        try {
            apicurioHttpClient.sendRequest(GroupRequestsProvider.updateArtifactState(normalizeGid(groupId), artifactId, data));
        } catch (JsonProcessingException e) {
            throw parseSerializationError(e);
        }
    }

    @Override
    public void testUpdateArtifact(String groupId, String artifactId, String contentType, InputStream data) {
        final Map<String, String> headers = contentType != null ? Map.of(Headers.CONTENT_TYPE, contentType) : Collections.emptyMap();
        apicurioHttpClient.sendRequest(GroupRequestsProvider.testUpdateArtifact(normalizeGid(groupId), artifactId, headers, data));
    }

    @Override
    public InputStream getArtifactVersion(String groupId, String artifactId, String version) {
        return apicurioHttpClient.sendRequest(GroupRequestsProvider.getArtifactVersion(normalizeGid(groupId), artifactId, version));
    }

    @Override
    public VersionMetaData getArtifactVersionMetaData(String groupId, String artifactId, String version) {
        return apicurioHttpClient.sendRequest(GroupRequestsProvider.getArtifactVersionMetaData(normalizeGid(groupId), artifactId, version));
    }

    @Override
    public void updateArtifactVersionMetaData(String groupId, String artifactId, String version, EditableMetaData data) {
        try {
            apicurioHttpClient.sendRequest(GroupRequestsProvider.updateArtifactVersionMetaData(normalizeGid(groupId), artifactId, version, data));
        } catch (JsonProcessingException e) {
            throw parseSerializationError(e);
        }
    }

    @Override
    public void deleteArtifactVersionMetaData(String groupId, String artifactId, String version) {
        apicurioHttpClient.sendRequest(GroupRequestsProvider.deleteArtifactVersionMetaData(normalizeGid(groupId), artifactId, version));
    }

    @Override
    public void updateArtifactVersionState(String groupId, String artifactId, String version, UpdateState data) {
        try {
            apicurioHttpClient.sendRequest(GroupRequestsProvider.updateArtifactVersionState(normalizeGid(groupId), artifactId, version, data));
        } catch (JsonProcessingException e) {
            throw parseSerializationError(e);
        }
    }

    @Override
    public VersionSearchResults listArtifactVersions(String groupId, String artifactId, Integer offset, Integer limit) {
        Map<String, List<String>> queryParams = new HashMap<>();
        checkCommonQueryParams(null, null, limit, offset, queryParams);
        return apicurioHttpClient.sendRequest(GroupRequestsProvider.listArtifactVersions(normalizeGid(groupId), artifactId, queryParams));
    }

    @Override
    public VersionMetaData createArtifactVersion(String groupId, String artifactId, String version, String artifactName, String artifactDescription, String contentType, InputStream data) {
        Map<String, String> headers = headersFrom(version, artifactName, artifactDescription, contentType);
        return apicurioHttpClient.sendRequest(GroupRequestsProvider.createArtifactVersion(normalizeGid(groupId), artifactId, data, headers));
    }

    @Override
    public ArtifactSearchResults listArtifactsInGroup(String groupId, SortBy orderBy, SortOrder order, Integer offset, Integer limit) {
        final Map<String, List<String>> queryParams = new HashMap<>();
        checkCommonQueryParams(orderBy, order, limit, offset, queryParams);
        return apicurioHttpClient.sendRequest(GroupRequestsProvider.listArtifactsInGroup(normalizeGid(groupId), queryParams));
    }

    @Override
    public ArtifactMetaData createArtifact(String groupId, String artifactId, String version, ArtifactType artifactType, IfExists ifExists, Boolean canonical, String artifactName, String artifactDescription, String contentType, InputStream data) {
        if (artifactId != null && !ArtifactIdValidator.isArtifactIdAllowed(artifactId)) {
            throw new InvalidArtifactIdException();
        }
        final Map<String, String> headers = headersFrom(version, artifactName, artifactDescription, contentType);
        if (artifactId != null) {
            headers.put(Headers.ARTIFACT_ID, artifactId);
        }
        if (artifactType != null) {
            headers.put(Headers.ARTIFACT_TYPE, artifactType.name());
        }
        final Map<String, List<String>> queryParams = new HashMap<>();
        if (canonical != null) {
            queryParams.put(Parameters.CANONICAL, Collections.singletonList(String.valueOf(canonical)));
        }
        if (ifExists != null) {
            queryParams.put(Parameters.IF_EXISTS, Collections.singletonList(ifExists.value()));
        }
        return apicurioHttpClient.sendRequest(GroupRequestsProvider.createArtifact(normalizeGid(groupId), headers, data, queryParams));
    }

    @Override
    public void deleteArtifactsInGroup(String groupId) {
        apicurioHttpClient.sendRequest(GroupRequestsProvider.deleteArtifactsInGroup(normalizeGid(groupId)));
    }

    @Override
    public InputStream getContentById(long contentId) {
        return apicurioHttpClient.sendRequest(IdRequestsProvider.getContentById(contentId));
    }

    @Override
    public InputStream getContentByGlobalId(long globalId) {
        return apicurioHttpClient.sendRequest(IdRequestsProvider.getContentByGlobalId(globalId));
    }

    @Override
    public InputStream getContentByHash(String contentHash, Boolean canonical) {
        final Map<String, List<String>> queryParams = canonical != null ? Map.of(Parameters.CANONICAL, Collections.singletonList(String.valueOf(canonical))) : Collections.emptyMap();
        return apicurioHttpClient.sendRequest(IdRequestsProvider.getContentByHash(contentHash, canonical, queryParams));
    }

    @Override
    public ArtifactSearchResults searchArtifacts(String group, String name, String
            description, List<String> labels, List<String> properties, SortBy orderBy, SortOrder order, Integer offset, Integer limit) {

        final Map<String, List<String>> queryParams = new HashMap<>();

        if (name != null) {
            queryParams.put(Parameters.NAME, Collections.singletonList(name));
        }
        if (description != null) {
            queryParams.put(Parameters.DESCRIPTION, Collections.singletonList(description));
        }
        if (group != null) {
            queryParams.put(Parameters.GROUP, Collections.singletonList(group));
        }
        checkCommonQueryParams(orderBy, order, limit, offset, queryParams);
        if (labels != null && !labels.isEmpty()) {
            queryParams.put(Parameters.LABELS, labels);
        }
        if (properties != null && !properties.isEmpty()) {
            queryParams.put(Parameters.PROPERTIES, properties);
        }
        return apicurioHttpClient.sendRequest(SearchRequestsProvider.searchArtifacts(queryParams));
    }

    @Override
    public ArtifactSearchResults searchArtifactsByContent(InputStream data, SortBy orderBy, SortOrder order,
                                                          Integer offset, Integer limit) {
        final Map<String, List<String>> queryParams = new HashMap<>();
        checkCommonQueryParams(orderBy, order, limit, offset, queryParams);
        return apicurioHttpClient.sendRequest(SearchRequestsProvider.searchArtifactsByContent(data, queryParams));
    }

    @Override
    public List<RuleType> listGlobalRules() {
        return apicurioHttpClient.sendRequest(AdminRequestsProvider.listGlobalRules());
    }

    @Override
    public void createGlobalRule(Rule data) {
        try {
            apicurioHttpClient.sendRequest(AdminRequestsProvider.createGlobalRule(data));
        } catch (JsonProcessingException e) {
            throw parseSerializationError(e);
        }
    }

    @Override
    public void deleteAllGlobalRules() {
        apicurioHttpClient.sendRequest(AdminRequestsProvider.deleteAllGlobalRules());
    }

    @Override
    public Rule getGlobalRuleConfig(RuleType rule) {
        return apicurioHttpClient.sendRequest(AdminRequestsProvider.getGlobalRule(rule));
    }

    @Override
    public Rule updateGlobalRuleConfig(RuleType rule, Rule data) {
        try {
            return apicurioHttpClient.sendRequest(AdminRequestsProvider.updateGlobalRuleConfig(rule, data));
        } catch (JsonProcessingException e) {
            throw parseSerializationError(e);
        }
    }

    @Override
    public void deleteGlobalRule(RuleType rule) {
        apicurioHttpClient.sendRequest(AdminRequestsProvider.deleteGlobalRule(rule));
    }

    @Override
    public List<NamedLogConfiguration> listLogConfigurations() {
        return apicurioHttpClient.sendRequest(AdminRequestsProvider.listLogConfigurations());
    }

    @Override
    public NamedLogConfiguration getLogConfiguration(String logger) {
        return apicurioHttpClient.sendRequest(AdminRequestsProvider.getLogConfiguration(logger));
    }

    @Override
    public NamedLogConfiguration setLogConfiguration(String logger, LogConfiguration data) {
        try {
            return apicurioHttpClient.sendRequest(AdminRequestsProvider.setLogConfiguration(logger, data));
        } catch (JsonProcessingException e) {
            throw parseSerializationError(e);
        }
    }

    @Override
    public NamedLogConfiguration removeLogConfiguration(String logger) {
        return apicurioHttpClient.sendRequest(AdminRequestsProvider.removeLogConfiguration(logger));
    }

    @Override
    public InputStream exportData() {
        return apicurioHttpClient.sendRequest(AdminRequestsProvider.exportData());
    }

    @Override
    public void importData(InputStream data) {
        apicurioHttpClient.sendRequest(AdminRequestsProvider.importData(data));
    }

    @Override
    public void createRoleMapping(RoleMapping data) {
        try {
            apicurioHttpClient.sendRequest(AdminRequestsProvider.createRoleMapping(data));
        } catch (JsonProcessingException e) {
            throw parseSerializationError(e);
        }
    }

    @Override
    public void deleteRoleMapping(String principalId) {
        apicurioHttpClient.sendRequest(AdminRequestsProvider.deleteRoleMapping(principalId));
    }

    @Override
    public RoleMapping getRoleMapping(String principalId) {
        return apicurioHttpClient.sendRequest(AdminRequestsProvider.getRoleMapping(principalId));
    }

    @Override
    public List<RoleMapping> listRoleMappings() {
        return apicurioHttpClient.sendRequest(AdminRequestsProvider.listRoleMappings());
    }

    @Override
    public void updateRoleMapping(String principalId, RoleType role) {
        try {
            apicurioHttpClient.sendRequest(AdminRequestsProvider.updateRoleMapping(principalId, role));
        } catch (JsonProcessingException e) {
            throw parseSerializationError(e);
        }
    }

    @Override
    public UserInfo getCurrentUserInfo() {
        return apicurioHttpClient.sendRequest(UsersRequestsProvider.getCurrentUserInfo());
    }

    @Override
    public void setNextRequestHeaders(Map<String, String> requestHeaders) {
        apicurioHttpClient.setNextRequestHeaders(requestHeaders);
    }

    @Override
    public Map<String, String> getHeaders() {
        return apicurioHttpClient.getHeaders();
    }

    private void checkCommonQueryParams(SortBy orderBy, SortOrder order, Integer limit, Integer offset,
                                        Map<String, List<String>> queryParams) {
        if (offset != null) {
            queryParams.put(Parameters.OFFSET, Collections.singletonList(String.valueOf(offset)));
        }
        if (limit != null) {
            queryParams.put(Parameters.LIMIT, Collections.singletonList(String.valueOf(limit)));
        }
        if (order != null) {
            queryParams.put(Parameters.SORT_ORDER, Collections.singletonList(order.value()));
        }
        if (orderBy != null) {
            queryParams.put(Parameters.ORDER_BY, Collections.singletonList(orderBy.value()));
        }
    }

    private String normalizeGid(String groupId) {
        return groupId == null ? "default" : groupId;
    }

    private String encodeToBase64(String toEncode) {
        return Base64.getEncoder().encodeToString(toEncode.getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, String> headersFrom(String version, String artifactName, String artifactDescription, String contentType) {
        final Map<String, String> headers = new HashMap<>();
        if (version != null) {
            headers.put(Headers.VERSION, version);
        }
        if (artifactName != null) {
            headers.put(Headers.NAME_ENCODED, encodeToBase64(artifactName));
        }
        if (artifactDescription != null) {
            headers.put(Headers.DESCRIPTION_ENCODED, encodeToBase64(artifactDescription));
        }
        if (contentType != null) {
            headers.put(Headers.CONTENT_TYPE, contentType);
        }
        return headers;
    }

    private static RestClientException parseSerializationError(JsonProcessingException ex) {
        final Error error = new Error();
        error.setName(ex.getClass().getSimpleName());
        error.setMessage(ex.getMessage());
        logger.debug("Error serializing request response", ex);
        return new RestClientException(error);
    }
}

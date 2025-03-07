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

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.slf4j.Logger;

import io.apicurio.registry.mt.MultitenancyProperties;
import io.apicurio.registry.mt.TenantContext;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;

/**
 * @author eric.wittmann@gmail.com
 */
@Authorized @Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class AuthorizedInterceptor {

    @Inject
    Logger log;

    @Inject
    AuthConfig authConfig;

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    AdminOverride adminOverride;

    @Inject
    RoleBasedAccessController rbac;

    @Inject
    OwnerBasedAccessController obac;

    @Inject
    MultitenancyProperties mtProperties;

    @Inject
    TenantContext tenantContext;

    @AroundInvoke
    public Object authorizeMethod(InvocationContext context) throws Exception {

        //if multitenancy is enabled but no tenant context is loaded, because no tenant was resolved from request, reject it
        //this is to avoid access to default tenant "_" when multitenancy is enabled
        if (mtProperties.isMultitenancyEnabled() && !tenantContext.isLoaded()) {
            log.warn("Request is rejected because the tenant could not be found, and access to default tenant is disabled in a multitenant deployment");
            throw new ForbiddenException("Default tenant access is not allowed in multitenancy mode.");
        }

        // If the user is trying to invoke a role-mapping operation, deny it if
        // database based RBAC is not enabled.
        RoleBasedAccessApiOperation rbacOpAnnotation = context.getMethod().getAnnotation(RoleBasedAccessApiOperation.class);
        if (rbacOpAnnotation != null) {
            if (!authConfig.isApplicationRbacEnabled()) {
                log.warn("Access to /admin/roleMappings denied because application managed RBAC is not enabled.");
                throw new ForbiddenException("Application RBAC not enabled.");
            }
        }

        // If authentication is not enabled, just do it.
        if (!authConfig.authenticationEnabled) {
            return context.proceed();
        }

        log.trace("Authentication enabled, protected resource: " + context.getMethod());

        // If the securityIdentity is not set (or is anonymous)...
        if (securityIdentity == null || securityIdentity.isAnonymous()) {
            Authorized annotation = context.getMethod().getAnnotation(Authorized.class);

            // Anonymous users are allowed to perform "None" operations.
            if (annotation.level() == AuthorizedLevel.None) {
                log.trace("Anonymous user is being granted access to unprotected operation.");
                return context.proceed();
            }

            // Anonymous users are allowed to perform read-only operations, but only if
            // registry.auth.anonymous-read-access.enabled is set to 'true'
            if (authConfig.anonymousReadAccessEnabled && annotation.level() == AuthorizedLevel.Read) {
                log.trace("Anonymous user is being granted access to read-only operation.");
                return context.proceed();
            }

            // Otherwise just fail - auth was enabled but no credentials provided.
            log.warn("Authentication credentials missing and required for protected endpoint.");
            throw new UnauthorizedException("User is not authenticated.");
        }

        log.trace("                               principalId:" + securityIdentity.getPrincipal().getName());

        // If the user is an admin (via the admin-override check) then there's no need to
        // check rbac or obac.
        if (adminOverride.isAdmin()) {
            log.trace("Admin override successful.");
            return context.proceed();
        }

        // If RBAC is enabled, apply role based rules
        if (authConfig.roleBasedAuthorizationEnabled && !rbac.isAuthorized(context)) {
            log.warn("RBAC enabled and required role missing.");
            throw new ForbiddenException("User " + securityIdentity.getPrincipal().getName() + " is not authorized to perform the requested operation.");
        }

        // If Owner-only is enabled, apply ownership rules
        if (authConfig.ownerOnlyAuthorizationEnabled && !obac.isAuthorized(context)) {
            log.warn("OBAC enabled and operation not permitted due to wrong owner.");
            throw new ForbiddenException("User " + securityIdentity.getPrincipal().getName() + " is not authorized to perform the requested operation.");
        }

        return context.proceed();
    }

}

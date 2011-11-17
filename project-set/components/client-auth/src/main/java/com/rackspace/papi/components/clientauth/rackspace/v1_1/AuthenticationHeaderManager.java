package com.rackspace.papi.components.clientauth.rackspace.v1_1;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.components.clientauth.openstack.v1_0.OpenStackServiceHeader;
import com.rackspace.papi.components.clientauth.rackspace.IdentityStatus;
import com.rackspace.papi.components.clientauth.rackspace.config.RackspaceAuth;
import com.rackspace.papi.components.clientauth.rackspace.config.User;
import com.rackspace.papi.components.clientauth.rackspace.config.UserRoles;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fran
 */
public class AuthenticationHeaderManager {
    private final boolean validToken;
    private final boolean isDelegatable;
    private final boolean keystone;
    private final UserRoles userRoles;
    private final FilterDirector filterDirector;
    private final String accountUsername;
    private final String[] groups;

    public AuthenticationHeaderManager(boolean validToken, RackspaceAuth cfg, FilterDirector filterDirector, String accountUsername, String[] groups) {
        this.validToken = validToken;
        this.isDelegatable = cfg.isDelegatable();
        this.keystone = cfg.isKeystoneActive();
        this.userRoles = cfg.getUserRoles();
        this.filterDirector = filterDirector;
        this.accountUsername = accountUsername;
        this.groups = groups;
    }

    public void setFilterDirectorValues() {
        filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION.headerKey(), "proxy " + accountUsername);

        if (validToken || isDelegatable) {
            filterDirector.setFilterAction(FilterAction.PASS);
        }

        if (validToken) {
            filterDirector.requestHeaderManager().putHeader(PowerApiHeader.GROUPS.headerKey(), groups);
            filterDirector.requestHeaderManager().putHeader(PowerApiHeader.USER.headerKey(), accountUsername);            
        }

        setIdentityStatus();
        setRoles();
    }

    /**
     * Set Identity Status
     */
    private void setIdentityStatus() {
        if (isDelegatable) {
            IdentityStatus identityStatus = IdentityStatus.Confirmed;

            if (!validToken) {
                identityStatus = IdentityStatus.Indeterminate;
            }

            filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.IDENTITY_STATUS.headerKey(), identityStatus.name());
        }
    }

    /**
     * Set Roles
     * This is temporary to support roles until REPOSE supports the OpenStack Identity Service Specification
     */
    private void setRoles() {
        if (validToken) {
            if (keystone) {
                filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.TENANT_NAME.headerKey(), accountUsername);
                filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.TENANT_ID.headerKey(), accountUsername);

                List<String> roleList = new ArrayList<String>();

                for (String r : userRoles.getDefault().getRoles().getRole()) {
                    roleList.add(r);
                }

                for (User user : userRoles.getUser()) {
                    if (user.getName().equalsIgnoreCase(accountUsername)) {
                        for (String r : user.getRoles().getRole()) {
                            roleList.add(r);
                        }
                    }
                }

                if (roleList.size() > 0) {
                    filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.ROLES.headerKey(), roleList.toArray(new String[0]));
                }
            }
        }
    }
}
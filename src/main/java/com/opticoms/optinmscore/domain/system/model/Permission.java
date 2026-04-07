package com.opticoms.optinmscore.domain.system.model;

import java.util.*;

public enum Permission {

    DASHBOARD_VIEW("dashboard:view"),

    SUBSCRIBERS_VIEW("subscribers:view"),
    SUBSCRIBERS_EDIT("subscribers:edit"),

    NETWORK_CONFIG_VIEW("network-config:view"),
    NETWORK_CONFIG_EDIT("network-config:edit"),

    NETWORKS_VIEW("networks:view"),

    ALARMS_VIEW("alarms:view"),
    ALARMS_MANAGE("alarms:manage"),

    PERFORMANCE_VIEW("performance:view"),
    PERFORMANCE_EDIT("performance:edit"),

    USERS_VIEW("users:view"),
    USERS_MANAGE("users:manage"),

    INVENTORY_VIEW("inventory:view"),
    INVENTORY_EDIT("inventory:edit"),

    POLICIES_VIEW("policies:view"),
    POLICIES_EDIT("policies:edit"),

    APN_VIEW("apn:view"),
    APN_EDIT("apn:edit"),

    EDGE_LOCATIONS_VIEW("edge-locations:view"),
    EDGE_LOCATIONS_EDIT("edge-locations:edit"),

    SUCI_VIEW("suci:view"),
    SUCI_EDIT("suci:edit"),

    CERTIFICATES_VIEW("certificates:view"),
    CERTIFICATES_EDIT("certificates:edit"),

    LICENSES_VIEW("licenses:view"),

    FIREWALL_MANAGE("firewall:manage"),
    OPEN5GS_MANAGE("open5gs:manage"),
    AUDIT_VIEW("audit:view"),

    SYSTEM_MANAGE("system:manage");

    private final String value;

    Permission(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    private static final Map<User.Role, List<Permission>> ROLE_PERMISSIONS;

    static {
        ROLE_PERMISSIONS = new EnumMap<>(User.Role.class);

        ROLE_PERMISSIONS.put(User.Role.SUPER_ADMIN, List.of(
                SYSTEM_MANAGE, DASHBOARD_VIEW
        ));

        ROLE_PERMISSIONS.put(User.Role.ADMIN, List.of(
                DASHBOARD_VIEW,
                SUBSCRIBERS_VIEW, SUBSCRIBERS_EDIT,
                NETWORK_CONFIG_VIEW, NETWORK_CONFIG_EDIT,
                NETWORKS_VIEW,
                ALARMS_VIEW, ALARMS_MANAGE,
                PERFORMANCE_VIEW, PERFORMANCE_EDIT,
                USERS_VIEW, USERS_MANAGE,
                INVENTORY_VIEW, INVENTORY_EDIT,
                POLICIES_VIEW, POLICIES_EDIT,
                APN_VIEW, APN_EDIT,
                EDGE_LOCATIONS_VIEW, EDGE_LOCATIONS_EDIT,
                SUCI_VIEW, SUCI_EDIT,
                CERTIFICATES_VIEW, CERTIFICATES_EDIT,
                LICENSES_VIEW,
                FIREWALL_MANAGE, OPEN5GS_MANAGE, AUDIT_VIEW
        ));

        ROLE_PERMISSIONS.put(User.Role.OPERATOR, List.of(
                DASHBOARD_VIEW,
                SUBSCRIBERS_VIEW,
                NETWORK_CONFIG_VIEW,
                NETWORKS_VIEW,
                ALARMS_VIEW, ALARMS_MANAGE,
                PERFORMANCE_VIEW, PERFORMANCE_EDIT,
                USERS_VIEW,
                INVENTORY_VIEW, INVENTORY_EDIT,
                POLICIES_VIEW,
                APN_VIEW,
                EDGE_LOCATIONS_VIEW,
                SUCI_VIEW,
                CERTIFICATES_VIEW,
                LICENSES_VIEW
        ));

        ROLE_PERMISSIONS.put(User.Role.VIEWER, List.of(
                DASHBOARD_VIEW,
                SUBSCRIBERS_VIEW,
                NETWORK_CONFIG_VIEW,
                NETWORKS_VIEW,
                ALARMS_VIEW,
                PERFORMANCE_VIEW,
                USERS_VIEW,
                INVENTORY_VIEW,
                APN_VIEW,
                EDGE_LOCATIONS_VIEW
        ));
    }

    public static List<String> forRole(User.Role role) {
        return ROLE_PERMISSIONS.getOrDefault(role, Collections.emptyList())
                .stream()
                .map(Permission::getValue)
                .toList();
    }
}

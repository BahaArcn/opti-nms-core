package com.opticoms.optinmscore.domain.network.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * UPF (User Plane Function) Configuration.
 *
 * Contains interface IP addresses for 5G (N3) and 4G (S1-U).
 *
 * Reference: Netopsight Document Table 5 - Core Parameters for 5G & 4G
 *
 * @author Opticoms Team
 * @version 0.2.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "upf_configs")
public class UpfConfig extends BaseEntity {

    /**
     * N3 Interface IP address (5G).
     *
     * This is the IP address of the N3 interface given in IPv4 format.
     * Used for 5G core network communication with gNodeB.
     *
     * YAML file: upf.yaml
     *
     * Example: 10.45.0.1
     */
    @Pattern(
            regexp = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$",
            message = "N3 Interface IP must be a valid IPv4 address (e.g., 10.45.0.1)"
    )
    private String n3InterfaceIp;

    /**
     * S1-U Interface IP address (4G).
     *
     * This is the IP address of the S1-U interface given in IPv4 format.
     * Used for 4G core network communication with eNodeB.
     *
     * YAML file: sgwu.yaml
     *
     * Example: 10.45.0.2
     */
    @Pattern(
            regexp = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$",
            message = "S1-U Interface IP must be a valid IPv4 address (e.g., 10.45.0.2)"
    )
    private String s1uInterfaceIp;

    /**
     * N4 Interface IP address — PFCP server listen address.
     *
     * SMF's PFCP client connects to UPF at this IP.
     * LLD Note: "The PFCP server IP address shouldn't be a local IP address.
     * The configured server IP address should be reachable by the remote UPF."
     *
     * In K8s: the IP assigned to the UPF pod's n4 network interface.
     * Example: 10.10.4.1
     *
     * YAML files: upf.yaml (upf.pfcp.server), smf.yaml (smf.pfcp.client.upf.address)
     */
    @Pattern(
            regexp = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$",
            message = "N4 (PFCP) Interface IP must be a valid IPv4 address (e.g., 10.10.4.1)"
    )
    private String n4PfcpIp;

}
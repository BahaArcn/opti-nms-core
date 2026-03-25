package com.opticoms.optinmscore.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Opti-NMS Core API",
                version = "1.0.0",
                description = "OptiNMS - 5G Network Management System Backend API. " +
                        "Provides subscriber management, network configuration (AMF/SMF/UPF), " +
                        "fault management (alarms), performance monitoring, inventory tracking, " +
                        "and Open5GS integration.",
                contact = @Contact(
                        name = "Opticoms Team",
                        email = "dev@opticoms.com"
                ),
                license = @License(
                        name = "Proprietary"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local Development"),
                @Server(url = "${PROD_API_URL:http://localhost:8080}", description = "Production")
        },
        security = @SecurityRequirement(name = "bearerAuth"),
        tags = {
                @Tag(name = "auth-controller", description = "Authentication - Login and JWT token management"),
                @Tag(name = "subscriber-controller", description = "Subscriber Management - CRUD operations for 5G subscribers"),
                @Tag(name = "amf-config-controller", description = "AMF Configuration - Access and Mobility Management Function settings"),
                @Tag(name = "smf-config-controller", description = "SMF Configuration - Session Management Function settings"),
                @Tag(name = "upf-config-controller", description = "UPF Configuration - User Plane Function settings"),
                @Tag(name = "network-config-controller", description = "Global Network Configuration"),
                @Tag(name = "alarm-controller", description = "Fault Management - Alarm creation, listing, and clearance"),
                @Tag(name = "pm-controller", description = "Performance Monitoring - Metric ingestion and history"),
                @Tag(name = "inventory-controller", description = "Network Inventory - gNodeB, UE, and PDU Session tracking"),
                @Tag(name = "dashboard-controller", description = "Dashboard - Aggregated system summary and statistics")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT token obtained from POST /api/v1/auth/login. " +
                "Enter: Bearer <your-token>"
)
public class OpenApiConfig {
}
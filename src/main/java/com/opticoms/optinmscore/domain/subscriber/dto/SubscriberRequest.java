package com.opticoms.optinmscore.domain.subscriber.dto;

import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class SubscriberRequest {

    @NotBlank
    @Pattern(regexp = "^\\d{15}$", message = "IMSI must be exactly 15 digits")
    private String imsi;

    @Pattern(regexp = "^$|^\\d{10,15}$", message = "MSISDN must be 10-15 digits")
    private String msisdn;

    @Size(max = 100, message = "Label must be 100 characters or less")
    private String label;

    @NotBlank
    private String ki;

    @NotNull
    private Subscriber.UsimType usimType;

    private String opc;
    private String op;

    @Min(0)
    private long ueAmbrDl;

    @Min(0)
    private long ueAmbrUl;

    private Subscriber.SimType simType;

    @Pattern(regexp = "^[0-9a-fA-F]{12}$", message = "SQN must be 12 hex chars (6 bytes)")
    private String sqn;

    private boolean lboRoamingAllowed;
    private String policyId;
    private String edgeLocationId;

    @Valid
    @NotEmpty(message = "At least one profile is required")
    private List<Subscriber.SessionProfile> profileList;
}

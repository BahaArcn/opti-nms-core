package com.opticoms.optinmscore.domain.subscriber.dto;

import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import lombok.Data;

import java.util.List;

@Data
public class SubscriberResponse {
    private String id;
    private String imsi;
    private String msisdn;
    private String label;
    private long ueAmbrDl;
    private long ueAmbrUl;
    private Subscriber.SimType simType;
    private Subscriber.UsimType usimType;
    private String sqn;
    private boolean lboRoamingAllowed;
    private String policyId;
    private String edgeLocationId;
    private List<Subscriber.SessionProfile> profileList;
    private Subscriber.ConnectionStatus connectionStatus;
    private Long createdAt;
    private Long updatedAt;
}

package com.opticoms.optinmscore.domain.firewall.mapper;

import com.opticoms.optinmscore.domain.firewall.dto.FirewallRuleRequest;
import com.opticoms.optinmscore.domain.firewall.dto.FirewallRuleResponse;
import com.opticoms.optinmscore.domain.firewall.model.FirewallRule;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface FirewallRuleMapper {
    FirewallRule toEntity(FirewallRuleRequest request);
    FirewallRuleResponse toResponse(FirewallRule entity);
    List<FirewallRuleResponse> toResponseList(List<FirewallRule> entities);
}

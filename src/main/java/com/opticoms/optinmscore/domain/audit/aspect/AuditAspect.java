package com.opticoms.optinmscore.domain.audit.aspect;

import com.opticoms.optinmscore.common.model.BaseEntity;
import com.opticoms.optinmscore.domain.audit.model.AuditLog;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditOutcome;
import com.opticoms.optinmscore.domain.audit.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.regex.Pattern;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {

    private static final Pattern TENANT_ID_PATTERN =
            Pattern.compile("^[A-Z]{4}-\\d{4}/\\d{4}/\\d{2}$");

    private final AuditService auditService;

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        AuditLog entry = new AuditLog();
        entry.setAction(audited.action());
        entry.setEntityType(audited.entityType());
        entry.setTimestamp(new Date());

        String tenantId = extractTenantId(pjp);
        entry.setTenantId(tenantId);

        extractUserInfo(entry);
        extractHttpInfo(entry);

        try {
            Object result = pjp.proceed();

            entry.setOutcome(AuditOutcome.SUCCESS);
            entry.setEntityId(extractEntityId(result, audited.action(), pjp));
            entry.setDescription(buildDescription(audited.entityType(), audited.action(), entry.getEntityId()));

            try {
                auditService.log(entry);
            } catch (Exception e) {
                log.warn("Audit logging failed: {}", e.getMessage());
            }

            return result;
        } catch (Throwable ex) {
            entry.setOutcome(AuditOutcome.FAILURE);
            entry.setFailureReason(ex.getMessage());
            entry.setEntityId(extractEntityIdOnFailure(audited.action(), pjp));
            entry.setDescription(buildDescription(audited.entityType(), audited.action(), entry.getEntityId()));

            try {
                auditService.log(entry);
            } catch (Exception e) {
                log.warn("Audit logging failed: {}", e.getMessage());
            }

            throw ex;
        }
    }

    private String extractTenantId(ProceedingJoinPoint pjp) {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                Object tenantId = attrs.getRequest().getAttribute("tenantId");
                if (tenantId instanceof String s && !s.isBlank()) {
                    return s;
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract tenantId from request context: {}", e.getMessage());
        }
        Object[] args = pjp.getArgs();
        for (Object arg : args) {
            if (arg instanceof String s && TENANT_ID_PATTERN.matcher(s).matches()) {
                return s;
            }
        }
        return "SYSTEM";
    }

    private void extractUserInfo(AuditLog entry) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                entry.setUsername(auth.getName());
                Object principal = auth.getPrincipal();
                if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                    entry.setUserId(auth.getName());
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract user info for audit: {}", e.getMessage());
        }
    }

    private void extractHttpInfo(AuditLog entry) {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                entry.setIpAddress(request.getRemoteAddr());
                entry.setHttpMethod(request.getMethod());
                entry.setRequestUri(request.getRequestURI());
            }
        } catch (Exception e) {
            log.debug("Could not extract HTTP info for audit: {}", e.getMessage());
        }
    }

    private String extractEntityId(Object result, AuditAction action, ProceedingJoinPoint pjp) {
        if (result instanceof BaseEntity) {
            return ((BaseEntity) result).getId();
        }
        if (action == AuditAction.DELETE) {
            return extractDeleteId(pjp);
        }
        return null;
    }

    private String extractEntityIdOnFailure(AuditAction action, ProceedingJoinPoint pjp) {
        if (action == AuditAction.DELETE) {
            return extractDeleteId(pjp);
        }
        Object[] args = pjp.getArgs();
        if (args.length > 1 && args[1] instanceof String) {
            return (String) args[1];
        }
        return null;
    }

    private String extractDeleteId(ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();
        if (args.length > 1 && args[1] instanceof String) {
            return (String) args[1];
        }
        return null;
    }

    private String buildDescription(String entityType, AuditAction action, String entityId) {
        StringBuilder sb = new StringBuilder();
        sb.append(entityType).append(" ").append(action);
        if (entityId != null) {
            sb.append(" id=").append(entityId);
        }
        return sb.toString();
    }
}

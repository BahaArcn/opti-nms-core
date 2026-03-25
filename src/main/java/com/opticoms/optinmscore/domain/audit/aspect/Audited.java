package com.opticoms.optinmscore.domain.audit.aspect;

import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audited {
    AuditAction action();
    String entityType();
}

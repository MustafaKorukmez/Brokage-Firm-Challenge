package com.brokage.aspect;

import com.brokage.annotation.Auditable;
import com.brokage.dto.response.OrderResponse;
import com.brokage.entity.AuditLog;
import com.brokage.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void logAuditableAction(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            String username = getCurrentUsername();
            String action = auditable.action();
            String entityType = null;
            Long entityId = null;
            String details = buildDetails(joinPoint, result);

            if (result instanceof OrderResponse orderResponse) {
                entityType = "Order";
                entityId = orderResponse.getId();
            }

            AuditLog auditLog = AuditLog.builder()
                    .username(username)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .build();

            auditLogRepository.save(auditLog);

            log.info("Audit: {} performed {} on {} [ID: {}]",
                    username, action, entityType, entityId);

        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage());
        }
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }

    private String buildDetails(JoinPoint joinPoint, Object result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Method: ").append(joinPoint.getSignature().getName());

        Object[] args = joinPoint.getArgs();
        if (args.length > 0) {
            sb.append(", Args: ");
            for (int i = 0; i < args.length; i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append(args[i] != null ? args[i].toString() : "null");
            }
        }

        return sb.toString();
    }
}

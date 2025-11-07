package com.droid.bss.domain.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class LockInfo {
    private String lockType;
    private String mode;
    private String lockStatus;
    private Boolean granted;
    private Long blockedPid;
    private String blockedUser;
    private String blockedQuery;
    private Instant blockedQueryStart;
    private Long blockingPid;
    private String blockingUser;
    private String blockingQuery;
    private Instant blockingQueryStart;
}

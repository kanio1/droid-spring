package com.droid.bss.application.dto.asset;

import com.droid.bss.domain.asset.SIMCardEntity;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for SIM card data
 */
public record SIMCardResponse(
        UUID id,
        String iccid,
        String msisdn,
        String imsi,
        String status,
        String networkOperator,
        String apn,
        LocalDate activationDate,
        LocalDate expiryDate,
        String assignedToType,
        String assignedToId,
        String assignedToName,
        LocalDate assignedDate,
        Long dataLimitMb,
        Long dataUsedMb,
        Long voiceLimitMinutes,
        Long voiceUsedMinutes,
        Long smsLimit,
        Long smsUsed,
        LocalDate lastUsageDate
) {

    public static SIMCardResponse from(SIMCardEntity sim) {
        return new SIMCardResponse(
                sim.getId() != null ? sim.getId() : null,
                sim.getIccid(),
                sim.getMsisdn(),
                sim.getImsi(),
                sim.getStatus().name(),
                sim.getNetworkOperator(),
                sim.getApn(),
                sim.getActivationDate(),
                sim.getExpiryDate(),
                sim.getAssignedToType(),
                sim.getAssignedToId(),
                sim.getAssignedToName(),
                sim.getAssignedDate(),
                sim.getDataLimitMb(),
                sim.getDataUsedMb(),
                sim.getVoiceLimitMinutes(),
                sim.getVoiceUsedMinutes(),
                sim.getSmsLimit(),
                sim.getSmsUsed(),
                sim.getLastUsageDate()
        );
    }
}

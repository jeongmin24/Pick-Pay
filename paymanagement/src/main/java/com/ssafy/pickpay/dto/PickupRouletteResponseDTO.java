package com.ssafy.pickpay.dto;

import java.util.List;

public record PickupRouletteResponseDTO(
        String groupId,
        String roundId,
        String status,
        long startedAt,
        long durationMs,
        Long winnerUserId,
        String winnerNickname,
        int winnerIndex,
        boolean alreadySelected,
        boolean chatPushed,
        List<PickupCandidateDTO> candidates
) {
    public record PickupCandidateDTO(
            Long userId,
            String nickname
    ) {
    }
}

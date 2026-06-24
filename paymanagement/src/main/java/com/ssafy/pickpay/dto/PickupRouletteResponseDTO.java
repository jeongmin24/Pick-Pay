package com.ssafy.pickpay.dto;

import java.util.List;

public record PickupRouletteResponseDTO(
        String groupId,
        Long winnerUserId,
        String winnerNickname,
        int winnerIndex,
        boolean alreadySelected,
        List<PickupCandidateDTO> candidates
) {
    public record PickupCandidateDTO(
            Long userId,
            String nickname
    ) {
    }
}

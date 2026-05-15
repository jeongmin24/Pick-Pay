package com.miniproject.server.config;

import com.miniproject.server.domain.jwt.repository.RefreshRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ScheduleConfig {

    private final RefreshRepository refreshRepository;


    public ScheduleConfig(RefreshRepository refreshRepository) {
        this.refreshRepository = refreshRepository;
    }

    // 리프레시 토큰 저장소 8일 지난 토큰 삭제
    @Scheduled(cron = "0 0 3 * * *")
    public void refreshEntityTtlSchdule() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(8);
        refreshRepository.deleteByCreatedDateBefore(cutoff); // 리포지토리를 참조하도록 세팅했지만 서비스단을 참조해서 서비스단에 지우는 메소드를 등록하는게 더 좋음
    }
}

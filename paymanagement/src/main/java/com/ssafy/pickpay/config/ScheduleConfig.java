package com.ssafy.pickpay.config;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.pickpay.repository.RefreshRepository;

@Component
public class ScheduleConfig {
	
	private final RefreshRepository refreshRepository;

	public ScheduleConfig(RefreshRepository refreshRepository) {
		super();
		this.refreshRepository = refreshRepository;
	}
	
	@Transactional
	@Scheduled(cron = "0 0 3 * * *")
	public void refreshEntityTtlSchedule() {
		LocalDateTime cutoff = LocalDateTime.now().minusDays(8);
		refreshRepository.deleteByCreatedDateBefore(cutoff);
	}

}

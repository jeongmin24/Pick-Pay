package com.ssafy.pickpay.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.pickpay.domain.GroupOrder;
import java.util.List;


public interface GroupOrderRepository extends JpaRepository<GroupOrder, Long> {
	Optional<GroupOrder> findByShareToken(String shareToken);
	Optional<GroupOrder> findByHost_UserIdAndStatus(Long host_UserId, String status);
}

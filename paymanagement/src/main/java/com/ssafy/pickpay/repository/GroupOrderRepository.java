package com.ssafy.pickpay.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.pickpay.common.GroupOrderStatus;
import com.ssafy.pickpay.domain.GroupOrder;


public interface GroupOrderRepository extends JpaRepository<GroupOrder, String> {
	Optional<GroupOrder> findByShareToken(String shareToken);
	Optional<GroupOrder> findByHost_UserIdAndStatus(Long host_UserId, GroupOrderStatus status);
}

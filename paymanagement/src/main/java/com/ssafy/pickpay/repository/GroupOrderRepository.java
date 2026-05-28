package com.ssafy.pickpay.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.pickpay.domain.GroupOrder;

public interface GroupOrderRepository extends JpaRepository<GroupOrder, Long> {
	Optional<GroupOrder> findByShareLink(String shareLink);
}

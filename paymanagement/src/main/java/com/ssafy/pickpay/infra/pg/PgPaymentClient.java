package com.ssafy.pickpay.infra.pg;

import com.ssafy.pickpay.dto.PgConfirmResponse;

public interface PgPaymentClient {

	PgConfirmResponse confirmPayment(
			String payment,
			String orderId,
			Long amount
			);
	
}

package com.ssafy.pickpay.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ssafy.pickpay.common.GroupOrderStatus;
import com.ssafy.pickpay.common.GroupPayType;
import com.ssafy.pickpay.dto.FirebaseCartItemDTO;

@Service
public class FirebaseSyncService {
	
	/**
     * Firebase Realtime DB에서 특정 그룹의 실시간 장바구니 리스트를 읽어옵니다.
     * @param groupId String 타입의 그룹 ID (Firebase Node Key)
     */
    public List<FirebaseCartItemDTO> getCartItems(String groupId) throws Exception {
        CompletableFuture<List<FirebaseCartItemDTO>> future = new CompletableFuture<>();

        // Firebase 탐색 경로 설정: group_orders/{groupId}/items
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("group_orders/" + groupId + "/items");

        // 단회성 데이터 조회를 위해 addListenerForSingleValueEvent 사용
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
			
			@Override
			public void onDataChange(DataSnapshot snapshot) {
				List<FirebaseCartItemDTO> items = new ArrayList<>();
				
				for(DataSnapshot userNode : snapshot.getChildren()) {
					FirebaseCartItemDTO item = userNode.getValue(FirebaseCartItemDTO.class);
					if(item!=null) {
						items.add(item);
					}
				}
				// 비동기 작업 완료 신호와 함께 리스트 전달
				future.complete(items);
				
			}
			
			@Override
			public void onCancelled(DatabaseError error) {
				future.completeExceptionally(error.toException());
				
			}
		});

        // Firebase 서버로부터 데이터를 다 받아올 때까지 스레드 대기 (동기화)
        return future.get();
    }

    /**
     * 주문 마감 후 Firebase 상태를 LOCKED로 업데이트
     * */
    public void updateFirebaseGroupStatus(String groupId, GroupPayType payType) {

        DatabaseReference groupRef = FirebaseDatabase.getInstance()
                .getReference("group_orders/" + groupId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", GroupOrderStatus.LOCKED.name());
        updates.put("payType", payType.name());

        try {
            groupRef.updateChildrenAsync(updates).get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firebase 그룹 상태 업데이트 중 요청이 중단되었습니다.", e);

        } catch (ExecutionException e) {
            throw new RuntimeException("Firebase 그룹 상태 업데이트 중 오류가 발생했습니다.", e);
        }
    }
}

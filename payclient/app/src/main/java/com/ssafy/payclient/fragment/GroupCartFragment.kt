package com.ssafy.payclient.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.database.*
import com.ssafy.payclient.data.model.CartItem
import com.ssafy.payclient.databinding.FragmentGroupCartBinding

class GroupCartFragment : Fragment() {

    private var _binding: FragmentGroupCartBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private var groupId: Long = -1L
    private var isHost: Boolean = false
    private var cartItemsListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        groupId = arguments?.getLong("GROUP_ID") ?: -1L
        isHost = arguments?.getBoolean("IS_HOST") ?: false

        database = FirebaseDatabase.getInstance("https://pickpay-be337-default-rtdb.firebaseio.com/").reference

        setupToolbar() // 🔥 상단바 설정 추가

        // 방장이 아니면 주문 마감 버튼 숨기기
        if (!isHost) {
            binding.btnCloseOrder.visibility = View.GONE
        }

        loadCartData()

        binding.btnCloseOrder.setOnClickListener {
            closeOrderAndProceedToPayment()
        }
    }

    private fun setupToolbar() {
        // 상단바의 뒤로가기 아이콘 클릭 시 이전 화면으로 이동
        binding.toolbarGroupCart.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun loadCartData() {
        val itemsRef = database.child("group_orders").child(groupId.toString()).child("items")

        cartItemsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sb = StringBuilder()

                for (itemSnapshot in snapshot.children) {
                    val item = itemSnapshot.getValue(CartItem::class.java)
                    if (item != null) {
                        sb.append("👤 유저 ${item.userId}\n")
                        sb.append("   └ ${item.menuName} (x${item.quantity})\n\n")
                    }
                }

                if (sb.isEmpty()) {
                    binding.tvCartDetails.text = "장바구니가 비어 있습니다."
                } else {
                    binding.tvCartDetails.text = sb.toString()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        itemsRef.addValueEventListener(cartItemsListener!!)
    }

    private fun closeOrderAndProceedToPayment() {
        Toast.makeText(context, "주문 마감 API 호출 완료!\n결제 화면으로 이동합니다.", Toast.LENGTH_LONG).show()
        // 결제 화면 Fragment로 전환 (추후 구현)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cartItemsListener?.let {
            database.child("group_orders").child(groupId.toString()).child("items").removeEventListener(it)
        }
        _binding = null
    }
}
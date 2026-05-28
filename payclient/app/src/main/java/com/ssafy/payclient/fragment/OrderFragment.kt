package com.ssafy.payclient.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.ssafy.payclient.R

class OrderFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_order, container, false)

        val btnIndividual = view.findViewById<Button>(R.id.btn_individual_order)
        val btnGroup = view.findViewById<Button>(R.id.btn_group_order)

        // 단체 주문 버튼 클릭 시
        btnGroup.setOnClickListener {
            // GroupOrderFragment로 화면 전환 (Navigation Component를 쓰신다면 findNavController 사용)

        }

        return view
    }
}
package com.ssafy.payclient.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ssafy.payclient.R
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.model.CloseGroupOrderRequest
import com.ssafy.payclient.data.model.GroupPayType
import com.ssafy.payclient.data.network.RetrofitClient
import com.ssafy.payclient.databinding.FragmentGroupPayTypeBinding
import kotlinx.coroutines.launch
import org.json.JSONObject

class GroupPayTypeFragment : Fragment() {

    private var _binding: FragmentGroupPayTypeBinding? = null
    private val binding get() = _binding!!

    private var groupId: String = ""
    private var isHost: Boolean = false
    private var currentUserId: Long = -1L
    private var isClosing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupPayTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        groupId = arguments?.getString("GROUP_ID").orEmpty()
        isHost = arguments?.getBoolean("IS_HOST") ?: false
        currentUserId = arguments?.getLong("USER_ID") ?: -1L

        setupToolbar()
        setupPayTypeButtons()
    }

    private fun setupToolbar() {
        binding.toolbarGroupPayType.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupPayTypeButtons() {
        binding.tvGroupPayTypeSummary.text = "그룹방 번호 $groupId"

        if (!isHost) {
            binding.btnDutchPay.isEnabled = false
            binding.btnHostPay.isEnabled = false
            binding.tvGroupPayTypeDescription.text = "방장만 주문을 마감할 수 있습니다."
            return
        }

        binding.btnDutchPay.setOnClickListener {
            closeGroupOrder(GroupPayType.DUTCH)
        }

        binding.btnHostPay.setOnClickListener {
            closeGroupOrder(GroupPayType.HOST)
        }
    }

    private fun closeGroupOrder(payType: GroupPayType) {
        if (isClosing) return

        if (groupId.isBlank()) {
            Toast.makeText(requireContext(), "그룹방 정보를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tokenManager = TokenManager(requireContext().applicationContext)
                val apiService = RetrofitClient.getGroupOrderApiService(tokenManager)
                val response = apiService.closeGroupOrder(
                    groupId = groupId,
                    request = CloseGroupOrderRequest(payType)
                )

                if (response.isSuccessful) {
                    val message = response.body()?.string()
                        ?.takeIf { it.isNotBlank() }
                        ?: "주문이 마감되었습니다."
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

                    navigatePaymentWaiting()
                } else {
                    Toast.makeText(
                        requireContext(),
                        closeOrderErrorMessage(response.code(), response.errorBody()?.string()),
                        Toast.LENGTH_SHORT
                    ).show()
                    setLoading(false)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "주문 마감 중 오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                setLoading(false)
            }
        }
    }

    private fun navigatePaymentWaiting() {
        findNavController().navigate(
            R.id.action_fragment_group_pay_type_to_fragment_group_payment_waiting,
            Bundle().apply {
                putString(GroupPaymentWaitingFragment.ARG_GROUP_ID, groupId)
                putLong(GroupPaymentWaitingFragment.ARG_USER_ID, currentUserId)
            }
        )
    }

    private fun setLoading(loading: Boolean) {
        isClosing = loading
        binding.btnDutchPay.isEnabled = !loading
        binding.btnHostPay.isEnabled = !loading
        binding.progressGroupPayType.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun closeOrderErrorMessage(code: Int, errorBody: String?): String {
        val serverMessage = errorBody
            ?.let { body ->
                runCatching { JSONObject(body).optString("message") }.getOrNull()
                    ?.takeIf { it.isNotBlank() }
            }
        return serverMessage ?: "주문 마감에 실패했습니다. ($code)"
    }

    override fun onResume() {
        super.onResume()
        activity?.findViewById<View>(R.id.bottom_navigation)?.visibility = View.GONE
    }

    override fun onStop() {
        super.onStop()
        activity?.findViewById<View>(R.id.bottom_navigation)?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

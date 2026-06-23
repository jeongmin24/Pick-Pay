package com.ssafy.payclient

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.network.RetrofitClient
import com.ssafy.payclient.data.repository.AuthRepository
import com.ssafy.payclient.databinding.ActivityMainBinding
import com.ssafy.payclient.ui.login.LoginActivity
import com.ssafy.payclient.ui.menu.MenuViewModel // 추가
import com.ssafy.payclient.ui.menu.MenuViewModelFactory
import com.ssafy.payclient.util.ViewModelFactory
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val sharedTokenManager by lazy { TokenManager(applicationContext) }

    private val viewModel: MainViewModel by viewModels {
        val apiService = RetrofitClient.getApiService(sharedTokenManager)
        val repository = AuthRepository(apiService)
        ViewModelFactory(repository, sharedTokenManager)
    }

    private val menuViewModel: MenuViewModel by viewModels {
        MenuViewModelFactory(sharedTokenManager)
    }

    // NFC 관련 변수
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var nfcPendingIntent: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        nfcPendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        initViews()
        handlePaymentDeepLink(getIntent())
    }

    // 4. 앱이 화면에 켜져 있을 때 NFC 시스템 이벤트를 선점(독점)
    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null)
    }

    // 5. 앱이 화면에서 벗어나면 독점 해제
    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePaymentDeepLink(intent)

        // 6. NFC 태그가 인식되었는지 체크
        handleMenuNfcIntent(intent)
    }

    private fun initViews() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)
        binding.bottomNavigation.itemIconTintList = null
    }

    private fun handleMenuNfcIntent(intent: Intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {

            val menuId = parseNfcIntent(intent)
            if (menuId != null) {
                menuViewModel.onMenuNfcScanned(menuId) { success ->
                    if (success) {
                        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                        val navController = navHostFragment?.navController

                        navController?.navigate(R.id.fragment_cart)
                    } else {
                        Toast.makeText(this, "NFC 상품 장바구니 추가 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun parseNfcIntent(intent: Intent): Long? {
        val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES) ?: return null
        try {
            val record = (rawMsgs[0] as NdefMessage).records[0]
            val payload = record.payload

            val isTextRecord = record.tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN
                    && java.util.Arrays.equals(record.type, android.nfc.NdefRecord.RTD_TEXT)

            val resultString = if (isTextRecord) {
                val languageCodeLength = (payload[0].toInt() and 0x003F)
                String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, Charset.forName("UTF-8"))
            } else {
                String(payload, Charset.forName("UTF-8"))
            }

            return resultString.trim().toLongOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun handlePaymentDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "tosspayments" || uri.host != "payment") return

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val navController = navHostFragment?.navController ?: return
        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.set(PAYMENT_DEEP_LINK_URI, uri.toString())
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        const val PAYMENT_DEEP_LINK_URI = "PAYMENT_DEEP_LINK_URI"
    }
}
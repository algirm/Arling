package com.algirm.arling.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.algirm.arling.R
import com.algirm.arling.databinding.ActivityLoginBinding
import com.algirm.arling.ui.main.MainActivity
import com.algirm.arling.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class LoginActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    private var sektor = 9

//    @Inject
//    lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        initActionGo()

        lifecycleScope.launchWhenResumed {
            viewModel.authResult.collect { loginResult ->
                when(loginResult) {
                    is Resource.Failure -> {
                        Toast.makeText(
                            this@LoginActivity,
                            loginResult.throwable?.message?: "Terjadi Kesalahan",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.progressBar.hide()
                    }
                    is Resource.Init -> {}
                    is Resource.Loading -> {
                        binding.progressBar.show()
                    }
                    is Resource.Success -> {
                        Toast.makeText(
                            this@LoginActivity,
                            "Masuk sebagai ${loginResult.data.displayName!!.split("?")[0]}",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.progressBar.hide()
                        startActivity(Intent(this@LoginActivity, SectorActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }

    private fun signIn() {
        val username = binding.loginId.text.toString()
        val password = binding.loginPw.text.toString()
        if (username.isBlank()) {
            binding.loginId.error = "Username tidak boleh kosong"
            return
        }
        if (password.isBlank()) {
            binding.loginPw.error = "Password tidak boleh kosong"
            return
        }
        viewModel.signIn("${username}@cimak.id", password, sektor)
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        when (p0?.id) {
//            binding.sektorSpinner.id -> sektor = p2
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        Toast.makeText(this, "Pilih Sektor", Toast.LENGTH_SHORT).show()
    }

    private fun initView() {
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        binding.loginButton.setOnClickListener { signIn() }

//        ArrayAdapter.createFromResource(
//            this,
//            R.array.sektor_list,
//            android.R.layout.simple_spinner_item
//        ).also { adapter ->
//            adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
//            binding.sektorSpinner.adapter = adapter
//            binding.sektorSpinner.onItemSelectedListener = this
//        }
    }

    private fun initActionGo() {
        binding.loginPw.setOnEditorActionListener { _, i, _ ->
            if (i == EditorInfo.IME_ACTION_GO) {
                signIn()
                val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                im.hideSoftInputFromWindow(
                    currentFocus?.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }
}
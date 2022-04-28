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
import com.algirm.arling.databinding.ActivitySectorBinding
import com.algirm.arling.ui.main.MainActivity
import com.algirm.arling.util.Resource
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class SectorActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var binding: ActivitySectorBinding
    private val viewModel: AuthViewModel by viewModels()

    private var sektor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySectorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()

        lifecycleScope.launchWhenResumed {
            viewModel.selectResult.collect { loginResult ->
                when(loginResult) {
                    is Resource.Failure -> {
                        Toast.makeText(
                            this@SectorActivity,
                            loginResult.throwable?.message?: "Terjadi Kesalahan",
                            Toast.LENGTH_LONG
                        ).show()
//                        binding.progressBar.hide()
                    }
                    is Resource.Init -> {}
                    is Resource.Loading -> {
//                        binding.progressBar.show()
                    }
                    is Resource.Success -> {
                        Toast.makeText(
                            this@SectorActivity,
                            "Masuk sebagai ${loginResult.data.displayName!!.split("?")[0]}",
                            Toast.LENGTH_LONG
                        ).show()
//                        binding.progressBar.hide()
                        startActivity(Intent(this@SectorActivity, MainActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        when (p0?.id) {
            binding.sektorSpinner.id -> sektor = p2
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        Toast.makeText(this, "Pilih Sektor", Toast.LENGTH_SHORT).show()
    }

    private fun initView() {
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        binding.buttonLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        binding.buttonMasuk.setOnClickListener {
            viewModel.pilihSektor(sektor)
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.sektor_list,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
            binding.sektorSpinner.adapter = adapter
            binding.sektorSpinner.onItemSelectedListener = this
        }
    }

}
package com.example.suicahistory.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.suicahistory.SuicaApp
import com.example.suicahistory.data.CredentialStore
import com.example.suicahistory.databinding.ActivitySettingsBinding
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(com.example.suicahistory.R.string.title_settings)

        // 保存済み認証情報を表示
        CredentialStore.load(this)?.let { (email, _) ->
            binding.emailInput.setText(email)
            binding.savedStatus.visibility = View.VISIBLE
            binding.savedStatus.text = "保存済み: $email"
        }

        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text?.toString()?.trim() ?: ""
            val password = binding.passwordInput.text?.toString() ?: ""
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "メールアドレスとパスワードを入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            doLoginAndSync(email, password)
        }

        binding.clearButton.setOnClickListener {
            CredentialStore.clear(this)
            binding.emailInput.text?.clear()
            binding.passwordInput.text?.clear()
            binding.savedStatus.visibility = View.GONE
            Toast.makeText(this, "ログイン情報を削除しました", Toast.LENGTH_SHORT).show()
        }
    }

    private fun doLoginAndSync(email: String, password: String) {
        // アプリ全体のシングルトンRepositoryを使用してセッションを共有
        val repository = (application as SuicaApp).repository

        binding.loginButton.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val ok = repository.loginWeb(email, password)
            if (ok) {
                // 認証情報を保存（以降のWeb同期でも使えるように）
                CredentialStore.save(this@SettingsActivity, email, password)
                binding.savedStatus.text = "保存済み: $email"
                binding.savedStatus.visibility = View.VISIBLE
                Toast.makeText(this@SettingsActivity, "ログイン成功。履歴を取得中...", Toast.LENGTH_SHORT).show()

                val count = repository.syncFromWeb()
                Toast.makeText(
                    this@SettingsActivity,
                    "${count}件の履歴を取得しました",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this@SettingsActivity,
                    "ログインに失敗しました。メールアドレスとパスワードを確認してください",
                    Toast.LENGTH_LONG
                ).show()
            }
            binding.loginButton.isEnabled = true
            binding.progressBar.visibility = View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

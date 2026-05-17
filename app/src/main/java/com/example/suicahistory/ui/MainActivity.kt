package com.example.suicahistory.ui

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.suicahistory.R
import com.example.suicahistory.data.CredentialStore
import com.example.suicahistory.databinding.ActivityMainBinding
import com.example.suicahistory.ui.home.HomeFragment
import com.example.suicahistory.ui.monthly.MonthlyFragment
import com.example.suicahistory.ui.settings.SettingsActivity
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private var nfcAdapter: NfcAdapter? = null
    private var nfcPendingIntent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        viewModel = ViewModelProvider(
            this,
            MainViewModel.Factory(application)
        )[MainViewModel::class.java]

        setupTabs()
        setupNfc()
        observeViewModel()
    }

    private fun setupTabs() {
        val titles = listOf(getString(R.string.tab_daily), getString(R.string.tab_monthly))
        binding.viewPager.adapter = object : androidx.viewpager2.adapter.FragmentStateAdapter(this) {
            override fun getItemCount() = 2
            override fun createFragment(position: Int) = when (position) {
                0 -> HomeFragment()
                else -> MonthlyFragment()
            }
        }
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            tab.text = titles[pos]
        }.attach()
    }

    private fun setupNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) return
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        nfcPendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
    }

    private fun observeViewModel() {
        viewModel.nfcResult.observe(this) { count ->
            Toast.makeText(this, "NFC: ${count}件取得しました", Toast.LENGTH_SHORT).show()
        }
        viewModel.webSyncResult.observe(this) { count ->
            Toast.makeText(this, "Web同期完了: ${count}件取得しました", Toast.LENGTH_LONG).show()
        }
        viewModel.error.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
        viewModel.syncInProgress.observe(this) { inProgress ->
            binding.progressBar.visibility = if (inProgress) View.VISIBLE else View.GONE
            invalidateOptionsMenu()
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        else @Suppress("DEPRECATION") intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            Toast.makeText(this, "Suicaを検出しました。読み取り中...", Toast.LENGTH_SHORT).show()
            viewModel.importFromNfc(tag)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val syncing = viewModel.syncInProgress.value == true
        menu.findItem(R.id.action_sync_web)?.let {
            it.isEnabled = !syncing
            it.title = if (syncing) "同期中..." else "Web同期"
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_sync_web -> {
                if (!CredentialStore.hasSaved(this)) {
                    Toast.makeText(this, "先に設定からログインしてください", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, SettingsActivity::class.java))
                } else {
                    viewModel.syncFromWeb()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

package com.example.suicahistory.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.suicahistory.data.db.Transaction
import com.example.suicahistory.databinding.FragmentHomeBinding
import com.example.suicahistory.ui.MainViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.text.SimpleDateFormat
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: DailyTransactionAdapter

    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            MainViewModel.Factory(requireActivity())
        )[MainViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = DailyTransactionAdapter()
        binding.recyclerView.adapter = adapter

        binding.nfcFab.setOnClickListener {
            binding.nfcHint.visibility = View.VISIBLE
        }

        viewModel.repository.getAllTransactions()
            .onEach { transactions -> showGrouped(transactions) }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun showGrouped(transactions: List<Transaction>) {
        binding.nfcHint.visibility = View.GONE
        val dateFormat = SimpleDateFormat("yyyy/MM/dd (E)", Locale.JAPAN)
        val grouped = transactions.groupBy { dateFormat.format(it.date) }
        val items = mutableListOf<DailyItem>()
        grouped.forEach { (date, txList) ->
            items.add(DailyItem.Header(date, txList.sumOf { if (it.amount < 0) -it.amount else 0 }))
            txList.forEach { items.add(DailyItem.Row(it)) }
        }
        adapter.submitList(items)
        binding.emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

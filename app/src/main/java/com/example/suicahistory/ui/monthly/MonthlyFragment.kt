package com.example.suicahistory.ui.monthly

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.suicahistory.data.db.MonthlyCategoryTotal
import com.example.suicahistory.databinding.FragmentMonthlyBinding
import com.example.suicahistory.ui.MainViewModel
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MonthlyFragment : Fragment() {

    private var _binding: FragmentMonthlyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            MainViewModel.Factory(requireActivity())
        )[MainViewModel::class.java]
    }

    private val categoryColors = mapOf(
        "コンビニ" to Color.parseColor("#FF7043"),
        "交通" to Color.parseColor("#42A5F5"),
        "飲食" to Color.parseColor("#66BB6A"),
        "スーパー" to Color.parseColor("#FFA726"),
        "ドラッグ" to Color.parseColor("#AB47BC"),
        "チャージ" to Color.parseColor("#78909C"),
        "その他" to Color.parseColor("#BDBDBD")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMonthlyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.repository.getMonthlyCategoryTotals()
            .onEach { updateCharts(it) }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun updateCharts(data: List<MonthlyCategoryTotal>) {
        if (data.isEmpty()) return

        val months = data.map { it.month }.distinct().sortedDescending().take(6).reversed()
        val categories = data.map { it.category }.distinct()

        // 積み上げ棒グラフ
        val barDataSets = categories.map { category ->
            val entries = months.mapIndexed { idx, month ->
                val total = data.firstOrNull { it.month == month && it.category == category }?.total ?: 0
                BarEntry(idx.toFloat(), total.toFloat())
            }
            BarDataSet(entries, category).apply {
                color = categoryColors[category] ?: Color.GRAY
                setDrawValues(false)
            }
        }

        binding.barChart.apply {
            val monthLabels = months.map { m -> if (m.length >= 7) m.substring(5) else m }
            this.data = BarData(barDataSets)
            xAxis.valueFormatter = IndexAxisValueFormatter(monthLabels)
            xAxis.granularity = 1f
            description.isEnabled = false
            legend.isEnabled = true
            animateY(600)
            invalidate()
        }

        // 今月のカテゴリ円グラフ
        val latestMonth = months.lastOrNull() ?: return
        val pieEntries = data.filter { it.month == latestMonth && it.total > 0 }.map {
            PieEntry(it.total.toFloat(), it.category)
        }
        if (pieEntries.isEmpty()) return

        val pieDataSet = PieDataSet(pieEntries, "").apply {
            colors = pieEntries.map { categoryColors[it.label] ?: Color.GRAY }
            valueTextSize = 12f
        }

        val centerLabel = if (latestMonth.length >= 7) latestMonth.substring(5) + "月" else latestMonth

        binding.pieChart.apply {
            this.data = PieData(pieDataSet)
            centerText = centerLabel
            setCenterTextSize(16f)
            description.isEnabled = false
            animateY(600)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

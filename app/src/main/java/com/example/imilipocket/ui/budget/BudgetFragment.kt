package com.example.imilipocket.ui.budget

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.imilipocket.R
import com.example.imilipocket.data.PreferenceManager
import com.example.imilipocket.databinding.FragmentBudgetBinding
import com.example.imilipocket.util.NotificationHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BudgetFragment : Fragment() {
    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var viewModel: BudgetViewModel
    private lateinit var notificationHelper: NotificationHelper

    private val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            _binding = FragmentBudgetBinding.inflate(inflater, container, false)
            preferenceManager = PreferenceManager(requireContext())
            viewModel = ViewModelProvider(
                this,
                BudgetViewModel.Factory(preferenceManager)
            )[BudgetViewModel::class.java]
            notificationHelper = NotificationHelper(requireContext())

            setupUI()
            setupClickListeners()
            observeViewModel()

            return binding.root
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error initializing budget screen", Toast.LENGTH_SHORT).show()
            return binding.root
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            updateBudgetProgress()
            updateBreakdownSummary()
            updateSmartAlert()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupUI() {
        try {
            // Set the current month
            binding.tvCurrentMonth.text = dateFormat.format(Date())

            // Set the initial budget value
            val currentBudget = preferenceManager.getMonthlyBudget()
            binding.etBudgetAmount.setText(currentBudget.toString())

            // Update UI elements
            updateBudgetProgress()
            updateBreakdownSummary()
            updateSmartAlert()
        } catch (e: Exception) {
            e.printStackTrace()
            binding.etBudgetAmount.setText("0")
        }
    }

    private fun setupClickListeners() {
        binding.btnSaveBudget.setOnClickListener {
            saveBudget()
        }
    }

    private fun observeViewModel() {
        viewModel.budget.observe(viewLifecycleOwner) { budget ->
            try {
                binding.etBudgetAmount.setText(budget.toString())
                updateBudgetProgress()
                updateBreakdownSummary()
                updateSmartAlert()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        viewModel.budget.observe(viewLifecycleOwner) { expenses ->
            try {
                updateBudgetProgress()
                updateBreakdownSummary()
                updateSmartAlert()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveBudget() {
        try {
            val budgetText = binding.etBudgetAmount.text.toString()
            if (budgetText.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a budget amount", Toast.LENGTH_SHORT).show()
                return
            }

            val budget = budgetText.toDouble()
            if (budget < 0) {
                Toast.makeText(requireContext(), "Budget cannot be negative", Toast.LENGTH_SHORT).show()
                return
            }

            viewModel.updateBudget(budget)
            showBudgetNotification()
            Toast.makeText(requireContext(), "Budget updated", Toast.LENGTH_SHORT).show()

            // Update UI after saving
            updateBudgetProgress()
            updateBreakdownSummary()
            updateSmartAlert()
        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Invalid budget amount", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error saving budget", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBudgetNotification() {
        try {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val monthlyBudget = preferenceManager.getMonthlyBudget()
                notificationHelper.showBudgetNotification(monthlyBudget)
            } else {
                // Request permission if not granted
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateBudgetProgress() {
        try {
            val monthlyBudget = preferenceManager.getMonthlyBudget()
            val monthlyExpenses = viewModel.getMonthlyExpenses()
            val progress = if (monthlyBudget > 0) {
                ((monthlyExpenses / monthlyBudget) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }

            binding.progressCircular.progress = progress
            binding.tvProgressPercentage.text = "$progress%"
            binding.tvUsageStatus.text = "You've used $progress% of your budget"

            // Update progress indicator color based on usage
            val indicatorColor = when {
                progress >= 90 -> ContextCompat.getColor(requireContext(), R.color.red_500)
                progress >= 75 -> ContextCompat.getColor(requireContext(), R.color.orange_500)
                else -> ContextCompat.getColor(requireContext(), R.color.green_500)
            }
            binding.progressCircular.setIndicatorColor(indicatorColor)
        } catch (e: Exception) {
            e.printStackTrace()
            binding.progressCircular.progress = 0
            binding.tvProgressPercentage.text = "0%"
            binding.tvUsageStatus.text = "You've used 0% of your budget"
        }
    }

    private fun updateBreakdownSummary() {
        try {
            val monthlyBudget = preferenceManager.getMonthlyBudget()
            val monthlyExpenses = viewModel.getMonthlyExpenses()
            val remainingBudget = (monthlyBudget - monthlyExpenses).coerceAtLeast(0.0)

            binding.tvMonthlyBudgetValue.text = formatCurrency(monthlyBudget)
            binding.tvTotalExpensesValue.text = formatCurrency(monthlyExpenses)
            binding.tvRemainingBudgetValue.text = formatCurrency(remainingBudget)
        } catch (e: Exception) {
            e.printStackTrace()
            binding.tvMonthlyBudgetValue.text = formatCurrency(0.0)
            binding.tvTotalExpensesValue.text = formatCurrency(0.0)
            binding.tvRemainingBudgetValue.text = formatCurrency(0.0)
        }
    }

    private fun updateSmartAlert() {
        try {
            val monthlyBudget = preferenceManager.getMonthlyBudget()
            val monthlyExpenses = viewModel.getMonthlyExpenses()
            val progress = if (monthlyBudget > 0) {
                ((monthlyExpenses / monthlyBudget) * 100).toInt()
            } else {
                0
            }

            val (message, icon, tint) = when {
                monthlyBudget == 0.0 -> {
                    Triple(
                        "No budget set. Please set a monthly budget.",
                        R.drawable.ic_warning,
                        R.color.orange_500
                    )
                }
                progress >= 90 -> {
                    Triple(
                        "Warning: You've used over 90% of your budget!",
                        R.drawable.ic_warning,
                        R.color.red_500
                    )
                }
                progress >= 75 -> {
                    Triple(
                        "You're nearing your budget limit. Be cautious!",
                        R.drawable.ic_warning,
                        R.color.orange_500
                    )
                }
                else -> {
                    Triple(
                        "Great job! You're spending wisely.",
                        R.drawable.ic_check_circle,
                        R.color.green_500
                    )
                }
            }

            binding.tvAlertMessage.text = message
            binding.ivAlertIcon.setImageResource(icon)
            binding.ivAlertIcon.setColorFilter(ContextCompat.getColor(requireContext(), tint))
        } catch (e: Exception) {
            e.printStackTrace()
            binding.tvAlertMessage.text = "Error loading alert message"
            binding.ivAlertIcon.setImageResource(R.drawable.ic_warning)
            binding.ivAlertIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red_500))
        }
    }

    private fun formatCurrency(amount: Double): String {
        return try {
            val currency = preferenceManager.getSelectedCurrency()
            val locale = when (currency) {
                "USD" -> Locale.US
                "EUR" -> Locale.GERMANY
                "GBP" -> Locale.UK
                "JPY" -> Locale.JAPAN
                "INR" -> Locale("en", "IN")
                "AUD" -> Locale("en", "AU")
                "CAD" -> Locale("en", "CA")
                "LKR" -> Locale("si", "LK")
                "CNY" -> Locale("zh", "CN")
                "SGD" -> Locale("en", "SG")
                "MYR" -> Locale("ms", "MY")
                "THB" -> Locale("th", "TH")
                "IDR" -> Locale("id", "ID")
                "PHP" -> Locale("en", "PH")
                "VND" -> Locale("vi", "VN")
                "KRW" -> Locale("ko", "KR")
                "AED" -> Locale("ar", "AE")
                "SAR" -> Locale("ar", "SA")
                "QAR" -> Locale("ar", "QA")
                else -> Locale.US
            }
            val format = NumberFormat.getCurrencyInstance(locale)
            format.format(amount)
        } catch (e: Exception) {
            e.printStackTrace()
            "$0.00"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showBudgetNotification()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Notification permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
package com.example.imilipocket.ui.transactions

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.imilipocket.R
import com.example.imilipocket.data.Transaction
import com.example.imilipocket.databinding.FragmentTransactionsBinding
import com.example.imilipocket.data.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class TransactionsFragment : Fragment() {

    private lateinit var binding: FragmentTransactionsBinding
    private val viewModel: TransactionsViewModel by viewModels {
        TransactionsViewModelFactory(PreferenceManager(requireContext()), requireContext())
    }
    private lateinit var adapter: TransactionsAdapter
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        preferenceManager = PreferenceManager(requireContext()) // Initialize PreferenceManager
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        if (isAdded && !isDetached) {
            viewModel.loadTransactions()
        }
    }

    private fun setupRecyclerView() {
        adapter = TransactionsAdapter(
            onEditClick = { transaction ->
                val action = TransactionsFragmentDirections
                    .actionNavigationTransactionsToEditTransactionFragment(transaction)
                findNavController().navigate(action)
            },
            onDeleteClick = { transaction ->
                showDeleteConfirmationDialog(transaction)
            }
        )
        binding.recyclerViewTransactions.adapter = adapter
        binding.recyclerViewTransactions.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupClickListeners() {
        binding.fabAddTransaction.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_transactions_to_addTransactionFragment)
        }
    }

    private fun observeViewModel() {
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            adapter.submitList(transactions)
        }
    }

    private fun showDeleteConfirmationDialog(transaction: Transaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTransaction(transaction)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun backupToInternalStorage(context: Context): Boolean {
        try {
            // Get the transactions as a JSON string using PreferenceManager
            val transactions = preferenceManager.getTransactions()
            val transactionsJson = Gson().toJson(transactions)

            context.openFileOutput("transactions_backup.json", Context.MODE_PRIVATE).use {
                it.write(transactionsJson.toByteArray())
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun restoreFromInternalStorage(context: Context): Boolean {
        try {
            context.openFileInput("transactions_backup.json").use { inputStream ->
                val transactionsJson = inputStream.bufferedReader().use { it.readText() }
                // Convert JSON string back to List<Transaction>
                val type = object : TypeToken<List<Transaction>>() {}.type
                val transactions: List<Transaction> = Gson().fromJson(transactionsJson, type) ?: emptyList()
                // Save the transactions using PreferenceManager
                preferenceManager.saveTransactions(transactions)
                // Reload transactions in ViewModel to update UI
                viewModel.loadTransactions()
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
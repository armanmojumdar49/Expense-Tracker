package com.digitalproductbuy.expensetracker

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Transaction(
    val id: Long,
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    val date: String
)

class MainActivity : Activity() {
    private val transactions = mutableListOf<Transaction>()
    private lateinit var listLayout: LinearLayout
    private lateinit var summary: TextView
    private val prefs by lazy { getSharedPreferences("expense_tracker", MODE_PRIVATE) }
    private val green = Color.rgb(19, 126, 88)
    private val red = Color.rgb(190, 54, 54)
    private val ink = Color.rgb(30, 41, 59)
    private val muted = Color.rgb(100, 116, 139)
    private val bg = Color.rgb(246, 248, 251)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadTransactions()
        render()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun render() {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }
        scroll.addView(page)
        setContentView(scroll)

        page.addView(TextView(this).apply {
            text = "Expense Tracker"
            textSize = 27f
            setTextColor(ink)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        page.addView(TextView(this).apply {
            text = "Keep your money organized"
            textSize = 14f
            setTextColor(muted)
            setPadding(0, dp(4), 0, dp(18))
        })

        val summaryCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            setBackgroundColor(Color.WHITE)
            elevation = dp(2).toFloat()
        }
        summary = TextView(this).apply { textSize = 15f; setTextColor(ink) }
        summaryCard.addView(summary)
        page.addView(summaryCard, matchWidth())
        updateSummary()

        val addButton = Button(this).apply {
            text = "+  Add transaction"
            setTextColor(Color.WHITE)
            setBackgroundColor(green)
            setOnClickListener { showAddDialog() }
        }
        val btnParams = matchWidth().apply { topMargin = dp(16); bottomMargin = dp(16) }
        page.addView(addButton, btnParams)

        page.addView(TextView(this).apply {
            text = "Recent transactions"
            textSize = 19f
            setTextColor(ink)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, dp(4), 0, dp(10))
        })
        listLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(listLayout, matchWidth())
        refreshList()
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun updateSummary() {
        val income = transactions.filter { it.type == "Income" }.sumOf { it.amount }
        val expenses = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
        val balance = income - expenses
        if (::summary.isInitialized) {
            summary.text = "TOTAL BALANCE\n${money(balance)}\n\nIncome: ${money(income)}     Expenses: ${money(expenses)}"
            summary.textSize = 16f
            summary.setLineSpacing(dp(3).toFloat(), 1f)
        }
    }

    private fun money(value: Double) = "৳" + String.format(Locale.US, "%,.2f", value)

    private fun refreshList() {
        if (!::listLayout.isInitialized) return
        listLayout.removeAllViews()
        if (transactions.isEmpty()) {
            listLayout.addView(TextView(this).apply {
                text = "No transactions yet.\nTap “Add transaction” to record your first entry."
                textSize = 14f
                setTextColor(muted)
                gravity = Gravity.CENTER
                setPadding(dp(18), dp(28), dp(18), dp(28))
                setBackgroundColor(Color.WHITE)
            })
            return
        }
        transactions.sortedByDescending { it.id }.forEach { item ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(15), dp(13), dp(15), dp(13))
                setBackgroundColor(Color.WHITE)
                elevation = dp(1).toFloat()
            }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            left.addView(TextView(this).apply {
                text = item.title
                textSize = 16f
                setTextColor(ink)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            left.addView(TextView(this).apply {
                text = "${item.category} • ${item.date}"
                textSize = 12f
                setTextColor(muted)
                setPadding(0, dp(4), 0, 0)
            })
            row.addView(left, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(TextView(this).apply {
                text = (if (item.type == "Income") "+" else "−") + money(item.amount)
                textSize = 15f
                setTextColor(if (item.type == "Income") green else red)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            card.addView(row)
            card.setOnLongClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Delete transaction?")
                    .setMessage("Remove “${item.title}” from your records?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        transactions.removeAll { it.id == item.id }
                        saveTransactions()
                        updateSummary()
                        refreshList()
                    }.show()
                true
            }
            val params = matchWidth().apply { bottomMargin = dp(9) }
            listLayout.addView(card, params)
        }
    }

    private fun showAddDialog() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(8), dp(22), 0)
        }
        val titleInput = EditText(this).apply { hint = "Description (e.g. Groceries)" }
        panel.addView(titleInput)
        val amountInput = EditText(this).apply {
            hint = "Amount"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        panel.addView(amountInput)
        val typeSpinner = Spinner(this)
        typeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Expense", "Income"))
        panel.addView(typeSpinner)
        val categories = listOf("Food", "Transport", "Shopping", "Bills", "Health", "Education", "Salary", "Other")
        val categorySpinner = Spinner(this)
        categorySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        panel.addView(categorySpinner)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add transaction")
            .setView(panel)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = titleInput.text.toString().trim()
                val amount = amountInput.text.toString().trim().toDoubleOrNull()
                if (title.isBlank()) {
                    titleInput.error = "Enter a description"
                    return@setOnClickListener
                }
                if (amount == null || amount <= 0.0) {
                    amountInput.error = "Enter an amount greater than zero"
                    return@setOnClickListener
                }
                val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                transactions.add(Transaction(
                    System.currentTimeMillis(), title, amount,
                    typeSpinner.selectedItem.toString(),
                    categorySpinner.selectedItem.toString(), date
                ))
                saveTransactions()
                updateSummary()
                refreshList()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun saveTransactions() {
        val array = JSONArray()
        transactions.forEach {
            array.put(JSONObject().apply {
                put("id", it.id); put("title", it.title); put("amount", it.amount)
                put("type", it.type); put("category", it.category); put("date", it.date)
            })
        }
        prefs.edit().putString("transactions", array.toString()).apply()
    }

    private fun loadTransactions() {
        val raw = prefs.getString("transactions", "[]") ?: "[]"
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                transactions.add(Transaction(
                    obj.optLong("id"), obj.optString("title"),
                    obj.optDouble("amount"), obj.optString("type", "Expense"),
                    obj.optString("category", "Other"), obj.optString("date")
                ))
            }
        } catch (_: Exception) {
            transactions.clear()
        }
    }
}

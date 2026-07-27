package com.najmi.sciuro.core.ui.util

fun formatDecimalFirstInput(rawInput: String): String {
    val digitsOnly = rawInput.filter { it.isDigit() }
    if (digitsOnly.isEmpty()) return "0.00"
    val cents = digitsOnly.toBigDecimalOrNull() ?: return "0.00"
    return "%.2f".format(cents.divide(100.toBigDecimal()))
}

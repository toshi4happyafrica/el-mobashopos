package jp.elektrolibera.mobashopos.domain

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** 金額表示と入力の変換。金額は常に「最小単位の整数」で扱う */
object MoneyFormat {

    /** 例：1240 JPY → "¥1,240"、12345 USD(minor=2) → "$123.45" */
    fun format(amount: Long, currency: String, minorUnit: Int, locale: Locale = Locale.JAPAN): String {
        val value = BigDecimal.valueOf(amount).movePointLeft(minorUnit)
        val nf = NumberFormat.getCurrencyInstance(locale)
        runCatching { nf.currency = Currency.getInstance(currency) }
        nf.minimumFractionDigits = minorUnit
        nf.maximumFractionDigits = minorUnit
        return nf.format(value)
    }

    /**
     * 入力文字列を最小単位の整数に変換する。不正・負数・桁あふれは null。
     * 例："1,000" JPY → 1000、"12.5" USD(minor=2) → 1250
     */
    fun parse(input: String, minorUnit: Int): Long? {
        val cleaned = input.trim().replace(",", "").replace("，", "")
        if (cleaned.isEmpty()) return null
        return try {
            val value = BigDecimal(cleaned)
            if (value.signum() < 0) return null
            value.movePointRight(minorUnit).longValueExact()
        } catch (e: NumberFormatException) {
            null
        } catch (e: ArithmeticException) {
            null
        }
    }
}

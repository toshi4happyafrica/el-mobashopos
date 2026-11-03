package jp.elektrolibera.mobashopos.domain

/** 注文入力中の明細（1メニュー1行）。表示用の合計は目安で、確定額はサーバーが計算する */
data class CartLine(
    val menuItemId: String,
    val name: String,
    val unitPrice: Long,
    val quantity: Int,
) {
    val lineTotal: Long get() = unitPrice * quantity
}

data class OrderCart(val lines: List<CartLine> = emptyList()) {

    val totalQuantity: Int get() = lines.sumOf { it.quantity }
    val totalAmount: Long get() = lines.sumOf { it.lineTotal }
    val isEmpty: Boolean get() = lines.isEmpty()

    fun quantityOf(menuItemId: String): Int = lines.firstOrNull { it.menuItemId == menuItemId }?.quantity ?: 0

    /** 数量を delta だけ増減する（0 以下で行を削除、上限 MAX_QUANTITY） */
    fun change(menuItemId: String, name: String, unitPrice: Long, delta: Int): OrderCart {
        val current = quantityOf(menuItemId)
        val next = (current + delta).coerceIn(0, MAX_QUANTITY)
        val others = lines.filterNot { it.menuItemId == menuItemId }
        if (next == 0) return copy(lines = others)
        val updated = CartLine(menuItemId, name, unitPrice, next)
        val index = lines.indexOfFirst { it.menuItemId == menuItemId }
        return if (index < 0) {
            copy(lines = lines + updated)
        } else {
            copy(lines = lines.toMutableList().also { it[index] = updated })
        }
    }

    companion object {
        /** サーバーの上限（1明細の数量 1〜99）に合わせる */
        const val MAX_QUANTITY = 99
    }
}

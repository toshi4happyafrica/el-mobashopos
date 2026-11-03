package jp.elektrolibera.mobashopos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderCartTest {

    @Test
    fun add_and_remove_lines() {
        var cart = OrderCart()
        cart = cart.change("beer", "生ビール", 380, +1)
        cart = cart.change("beer", "生ビール", 380, +1)
        cart = cart.change("ramen", "ラーメン", 480, +1)
        assertEquals(3, cart.totalQuantity)
        assertEquals(380L * 2 + 480L, cart.totalAmount)

        cart = cart.change("beer", "生ビール", 380, -2)
        assertEquals(listOf("ramen"), cart.lines.map { it.menuItemId })
    }

    @Test
    fun quantity_is_capped() {
        var cart = OrderCart()
        repeat(120) { cart = cart.change("beer", "生ビール", 380, +1) }
        assertEquals(OrderCart.MAX_QUANTITY, cart.quantityOf("beer"))
        cart = cart.change("beer", "生ビール", 380, -200)
        assertTrue(cart.isEmpty)
    }

    @Test
    fun keeps_line_order() {
        var cart = OrderCart()
        cart = cart.change("a", "A", 100, 1)
        cart = cart.change("b", "B", 100, 1)
        cart = cart.change("a", "A", 100, 1)
        assertEquals(listOf("a", "b"), cart.lines.map { it.menuItemId })
    }
}

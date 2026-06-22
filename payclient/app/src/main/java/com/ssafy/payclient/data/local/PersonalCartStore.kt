package com.ssafy.payclient.data.local

import com.ssafy.payclient.data.model.MenuDTO

data class PersonalCartItem(
    val menuId: Long,
    val menuName: String,
    val price: Long,
    val quantity: Int
)

object PersonalCartStore {
    private val cartItems = linkedMapOf<Long, PersonalCartItem>()

    fun add(menu: MenuDTO, quantity: Int = 1) {
        val currentItem = cartItems[menu.menuId]
        cartItems[menu.menuId] = if (currentItem == null) {
            PersonalCartItem(
                menuId = menu.menuId,
                menuName = menu.menuName,
                price = menu.price,
                quantity = quantity
            )
        } else {
            currentItem.copy(quantity = currentItem.quantity + quantity)
        }
    }

    fun increase(menuId: Long) {
        val currentItem = cartItems[menuId] ?: return
        cartItems[menuId] = currentItem.copy(quantity = currentItem.quantity + 1)
    }

    fun decrease(menuId: Long) {
        val currentItem = cartItems[menuId] ?: return
        if (currentItem.quantity <= 1) {
            cartItems.remove(menuId)
        } else {
            cartItems[menuId] = currentItem.copy(quantity = currentItem.quantity - 1)
        }
    }

    fun remove(menuId: Long) {
        cartItems.remove(menuId)
    }

    fun clear() {
        cartItems.clear()
    }

    fun getItems(): List<PersonalCartItem> = cartItems.values.toList()

    fun getTotalPrice(): Long = cartItems.values.sumOf { it.price * it.quantity }
}

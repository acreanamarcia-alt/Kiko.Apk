package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppProductTest {

    @Test
    fun appProduct_convenienceConstructor_populatesNameNicheDescriptionPriceAndImageUrl() {
        val product = AppProduct(
            name = "App Delivery Sanduíches",
            niche = "Lanchonete",
            description = "Cardápio digital completo com checkout PIX e rastreamento.",
            price = 1490.0,
            imageUrl = "https://cdn.example.com/covers/lanchonete.jpg"
        )

        assertEquals("App Delivery Sanduíches", product.name)
        assertEquals("Lanchonete", product.niche)
        assertEquals("Cardápio digital completo com checkout PIX e rastreamento.", product.description)
        assertEquals(1490.0, product.price, 0.001)
        assertEquals("https://cdn.example.com/covers/lanchonete.jpg", product.imageUrl)
    }

    @Test
    fun appProduct_defaultCoverFallback_usesNicheKeyWhenImageUrlIsNull() {
        val product = AppProduct(
            name = "BarberClub Pro",
            niche = "Barbearia",
            description = "Agendamento rápido e clube de fidelidade.",
            price = 1390.0,
            imageUrl = null
        )

        assertEquals("barbearia", product.imageUrl)
    }
}

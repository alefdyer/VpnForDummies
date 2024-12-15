package com.asinosoft.vpn

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.asinosoft.vpn.dto.Subscription
import com.asinosoft.vpn.model.SubscriptionUiState
import com.asinosoft.vpn.ui.SubscriptionView
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class SubscriptionViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun startStateTest() {
        val clicked = mutableListOf<Subscription.Period>()
        composeTestRule.setContent {
            SubscriptionView(
                SubscriptionUiState.SelectSubscription,
                onCreateOrder = { clicked.add(it) }
            )
        }

        composeTestRule.onNodeWithText("На сутки").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("На неделю").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("На месяц").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("На год").assertIsDisplayed().performClick()

        Assert.assertEquals(Subscription.Period.entries, clicked)
    }
}

package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Reusable ad slot hook kept intentionally invisible while testing.
 * Set [AD_SLOT_ENABLED] to true and insert your AdMob/Ad SDK banner composable inside [AdBannerSlot]
 * once app testing is complete.
 */
object AdSlotConfig {
    const val AD_SLOT_ENABLED = false
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun AdBannerSlot(
    placementId: String,
    modifier: Modifier = Modifier
) {
    if (!AdSlotConfig.AD_SLOT_ENABLED) {
        return
    }
    // Insert ad banner composable here after testing
}

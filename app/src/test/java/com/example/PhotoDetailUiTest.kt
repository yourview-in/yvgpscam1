package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.PhotoRepository
import com.example.data.local.StampPreferencesRepository
import com.example.data.model.CapturedPhotoEntity
import com.example.map.MiniMapTileRenderer
import com.example.map.OfflineMapRepository
import com.example.ui.screens.PhotoDetailScreen
import com.example.ui.theme.YvGpsCamTheme
import com.example.ui.viewmodel.GalleryViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhotoDetailUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `photo detail hides telemetry until Details clicked and supports next photo paging`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val photoRepo = PhotoRepository(context, db.capturedPhotoDao())
        val offlineRepo = OfflineMapRepository(context, db.offlineRegionDao())
        val miniMapRenderer = MiniMapTileRenderer(offlineRepo)
        val stampPrefs = StampPreferencesRepository(context)

        val insertedIds = runBlocking {
            val id1 = photoRepo.insertPhoto(
                CapturedPhotoEntity(
                    mediaUri = "file:///tmp/photo_1.jpg",
                    displayName = "YV_First_1.jpg",
                    capturedAtMillis = 1763141040000L,
                    latitude = 17.385044,
                    longitude = 78.486671,
                    altitudeMeters = 505.0,
                    accuracyMeters = 3.5f,
                    compassAzimuth = 90f,
                    compassCardinal = "E",
                    addressLine = "100 Park Ave",
                    cityState = "Hyderabad",
                    country = "India",
                    customNote = "First capture",
                    templateName = "ADVANCE",
                    width = 1080,
                    height = 1440
                )
            )
            val id2 = photoRepo.insertPhoto(
                CapturedPhotoEntity(
                    mediaUri = "file:///tmp/photo_2.jpg",
                    displayName = "YV_Second_2.jpg",
                    capturedAtMillis = 1763142040000L,
                    latitude = 17.440081,
                    longitude = 78.348915,
                    altitudeMeters = 520.0,
                    accuracyMeters = 4.0f,
                    compassAzimuth = 180f,
                    compassCardinal = "S",
                    addressLine = "200 Tech Rd",
                    cityState = "Gachibowli",
                    country = "India",
                    customNote = "Second capture",
                    templateName = "ADVANCE",
                    width = 1080,
                    height = 1440
                )
            )
            listOf(id1, id2)
        }

        val galleryVm = GalleryViewModel(
            appContext = context,
            photoRepository = photoRepo,
            miniMapRenderer = miniMapRenderer,
            stampPrefs = stampPrefs
        )

        composeRule.setContent {
            YvGpsCamTheme {
                PhotoDetailScreen(
                    photoId = insertedIds[1], // newest first in Gallery -> page 0
                    galleryViewModel = galleryVm,
                    offlineOnlyMapMode = true,
                    onBack = {}
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("detail_next_photo_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()

        // Photo pager and Details button are displayed; info below photo is hidden initially
        composeRule.onNodeWithTag("photo_detail_horizontal_pager").assertIsDisplayed()
        composeRule.onNodeWithTag("detail_toggle_details_button").assertIsDisplayed()
        composeRule.onNodeWithTag("photo_detail_expanded_info").assertDoesNotExist()
        composeRule.onNodeWithText("YV_Second_2.jpg").assertIsDisplayed()

        // Side scroll / advance to next photo works cleanly while details remain hidden
        composeRule.onNodeWithTag("detail_next_photo_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("YV_First_1.jpg").assertIsDisplayed()
        composeRule.onNodeWithTag("photo_detail_expanded_info").assertDoesNotExist()

        // Clicking Details reveals GPS Location Telemetry & EXIF details below photo
        composeRule.onNodeWithTag("detail_toggle_details_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("photo_detail_expanded_info").assertIsDisplayed()
        composeRule.onNodeWithText("GPS Location Telemetry").assertIsDisplayed()

        // Clicking Details again hides info cleanly
        composeRule.onNodeWithTag("detail_toggle_details_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("photo_detail_expanded_info").assertDoesNotExist()
    }
}

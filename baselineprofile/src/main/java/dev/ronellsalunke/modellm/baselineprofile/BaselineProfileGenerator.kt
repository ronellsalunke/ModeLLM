package dev.ronellsalunke.modellm.baselineprofile

import android.widget.EditText
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PackageName = "dev.ronellsalunke.modellm"
private const val UiTimeoutMs = 10_000L

/**
 * Generates the Baseline Profile from two journeys: cold startup and the core browsing flows.
 *
 * The journeys avoid catalog content, so the profile is the same with or without a completed
 * sync. Run `gradlew :app:generateBaselineProfile` with a connected device.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = PackageName,
        includeInStartupProfile = true,
    ) {
        startActivityAndWait()
    }

    @Test
    fun browseCatalog() = baselineProfileRule.collect(packageName = PackageName) {
        startActivityAndWait()

        // Search. Compose exposes the editable node as an EditText, apart from its label node.
        click(By.clazz(EditText::class.java))
        find(By.clazz(EditText::class.java)).text = "gpt"
        device.waitForIdle()
        click(By.desc("Clear search"))

        // Filters sheet.
        click(By.desc("Filters"))
        click(By.text("Done"))

        // Settings, then back to the catalog.
        click(By.desc("Settings"))
        find(By.desc("Back"))
        device.pressBack()
        device.waitForIdle()

        // Every tab, ending on the default page.
        click(By.text("Providers"))
        click(By.text("Labs"))
        click(By.text("Models"))
    }
}

private fun MacrobenchmarkScope.find(selector: BySelector): UiObject2 =
    device.wait(Until.findObject(selector), UiTimeoutMs)
        ?: error("No UI element for $selector")

private fun MacrobenchmarkScope.click(selector: BySelector) {
    find(selector).click()
    device.waitForIdle()
}

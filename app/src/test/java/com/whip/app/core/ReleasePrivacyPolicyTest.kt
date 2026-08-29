package com.whip.app.core

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class ReleasePrivacyPolicyTest {
    @Test
    fun applicationDisablesPlatformBackupAndCleartextTraffic() {
        val application = manifest().getElementsByTagName("application").item(0) as Element

        assertEquals("false", application.androidAttribute("allowBackup"))
        assertEquals("false", application.androidAttribute("usesCleartextTraffic"))
        assertEquals("@xml/backup_rules", application.androidAttribute("fullBackupContent"))
        assertEquals("@xml/data_extraction_rules", application.androidAttribute("dataExtractionRules"))
    }

    @Test
    fun manifestRequestsOnlyTheDocumentedNotificationBootAndReadOnlyHealthPermissions() {
        val permissionNodes = manifest().getElementsByTagName("uses-permission")
        val permissions = (0 until permissionNodes.length)
            .map { (permissionNodes.item(it) as Element).androidAttribute("name") }
            .toSet()

        assertEquals(
            setOf(
                "android.permission.POST_NOTIFICATIONS",
                "android.permission.RECEIVE_BOOT_COMPLETED",
                "android.permission.health.READ_WEIGHT",
                "android.permission.health.READ_STEPS",
                "android.permission.health.READ_DISTANCE",
                "android.permission.health.READ_HYDRATION",
                "android.permission.health.READ_SLEEP",
                "android.permission.health.READ_EXERCISE",
            ),
            permissions,
        )
        assertFalse(permissions.any { permission ->
            permission.contains("LOCATION") || permission.contains("STORAGE") ||
                permission == "android.permission.INTERNET" || permission.contains("health.WRITE_")
        })
    }

    @Test
    fun exportedComponentsAreLimitedAndSensitiveReceiversRemainPrivate() {
        val document = manifest()
        fun exported(tag: String): Set<String> {
            val nodes = document.getElementsByTagName(tag)
            return (0 until nodes.length)
                .map { nodes.item(it) as Element }
                .filter { it.androidAttribute("exported") == "true" }
                .map { it.androidAttribute("name") }
                .toSet()
        }

        assertEquals(
            setOf(".MainActivity", ".widget.WhipWidgetConfigureActivity", ".health.HealthPermissionsRationaleActivity"),
            exported("activity"),
        )
        assertEquals(setOf(".HealthPermissionUsageActivity"), exported("activity-alias"))
        assertEquals(
            setOf(".widget.WhipWidgetProvider", ".widget.HabitTrackingWidgetProvider"),
            exported("receiver"),
        )
        assertTrue(exported("service").isEmpty())

        val receiverNodes = document.getElementsByTagName("receiver")
        val receivers = (0 until receiverNodes.length).associate {
            val element = receiverNodes.item(it) as Element
            element.androidAttribute("name") to element
        }
        listOf(
            ".reminders.ReminderActionReceiver",
            ".reminders.HabitReminderActionReceiver",
            ".reminders.GoalReminderActionReceiver",
        ).forEach { name -> assertEquals("false", receivers.getValue(name).androidAttribute("exported")) }
        assertEquals(
            "android.permission.BIND_APPWIDGET",
            receivers.getValue(".widget.WhipWidgetProvider").androidAttribute("permission"),
        )
        assertEquals(
            "android.permission.BIND_APPWIDGET",
            receivers.getValue(".widget.HabitTrackingWidgetProvider").androidAttribute("permission"),
        )
        val serviceNodes = document.getElementsByTagName("service")
        val services = (0 until serviceNodes.length).associate {
            val element = serviceNodes.item(it) as Element
            element.androidAttribute("name") to element
        }
        services.getValue(".widget.HabitWidgetRemoteViewsService").let { service ->
            assertEquals("false", service.androidAttribute("exported"))
            assertEquals("android.permission.BIND_REMOTEVIEWS", service.androidAttribute("permission"))
        }
        services.getValue(".widget.TaskWidgetRemoteViewsService").let { service ->
            assertEquals("false", service.androidAttribute("exported"))
            assertEquals("android.permission.BIND_REMOTEVIEWS", service.androidAttribute("permission"))
        }
    }

    @Test
    fun backupAndDeviceTransferRulesExcludeEveryPrivateStorageDomain() {
        assertEquals(
            setOf("root", "file", "database", "sharedpref", "external"),
            exclusionDomains(xml("app/src/main/res/xml/backup_rules.xml").documentElement),
        )
        val extraction = xml("app/src/main/res/xml/data_extraction_rules.xml")
        val required = setOf(
            "root", "file", "database", "sharedpref", "external",
            "device_root", "device_file", "device_database", "device_sharedpref",
        )
        listOf("cloud-backup", "device-transfer").forEach { section ->
            val element = extraction.getElementsByTagName(section).item(0) as Element
            assertEquals(required, exclusionDomains(element))
        }
    }

    private fun exclusionDomains(parent: org.w3c.dom.Node): Set<String> {
        val children = parent.childNodes
        val exclusions = (0 until children.length)
            .map { children.item(it) }
            .filterIsInstance<Element>()
            .filter { it.tagName == "exclude" }
        assertTrue(exclusions.all { it.getAttribute("path") == "." })
        return exclusions.map { it.getAttribute("domain") }.toSet()
    }

    private fun manifest() = xml("app/src/main/AndroidManifest.xml")

    private fun xml(path: String) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }.newDocumentBuilder().parse(rootFile(path))

    private fun rootFile(path: String): File {
        val root = sequenceOf(File("."), File(".."))
            .first { File(it, "app/src/main/AndroidManifest.xml").isFile }
        return File(root, path)
    }

    private fun Element.androidAttribute(name: String): String =
        getAttributeNS("http://schemas.android.com/apk/res/android", name)
}

package com.kidsnfcplaypos.util

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LocalizationConsistencyTest {

    private val baseStringsPath = "app/src/main/res/values/strings.xml"
    private val localizedPaths = listOf(
        "app/src/main/res/values-es/strings.xml",
        "app/src/main/res/values-ca/strings.xml",
        "app/src/main/res/values-nl/strings.xml"
    )

    @Test
    fun `all localized files should contain all keys from base strings`() {
        val baseKeys = extractKeys(baseStringsPath)
        
        localizedPaths.forEach { path ->
            val localizedKeys = extractKeys(path)
            val missingKeys = baseKeys - localizedKeys
            
            assertTrue(
                "File $path is missing keys: $missingKeys",
                missingKeys.isEmpty()
            )
        }
    }

    @Test
    fun `localized files should not contain extra keys not present in base strings`() {
        val baseKeys = extractKeys(baseStringsPath)
        
        localizedPaths.forEach { path ->
            val localizedKeys = extractKeys(path)
            val extraKeys = localizedKeys - baseKeys
            
            assertTrue(
                "File $path has extra keys not in base: $extraKeys",
                extraKeys.isEmpty()
            )
        }
    }

    @Test
    fun `format arguments should be consistent across all translations`() {
        val baseStrings = extractKeyToValue(baseStringsPath)
        
        localizedPaths.forEach { path ->
            val localizedStrings = extractKeyToValue(path)
            
            baseStrings.forEach { (key, baseValue) ->
                val localizedValue = localizedStrings[key]
                if (localizedValue != null) {
                    val baseArgs = extractFormatArgs(baseValue)
                    val localizedArgs = extractFormatArgs(localizedValue)
                    
                    assertTrue(
                        "Format arguments mismatch for key '$key' in $path. Expected $baseArgs but got $localizedArgs",
                        baseArgs == localizedArgs
                    )
                }
            }
        }
    }

    private fun extractKeys(filePath: String): Set<String> {
        return extractKeyToValue(filePath).keys
    }

    private fun extractKeyToValue(filePath: String): Map<String, String> {
        val file = File(filePath)
        if (!file.exists()) return emptyMap()

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(file)
        val stringNodes = doc.getElementsByTagName("string")
        
        val map = mutableMapOf<String, String>()
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            val key = node.attributes.getNamedItem("name")?.nodeValue
            val value = node.textContent
            if (key != null) {
                map[key] = value
            }
        }
        return map
    }

    private fun extractFormatArgs(text: String): List<String> {
        val regex = "%(\\d+\\$)?[-#+ 0,(]*\\d*(\\.\\d+)?[a-zA-Z]".toRegex()
        return regex.findAll(text).map { it.value }.toList()
    }
}

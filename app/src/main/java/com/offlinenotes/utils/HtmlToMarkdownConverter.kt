package com.offlinenotes.utils

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

class HtmlToMarkdownConverter {

    fun convert(html: String): String {
        return try {
            Logger.d("Converting HTML to Markdown: ${html.length} characters")
            val doc = Jsoup.parse(html, "UTF-8")
            
            // Удаляем ненужные элементы
            doc.select("script, style, nav, footer, header, aside").remove()
            
            // Находим основной контент
            val content = findMainContent(doc) ?: doc.body()
            
            val markdown = StringBuilder()
            convertNode(content, markdown)
            
            val result = markdown.toString().trim()
            Logger.d("Converted to Markdown: ${result.length} characters")
            result
            
        } catch (e: Exception) {
            Logger.e("Failed to convert HTML to Markdown", e)
            "Error converting page content"
        }
    }

    private fun findMainContent(doc: Document): Element? {
        // Ищем основные контейнеры контента
        val selectors = listOf(
            "main",
            "article", 
            "[role='main']",
            ".content",
            ".main-content",
            ".post-content",
            ".entry-content",
            ".article-content",
            "#content",
            "#main",
            "#article",
            ".post",
            ".entry"
        )
        
        for (selector in selectors) {
            val element = doc.select(selector).first()
            if (element != null) {
                Logger.d("Found main content with selector: $selector")
                return element
            }
        }
        
        Logger.d("No main content found, using body")
        return null
    }

    private fun convertNode(node: Node, markdown: StringBuilder) {
        when (node) {
            is TextNode -> {
                val text = node.text().trim()
                if (text.isNotEmpty()) {
                    markdown.append(text).append(" ")
                }
            }
            is Element -> {
                when (node.tagName()) {
                    "h1" -> {
                        markdown.append("\n# ${node.text()}\n\n")
                    }
                    "h2" -> {
                        markdown.append("\n## ${node.text()}\n\n")
                    }
                    "h3" -> {
                        markdown.append("\n### ${node.text()}\n\n")
                    }
                    "h4" -> {
                        markdown.append("\n#### ${node.text()}\n\n")
                    }
                    "h5" -> {
                        markdown.append("\n##### ${node.text()}\n\n")
                    }
                    "h6" -> {
                        markdown.append("\n###### ${node.text()}\n\n")
                    }
                    "p" -> {
                        convertChildren(node, markdown)
                        markdown.append("\n\n")
                    }
                    "br" -> {
                        markdown.append("\n")
                    }
                    "strong", "b" -> {
                        markdown.append("**")
                        convertChildren(node, markdown)
                        markdown.append("**")
                    }
                    "em", "i" -> {
                        markdown.append("*")
                        convertChildren(node, markdown)
                        markdown.append("*")
                    }
                    "a" -> {
                        val href = node.attr("href")
                        val text = node.text()
                        if (href.isNotEmpty()) {
                            markdown.append("[$text]($href)")
                        } else {
                            markdown.append(text)
                        }
                    }
                    "ul" -> {
                        convertChildren(node, markdown)
                        markdown.append("\n")
                    }
                    "ol" -> {
                        convertChildren(node, markdown)
                        markdown.append("\n")
                    }
                    "li" -> {
                        val parent = node.parent()
                        if (parent?.tagName() == "ul") {
                            markdown.append("- ")
                        } else if (parent?.tagName() == "ol") {
                            val index = parent.children().indexOf(node) + 1
                            markdown.append("$index. ")
                        }
                        convertChildren(node, markdown)
                        markdown.append("\n")
                    }
                    "blockquote" -> {
                        markdown.append("> ")
                        convertChildren(node, markdown)
                        markdown.append("\n\n")
                    }
                    "code" -> {
                        markdown.append("`")
                        convertChildren(node, markdown)
                        markdown.append("`")
                    }
                    "pre" -> {
                        markdown.append("\n```\n")
                        convertChildren(node, markdown)
                        markdown.append("\n```\n\n")
                    }
                    "img" -> {
                        val src = node.attr("src")
                        val alt = node.attr("alt")
                        if (src.isNotEmpty()) {
                            markdown.append("![$alt]($src)")
                        }
                    }
                    "table" -> {
                        markdown.append("\n")
                        convertTable(node, markdown)
                        markdown.append("\n")
                    }
                    "tr" -> {
                        // Обрабатывается в convertTable
                    }
                    "td", "th" -> {
                        // Обрабатывается в convertTable
                    }
                    "hr" -> {
                        markdown.append("\n---\n\n")
                    }
                    "div", "span", "section" -> {
                        convertChildren(node, markdown)
                    }
                    else -> {
                        convertChildren(node, markdown)
                    }
                }
            }
        }
    }

    private fun convertChildren(element: Element, markdown: StringBuilder) {
        for (child in element.childNodes()) {
            convertNode(child, markdown)
        }
    }

    private fun convertTable(table: Element, markdown: StringBuilder) {
        val rows = table.select("tr")
        if (rows.isEmpty()) return

        // Обрабатываем заголовки (первая строка)
        val headerRow = rows.firstOrNull() ?: return
        val headers = headerRow.select("th, td")
        if (headers.isNotEmpty()) {
            val headerTexts = headers.map { it.text().trim() }
            markdown.append("| ${headerTexts.joinToString(" | ")} |\n")
            markdown.append("| ${headerTexts.map { "---" }.joinToString(" | ")} |\n")
        }

        // Обрабатываем остальные строки
        for (i in 1 until rows.size) {
            val row = rows[i]
            val cells = row.select("td, th")
            if (cells.isNotEmpty()) {
                val cellTexts = cells.map { it.text().trim() }
                markdown.append("| ${cellTexts.joinToString(" | ")} |\n")
            }
        }
    }
}

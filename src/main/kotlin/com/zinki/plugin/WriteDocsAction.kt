package com.zinki.plugin

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import java.awt.GridLayout
import javax.swing.*

class WriteDocsAction : ZinkiBaseAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val code = getSelectedCode(e) ?: run {
            com.intellij.openapi.ui.Messages.showWarningDialog(
                "No code selected. Select code before running Write Docs.",
                "Zinki for Claude"
            )
            return
        }

        val dialog = WriteDocsOptionsDialog()
        if (!dialog.showAndGet()) return

        val prompt = buildString {
            append("/write-docs\n\n")
            append("Code to document (style=${dialog.style}, audience=${dialog.audience}):\n")
            append("```go\n$code\n```")
        }

        runInTerminal(e, prompt)
    }
}

class WriteDocsOptionsDialog : DialogWrapper(true) {
    private val styleBox = JComboBox(arrayOf("markdown", "code-comment", "docstring")).apply {
        selectedItem = "markdown"
    }
    private val audienceBox = JComboBox(arrayOf("internal", "api-users")).apply {
        selectedItem = "internal"
    }

    val style: String get() = styleBox.selectedItem as String
    val audience: String get() = audienceBox.selectedItem as String

    init {
        title = "Zinki: Write Docs"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridLayout(2, 2, 8, 8))
        panel.add(JBLabel("Style:"))
        panel.add(styleBox)
        panel.add(JBLabel("Audience:"))
        panel.add(audienceBox)
        return panel
    }
}

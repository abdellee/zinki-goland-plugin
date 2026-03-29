package com.zinki.plugin

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import java.awt.GridLayout
import javax.swing.*

class ExplainCodeAction : ZinkiBaseAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val code = getSelectedCode(e) ?: run {
            com.intellij.openapi.ui.Messages.showWarningDialog(
                "No code selected. Select code before running Explain Code.",
                "Zinki for Claude"
            )
            return
        }

        val dialog = ExplainOptionsDialog()
        if (!dialog.showAndGet()) return

        val prompt = buildString {
            append("/explain-code\n\n")
            append(getFileContext(e))
            append("Code to explain (depth=${dialog.depth}, focus=${dialog.focus}):\n")
            append("```go\n$code\n```")
        }

        runInTerminal(e, prompt, "explain-code")
    }
}

class ExplainOptionsDialog : DialogWrapper(true) {
    private val depthBox = JComboBox(arrayOf("beginner", "intermediate", "advanced")).apply {
        selectedItem = "intermediate"
    }
    private val focusBox = JComboBox(arrayOf("logic", "architecture", "debugging", "performance")).apply {
        selectedItem = "logic"
    }

    val depth: String get() = depthBox.selectedItem as String
    val focus: String get() = focusBox.selectedItem as String

    init {
        title = "Zinki: Explain Code"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridLayout(2, 2, 8, 8))
        panel.add(JBLabel("Depth:"))
        panel.add(depthBox)
        panel.add(JBLabel("Focus:"))
        panel.add(focusBox)
        return panel
    }
}

package com.zinki.plugin

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import java.awt.GridLayout
import javax.swing.*

class SimplifyCodeAction : ZinkiBaseAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val code = getSelectedCode(e) ?: run {
            com.intellij.openapi.ui.Messages.showWarningDialog(
                "No code selected. Select code before running Simplify Code.",
                "Zinki for Claude"
            )
            return
        }

        val dialog = SimplifyOptionsDialog()
        if (!dialog.showAndGet()) return

        val prompt = buildString {
            append("/simplify-code\n\n")
            append(getFileContext(e))
            append("Code to simplify (level=${dialog.level}):\n")
            append("```go\n$code\n```")
        }

        runInTerminal(e, prompt, "simplify-code")
    }
}

class SimplifyOptionsDialog : DialogWrapper(true) {
    private val levelBox = JComboBox(arrayOf("conservative", "moderate", "aggressive")).apply {
        selectedItem = "moderate"
    }

    val level: String get() = levelBox.selectedItem as String

    init {
        title = "Zinki: Simplify Code"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridLayout(1, 2, 8, 8))
        panel.add(JBLabel("Level:"))
        panel.add(levelBox)
        return panel
    }
}

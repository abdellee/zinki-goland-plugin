package com.zinki.plugin

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import java.awt.GridLayout
import javax.swing.*

class ReviewCodeAction : ZinkiBaseAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val code = getSelectedCode(e) ?: run {
            com.intellij.openapi.ui.Messages.showWarningDialog(
                "No code selected. Select code before running Review Code.",
                "Zinki for Claude"
            )
            return
        }

        val dialog = ReviewOptionsDialog()
        if (!dialog.showAndGet()) return

        val prompt = buildString {
            append("/review-code\n\n")
            append(getFileContext(e))
            append("Code to review (mode=${dialog.mode}, focus=${dialog.focus}):\n")
            append("```go\n$code\n```")
        }

        runInTerminal(e, prompt, "review-code")
    }
}

class ReviewOptionsDialog : DialogWrapper(true) {
    private val modeBox = JComboBox(arrayOf("light", "standard", "strict")).apply {
        selectedItem = "standard"
    }
    private val focusBox = JComboBox(arrayOf("all", "security", "performance", "readability", "correctness", "concurrency")).apply {
        selectedItem = "all"
    }

    val mode: String get() = modeBox.selectedItem as String
    val focus: String get() = focusBox.selectedItem as String

    init {
        title = "Zinki: Review Code"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridLayout(2, 2, 8, 8))
        panel.add(JBLabel("Mode:"))
        panel.add(modeBox)
        panel.add(JBLabel("Focus:"))
        panel.add(focusBox)
        return panel
    }
}

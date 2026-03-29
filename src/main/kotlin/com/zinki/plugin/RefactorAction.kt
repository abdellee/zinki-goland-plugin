package com.zinki.plugin

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import java.awt.GridLayout
import javax.swing.*

class RefactorAction : ZinkiBaseAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val code = getSelectedCode(e) ?: run {
            com.intellij.openapi.ui.Messages.showWarningDialog(
                "No code selected. Select code before running Refactor.",
                "Zinki for Claude"
            )
            return
        }

        val dialog = RefactorOptionsDialog()
        if (!dialog.showAndGet()) return

        val prompt = buildString {
            append("/refactor\n\n")
            append("Code to refactor (goals=${dialog.goals}, risk=${dialog.risk}):\n")
            append("```go\n$code\n```")
        }

        runInTerminal(e, prompt)
    }
}

class RefactorOptionsDialog : DialogWrapper(true) {
    private val goalsBox = JComboBox(arrayOf("readability", "performance", "maintainability")).apply {
        selectedItem = "readability"
    }
    private val riskBox = JComboBox(arrayOf("conservative", "moderate", "aggressive")).apply {
        selectedItem = "moderate"
    }

    val goals: String get() = goalsBox.selectedItem as String
    val risk: String get() = riskBox.selectedItem as String

    init {
        title = "Zinki: Refactor"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridLayout(2, 2, 8, 8))
        panel.add(JBLabel("Goals:"))
        panel.add(goalsBox)
        panel.add(JBLabel("Risk:"))
        panel.add(riskBox)
        return panel
    }
}

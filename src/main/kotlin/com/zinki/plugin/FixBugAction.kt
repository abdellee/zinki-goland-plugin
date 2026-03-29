package com.zinki.plugin

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import java.awt.GridLayout
import javax.swing.*

class FixBugAction : ZinkiBaseAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val code = getSelectedCode(e) ?: run {
            com.intellij.openapi.ui.Messages.showWarningDialog(
                "No code selected. Select code before running Fix Bug.",
                "Zinki for Claude"
            )
            return
        }

        val dialog = FixBugOptionsDialog()
        if (!dialog.showAndGet()) return

        val prompt = buildString {
            append("/fix-bug\n\n")
            append("Code to fix (scope=${dialog.scope}, verify=${dialog.verify}):\n")
            append("```go\n$code\n```")
        }

        runInTerminal(e, prompt)
    }
}

class FixBugOptionsDialog : DialogWrapper(true) {
    private val scopeBox = JComboBox(arrayOf("minimal", "targeted", "comprehensive")).apply {
        selectedItem = "targeted"
    }
    private val verifyBox = JComboBox(arrayOf("yes", "no")).apply {
        selectedItem = "yes"
    }

    val scope: String get() = scopeBox.selectedItem as String
    val verify: String get() = verifyBox.selectedItem as String

    init {
        title = "Zinki: Fix Bug"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridLayout(2, 2, 8, 8))
        panel.add(JBLabel("Scope:"))
        panel.add(scopeBox)
        panel.add(JBLabel("Run go test:"))
        panel.add(verifyBox)
        return panel
    }
}

package com.zinki.plugin

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import java.awt.GridLayout
import javax.swing.*

class GenerateTestsAction : ZinkiBaseAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val code = getSelectedCode(e) ?: run {
            com.intellij.openapi.ui.Messages.showWarningDialog(
                "No code selected. Select code before running Generate Tests.",
                "Zinki for Claude"
            )
            return
        }

        val dialog = GenerateTestsOptionsDialog()
        if (!dialog.showAndGet()) return

        val prompt = buildString {
            append("/generate-tests\n\n")
            append("Code to test (coverage=${dialog.coverage}, intensity=${dialog.intensity}):\n")
            append("```go\n$code\n```")
        }

        runInTerminal(e, prompt)
    }
}

class GenerateTestsOptionsDialog : DialogWrapper(true) {
    private val coverageBox = JComboBox(arrayOf("unit", "integration")).apply {
        selectedItem = "unit"
    }
    private val intensityBox = JComboBox(arrayOf("minimal", "standard", "thorough")).apply {
        selectedItem = "standard"
    }

    val coverage: String get() = coverageBox.selectedItem as String
    val intensity: String get() = intensityBox.selectedItem as String

    init {
        title = "Zinki: Generate Tests"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridLayout(2, 2, 8, 8))
        panel.add(JBLabel("Coverage:"))
        panel.add(coverageBox)
        panel.add(JBLabel("Intensity:"))
        panel.add(intensityBox)
        return panel
    }
}

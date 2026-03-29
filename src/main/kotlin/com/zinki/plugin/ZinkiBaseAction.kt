package com.zinki.plugin

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.terminal.ui.TerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

abstract class ZinkiBaseAction : AnAction() {

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible =
            editor != null && editor.selectionModel.hasSelection()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    protected fun getSelectedCode(e: AnActionEvent): String? {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
        return editor.selectionModel.selectedText?.takeIf { it.isNotBlank() }
    }

    /** Returns "File: foo.go (package bar)\n\n" context line, or empty string if unavailable. */
    protected fun getFileContext(e: AnActionEvent): String {
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return ""
        val fileName = virtualFile.name
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        // Attempt Go package name via reflection to avoid hard dependency on GoLand PSI classes
        val packageName: String = try {
            psiFile?.let {
                val cls = Class.forName("com.goide.psi.GoFile")
                if (cls.isInstance(it)) cls.getMethod("getPackageName").invoke(it) as? String else null
            } ?: ""
        } catch (_: Exception) { "" }
        return if (packageName.isNotEmpty()) "File: $fileName (package $packageName)\n\n"
        else "File: $fileName\n\n"
    }

    protected fun runInTerminal(e: AnActionEvent, prompt: String, skillName: String = "output") {
        val project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)

        val terminalManager = TerminalToolWindowManager.getInstance(project)

        val existing: TerminalWidget? = terminalManager.terminalWidgets
            .firstOrNull { w: TerminalWidget ->
                w.terminalTitle.buildTitle() == TERMINAL_TITLE
            }

        if (existing != null && claudeRunning) {
            // Claude is already running interactively — send prompt directly as input
            existing.sendCommandToExecute(prompt)
            ApplicationManager.getApplication().invokeLater {
                showSaveMdNotification(project, virtualFile, skillName)
            }
            return
        }

        // Write prompt to a .txt file and a .ps1 wrapper script.
        // Terminal shows only the short script invocation, not the full prompt text.
        val promptFile = File.createTempFile("zinki-prompt-", ".txt")
        promptFile.writeText(prompt, Charsets.UTF_8)
        promptFile.deleteOnExit()
        val promptPath = promptFile.absolutePath.replace("\\", "/")

        val scriptFile = File.createTempFile("zinki-", ".ps1")
        scriptFile.deleteOnExit()
        scriptFile.writeText(
            "\$p = Get-Content -Raw '$promptPath'\n" +
            "Remove-Item '$promptPath' -ErrorAction SilentlyContinue\n" +
            "claude --dangerously-skip-permissions \$p\n",
            Charsets.UTF_8
        )
        val scriptPath = scriptFile.absolutePath.replace("\\", "/")

        val widget: TerminalWidget = existing
            ?: terminalManager.createShellWidget(project.basePath ?: "", TERMINAL_TITLE, true, true)

        // Send via TtyConnector + ANSI erase to hide the invocation line from view
        val tty = widget.ttyConnectorAccessor.ttyConnector
        if (tty != null) {
            tty.write("& '$scriptPath'\r")
            tty.write("\u001b[1A\u001b[2K")
        } else {
            widget.sendCommandToExecute("& '$scriptPath'")
        }
        claudeRunning = true

        // Show notification on EDT after a short delay to ensure plugin XML is loaded
        ApplicationManager.getApplication().invokeLater {
            showSaveMdNotification(project, virtualFile, skillName)
        }
    }

    private fun showSaveMdNotification(
        project: com.intellij.openapi.project.Project,
        virtualFile: com.intellij.openapi.vfs.VirtualFile?,
        skillName: String
    ) {
        val group = NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP) ?: return

        val notification = group.createNotification(
                "Zinki for Claude",
                "Claude is running <b>$skillName</b>. Save output as Markdown when done?",
                NotificationType.INFORMATION
            )

        notification.addAction(object : AnAction("Save as MD") {
            override fun actionPerformed(e: AnActionEvent) {
                notification.expire()
                saveOutputAsMd(project, virtualFile, skillName)
            }
        })

        notification.notify(project)
    }

    private fun saveOutputAsMd(
        project: com.intellij.openapi.project.Project,
        virtualFile: com.intellij.openapi.vfs.VirtualFile?,
        skillName: String
    ) {
        val dir = virtualFile?.parent?.path ?: project.basePath ?: return
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val mdFile = File("$dir/zinki-$skillName-$timestamp.md")
        mdFile.writeText(
            "# Zinki: ${skillName.replaceFirstChar { it.uppercase() }} — ${virtualFile?.name ?: ""}\n\n" +
            "_Generated by Zinki for Claude on ${timestamp}_\n\n" +
            "<!-- Paste Claude's output below -->\n\n",
            Charsets.UTF_8
        )
        // Refresh VFS so the file appears immediately in GoLand's project tree
        ApplicationManager.getApplication().invokeLater {
            com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                .refreshAndFindFileByPath(mdFile.absolutePath)
        }
        com.intellij.openapi.ui.Messages.showInfoMessage(
            "Saved: ${mdFile.name}\n\nPaste Claude's output from the terminal into this file.",
            "Zinki: Saved"
        )
    }

    companion object {
        private const val TERMINAL_TITLE = "Zinki for Claude"
        private const val NOTIFICATION_GROUP = "Zinki"
        private var claudeRunning = false
    }
}

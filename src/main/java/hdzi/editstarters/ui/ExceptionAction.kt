package hdzi.editstarters.ui.dialog

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages


abstract class ExceptionAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        try {
            invoke(e)
        } catch (ex: Exception) {
            Messages.showErrorDialog(
                """
                Please check the network connection and try again.
                If it fails again, please contact the author.
                Details:
                ${if (ex.javaClass === Exception::class.java) ex.message else ex}
            """.trimIndent(),
                "Edit Starters Error"
            )
        }
    }

    abstract fun invoke(e: AnActionEvent)
}

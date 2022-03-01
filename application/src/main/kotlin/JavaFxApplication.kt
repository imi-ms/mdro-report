package de.uni_muenster.imi.oegd.application

import de.uni_muenster.imi.oegd.baseX.*
import de.uni_muenster.imi.oegd.webapp.createServer
import io.ktor.server.netty.*
import javafx.application.Application
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.StackPane
import javafx.scene.web.WebView
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import java.io.File
import kotlin.system.exitProcess

/**
 * Classes that extend Application require javafx in the jdk.
 * By running the application with this workaround we can use
 * the modular javafx-plugin
 */
class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            JavaFxApplication.main(args)
        }
    }
}

class JavaFxApplication : Application() {
    private val webappPort = findOpenPortInRange(1024..49151)
    private var server: NettyApplicationEngine? = null
    private lateinit var directory: File

    override fun start(primaryStage: Stage) {
        val page =
            FXMLLoader.load<Parent>(javaClass.getResource("/start-dialog.fxml"))

        primaryStage.scene = Scene(page)
        primaryStage.show()

        (page.lookup("#button_file") as Button).onAction = EventHandler<ActionEvent> {
            val directoryChooser = DirectoryChooser()
            directoryChooser.title = "Wähle Verzeichnis mit XML Dateien..."
            directory = directoryChooser.showDialog(primaryStage)

            (page.lookup("#label_file") as Label).text = directory.absolutePath
        }

        (page.lookup("#button_confirm") as Button).onAction = EventHandler<ActionEvent> {
            if ((page.lookup("#radio_basex") as RadioButton).isSelected) {

                val basex = RestClient(
                    (page.findChildById("server") as TextField).text,
                    (page.findChildById("database") as TextField).text,
                    (page.findChildById("username") as TextField).text,
                    (page.findChildById("password") as PasswordField).text
                )

                server = createServer(basex, webappPort!!)
                server!!.start()

                startWebView(primaryStage)
            } else {
                try {
                    val baseXLocal = LocalBaseXClient(directory)

                    server = createServer(baseXLocal, webappPort!!)
                    server!!.start()

                    startWebView(primaryStage)
                }
                catch (e: Exception) {

                    val alert = Alert(Alert.AlertType.ERROR)
                    alert.title = "Fehlermeldung"
                    alert.headerText = "Etwas ist schief gelaufen."
                    alert.contentText = "Möglicherweise handelt es sich bei den Dateien im von Ihnen angegebenen " +
                            "Verzeichnis nicht um gültige BaseX Dateien. Starten Sie die Applikation erneut und " +
                            "wählen ein gültiges Verzeichnis."

                    alert.showAndWait()
                    exitProcess(0)
                }
            }
        }

        (page.lookup("#button_cancel") as Button).onAction = EventHandler<ActionEvent> {
            stop()
        }

    }

    override fun init() {
        super.init()
    }

    override fun stop() {
        super.stop()
        Platform.exit()
        exitProcess(0)
    }

    companion object {
        fun main(args: Array<String>) {
            launch(JavaFxApplication::class.java, *args)
        }
    }

    private fun startWebView(primaryStage: Stage) {
        primaryStage.title = "ÖGD-Report-Tool"
        val webView = WebView()
        val indicator = ProgressIndicator()
        primaryStage.scene = Scene(StackPane(webView, indicator), 1280.0, 800.0)
        webView.engine.load("http://localhost:$webappPort/")
        indicator.visibleProperty()
            .bind(webView.engine.loadWorker.stateProperty().isEqualTo(Worker.State.RUNNING))
        webView.contextMenuEnabledProperty().set(false)
        primaryStage.show()
        primaryStage.centerOnScreen()
    }


}

fun Node.findChildById(id: String): Node? {
    if (this.id == id) {
        return this
    }
    if (this !is Parent) {
        return null
    }
    for (node in this.childrenUnmodifiable) {
        val result = node.findChildById(id)
        if (result != null) {
            return result
        }

    }
    return null
}

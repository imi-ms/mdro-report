package de.uni_muenster.imi.oegd.application

import de.uni_muenster.imi.oegd.baseX.LocalBaseXClient
import de.uni_muenster.imi.oegd.common.GlobalData
import de.uni_muenster.imi.oegd.common.IBaseXClient
import de.uni_muenster.imi.oegd.common.RestClient
import de.uni_muenster.imi.oegd.common.findOpenPortInRange
import de.uni_muenster.imi.oegd.webapp.createServer
import io.ktor.server.netty.*
import javafx.application.Application
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.layout.StackPane
import javafx.scene.web.WebView
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import kotlinx.coroutines.runBlocking
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
    private val webappPort = findOpenPortInRange(1024..49151) ?: error("Cannot find free port for internal webserver!")
    private var server: NettyApplicationEngine? = null
    private lateinit var directory: File

    override fun start(primaryStage: Stage) {
        val page = FXMLLoader.load<Parent>(javaClass.getResource("/start-dialog.fxml"))
        primaryStage.scene = Scene(page)
        primaryStage.title = "MDReport"
        primaryStage.icons.add(Image("icon.png"))
        primaryStage.show()


        (page.lookup("#button_file") as Button).onAction = EventHandler<ActionEvent> {
            val directoryChooser = DirectoryChooser()
            directoryChooser.title = "Wähle Verzeichnis mit XML Dateien..."
            directory = directoryChooser.showDialog(primaryStage)

            (page.lookup("#label_file") as Label).text = directory.absolutePath
        }

        (page.lookup("#button_confirm") as Button).onAction = EventHandler<ActionEvent> {
            val basex: IBaseXClient
            if ((page.lookup("#radio_basex") as RadioButton).isSelected) {
                GlobalData.database = (page.lookup("#database") as TextField).text
                GlobalData.url = (page.lookup("#server") as TextField).text
                GlobalData.user = (page.lookup("#username") as TextField).text
                GlobalData.isLocal = false

                basex = RestClient(
                    (page.lookup("#server") as TextField).text,
                    (page.lookup("#database") as TextField).text,
                    (page.lookup("#username") as TextField).text,
                    (page.lookup("#password") as PasswordField).text
                )
                try {
                    assert("Test" == runBlocking { basex.executeXQuery("\"Test\"") })
                } catch (e: Exception) {
                    Alert(Alert.AlertType.ERROR).apply {
                        title = "Fehlermeldung"
                        headerText =
                            "Etwas ist schief gelaufen. Bitte überprüfen Sie die Verbindung zum BaseX-Server und die Zugangsdaten!"
                        contentText = "$e"
                    }.showAndWait()
                    return@EventHandler
                }
            } else {
                GlobalData.database="LOCAL"
                GlobalData.url="LOCAL"
                GlobalData.user="LOCAL"
                GlobalData.isLocal = true

                try {
                    basex = LocalBaseXClient(directory)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Alert(Alert.AlertType.ERROR).apply {
                        title = "Fehlermeldung"
                        headerText = "Etwas ist schief gelaufen."
                        contentText = "Möglicherweise handelt es sich bei den Dateien im von Ihnen angegebenen " +
                                "Verzeichnis nicht um gültige BaseX Dateien. Starten Sie die Applikation erneut und " +
                                "wählen ein gültiges Verzeichnis!"
                    }.showAndWait()
                    stop()
                }
            }
            server = createServer(basex, webappPort)
            server!!.start()

            startWebView(primaryStage)
        }

        (page.lookup("#button_cancel") as Button).onAction = EventHandler<ActionEvent> {
            stop()
        }

    }

    override fun init() {
        super.init()
    }

    override fun stop(): Nothing {
        super.stop()
        Platform.exit()
        server?.stop(1000, 1000)
        exitProcess(0)
    }

    companion object {
        fun main(args: Array<String>) {
            launch(JavaFxApplication::class.java, *args)
        }
    }

    private fun startWebView(primaryStage: Stage) {
        val webView = WebView()
        val indicator = ProgressIndicator()
        primaryStage.scene = Scene(StackPane(webView, indicator), 1280.0, 800.0)
        webView.engine.load("http://localhost:$webappPort/")
        webView.isContextMenuEnabled = false

        indicator.visibleProperty()
            .bind(webView.engine.loadWorker.stateProperty().isEqualTo(Worker.State.RUNNING))

        primaryStage.show()
        primaryStage.centerOnScreen()
    }


}


package de.uni_muenster.imi.oegd.application

import de.uni_muenster.imi.oegd.baseX.LocalBaseXClient
import de.uni_muenster.imi.oegd.common.IBaseXClient
import de.uni_muenster.imi.oegd.common.RestClient
import de.uni_muenster.imi.oegd.common.findOpenPortInRange
import de.uni_muenster.imi.oegd.webapp.createServer
import io.ktor.server.netty.*
import javafx.application.Application
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.concurrent.Worker
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.web.WebView
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Stage
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.URL
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
        primaryStage.title = "MREReport"
        primaryStage.icons.add(Image("label.png"))
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
        val indicator2 =
            VBox(ProgressIndicator(), Label("Erstellen des Berichtes kann einige Zeit dauern, bitte warten!")).apply {
                alignment = Pos.CENTER
                background = Background(BackgroundFill(Color(1.0, 1.0, 1.0, 0.5), CornerRadii.EMPTY, Insets.EMPTY))
            }
        indicator2.isVisible = false
        primaryStage.scene = Scene(StackPane(webView, indicator, indicator2), 1280.0, 800.0)
        webView.engine.load("http://localhost:$webappPort/")
        webView.isContextMenuEnabled = false
        indicator.visibleProperty()
            .bind(webView.engine.loadWorker.stateProperty().isEqualTo(Worker.State.RUNNING))
        webView.engine.locationProperty().addListener { _, oldLocation, newLocation ->
            //TODO: Womöglich wird dadurch die Datei zweimal heruntergeladen - einmal durch den Browser und einmal unten durch den Code
            if (newLocation.contains("downloadCache")) {
                val data = object : Task<String>() {
                    override fun call() = URL(newLocation).readText()
                }
                data.setOnFailed {
                    Alert(Alert.AlertType.ERROR, "Cannot download report. Please check stacktrace!")
                }

                val file = FileChooser().apply {
                    initialFileName = "report.mrereport"
                }.showSaveDialog(primaryStage) ?: return@addListener
                indicator2.visibleProperty().bind(data.runningProperty())
                webView.disableProperty().bind(data.runningProperty())
                data.setOnSucceeded {
                    file.writeText(data.get())
                }
                Thread(data).apply { isDaemon = true }.start()
            }
            if (newLocation.contains("imi.uni-muenster.de") || newLocation.contains("ukm.de")) {
                hostServices.showDocument(newLocation)
                webView.engine.load(oldLocation)
            }
        }
        primaryStage.show()
        primaryStage.centerOnScreen()
    }


}


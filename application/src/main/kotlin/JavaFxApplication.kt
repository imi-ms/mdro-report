package de.uni_muenster.imi.oegd.application

import de.uni_muenster.imi.oegd.baseX.LocalBaseXClient
import de.uni_muenster.imi.oegd.webapp.createServer
import de.uni_muenster.imi.oegd.webapp.findOpenPortInRange
import de.uni_muenster.imi.oegd.webapp.model.IBaseXClient
import de.uni_muenster.imi.oegd.webapp.model.RestClient
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.concurrent.Task
import javafx.concurrent.Worker
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.web.WebView
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Stage
import kotlinx.coroutines.runBlocking
import netscape.javascript.JSObject
import java.io.File
import java.net.URL
import java.util.*
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

@Suppress("UNCHECKED_CAST")
fun <T : javafx.scene.Node> javafx.scene.Node.find(cssSelector: String) = this.lookup(cssSelector) as T


class JavaFxApplication : Application() {
    private val webappPort = findOpenPortInRange(1024..49151) ?: error("Cannot find free port for internal webserver!")
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private var directory: File? = null
    private var language = Language.findByLocale(Locale.getDefault()) ?: Language.ENGLISH
    private lateinit var i18n: ResourceBundle

    override fun start(primaryStage: Stage) {
        drawStartDialog(primaryStage)
    }

    private fun drawStartDialog(primaryStage: Stage) {
        i18n = ResourceBundle.getBundle("internationalization", language.locale)
        val page = FXMLLoader.load<Parent>(javaClass.getResource("/start-dialog.fxml"), i18n)
        primaryStage.scene = Scene(page)
        primaryStage.title = "MREReport"
        primaryStage.icons.add(Image("label.png"))
        primaryStage.show()

        val loadingProperty = SimpleBooleanProperty(false)
        page.lookupAll(".disabled_while_loading").forEach { it.disableProperty().bind(loadingProperty) }
        page.find<ProgressIndicator>("#loading_spinner").visibleProperty().bind(loadingProperty)

        page.find<ComboBox<Language>>("#language_comboBox").apply {
            items = FXCollections.observableArrayList(*Language.entries.toTypedArray())
            setCellFactory { _ ->
                object : ListCell<Language>() {
                    override fun updateItem(item: Language?, empty: Boolean) {
                        super.updateItem(item, empty)
                        graphic = if (item != null && !empty) {
                            createImageLabel(i18n.getString(item.languageCode), item.imgPath)
                        } else {
                            null
                        }
                    }
                }
            }
            value = language
            buttonCell = cellFactory.call(null)
            selectionModel.selectedItemProperty().addListener { _, _, newValue ->
                language = newValue ?: Language.ENGLISH
                drawStartDialog(primaryStage)
            }
        }


        page.find<Button>("#button_file").onAction = EventHandler {
            val directoryChooser = DirectoryChooser()
            directoryChooser.title = i18n.getString("directoryChooser.header")
            directory = directoryChooser.showDialog(primaryStage)

            page.find<Label>("#label_file").text = directory?.absolutePath ?: i18n.getString("noDirectorySelected")
        }
        page.find<Button>("#button_confirm").onAction = EventHandler {
            val basex: IBaseXClient
            if (page.find<RadioButton>("#radio_basex").isSelected) {
                loadingProperty.set(true)

                basex = RestClient(
                    page.find<TextField>("#server").text,
                    page.find<TextField>("#database").text,
                    page.find<TextField>("#username").text,
                    page.find<PasswordField>("#password").text
                )

                val task: Task<String> = object : Task<String>() {
                    override fun call(): String {
                        return runBlocking { basex.executeXQuery("\"Test\"") }
                    }

                    override fun succeeded() {
                        val queryResult = this.value
                        if (queryResult != "Test") {
                            Alert(Alert.AlertType.ERROR).apply {
                                headerText = i18n.getString("errorBaseXConnection")
                                contentText = queryResult
                            }.showAndWait()
                        } else {
                            startServer(basex, primaryStage)
                        }
                    }

                    override fun done() {
                        loadingProperty.set(false)
                    }
                }

                Thread(task).start()

            } else {
                if (directory == null) {
                    Alert(Alert.AlertType.ERROR, i18n.getString("errorNoFolderSelected")).showAndWait()
                    return@EventHandler
                }

                try {
                    basex = LocalBaseXClient(directory!!)
                    startServer(basex, primaryStage)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Alert(Alert.AlertType.ERROR).apply {
                        headerText = i18n.getString("errorHeader")
                        contentText = i18n.getString("errorLocalBaseXContent")
                    }.showAndWait()
                    stop()
                }
            }

        }

        page.find<Button>("#button_cancel").onAction = EventHandler {
            stop()
        }
    }

    private fun startServer(basex: IBaseXClient, primaryStage: Stage) {
        server = createServer(basex, webappPort, language.locale)
        server!!.start()

        startWebView(primaryStage)
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
                isVisible = false
            }
        primaryStage.scene = Scene(StackPane(webView, indicator, indicator2), 1280.0, 800.0)
        webView.engine.load("http://localhost:$webappPort/")
        webView.isContextMenuEnabled = false
        indicator.visibleProperty()
            .bind(webView.engine.loadWorker.stateProperty().isEqualTo(Worker.State.RUNNING))

        fun downloadFile(url: String) {
            val data = object : Task<String>() {
                override fun call() = URL(url).readText()
            }
            data.setOnFailed {
                Alert(Alert.AlertType.ERROR, "Cannot download report. Please check stacktrace!")
            }

            val file = FileChooser().apply {
                initialFileName = when {
                    url.contains("downloadCache") -> "report.mrereport"
                    url.contains("MRGN") -> "mrgn.csv"
                    url.contains("MRSA") -> "mrsa.csv"
                    url.contains("VRE") -> "vre.csv"
                    else -> "file.txt"
                }
            }.showSaveDialog(primaryStage) ?: return
            indicator2.visibleProperty().bind(data.runningProperty())
            webView.disableProperty().bind(data.runningProperty())
            data.setOnSucceeded {
                file.writeText(data.get())
            }
            Thread(data).apply { isDaemon = true }.start()
        }

        //TODO: After loading of page, add this to any page
        (webView.engine.executeScript("window") as JSObject).setMember("Downloader", object {
            fun downloadFile(url: String) {
                downloadFile(url)
            }
        })

        webView.engine.locationProperty().addListener { _, oldLocation, newLocation ->
            //TODO: Dadurch wird die Datei zweimal heruntergeladen - einmal durch den Browser und einmal unten durch den Code
            if ("downloadCache" in newLocation || "list/csv" in newLocation) {
                downloadFile(newLocation)
                webView.engine.load(oldLocation)
            }
            if (newLocation.contains("imi.uni-muenster.de") || newLocation.contains("ukm.de")) {
                hostServices.showDocument(newLocation)
                webView.engine.load(oldLocation)
            }
        }
        primaryStage.show()
        primaryStage.centerOnScreen()
    }

    private fun createImageLabel(label: String, imgPath: String): Label = Label(label).apply {
        style = "-fx-text-fill: black"
        graphic = ImageView(Image(imgPath)).apply {
            fitWidth = 21.6
            fitHeight = 21.6
        }
    }

    private enum class Language(val languageCode: String, val locale: Locale, val imgPath: String) {
        GERMAN("language.de", Locale.GERMAN, "de.png"),
        ENGLISH("language.en", Locale.ENGLISH, "gb.png");


        companion object {
            fun findByLocale(locale: Locale) = entries.find { it.locale.language == locale.language }
        }
    }

}


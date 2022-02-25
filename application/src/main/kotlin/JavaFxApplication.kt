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
import javafx.stage.FileChooser
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
    private val webappPort = findOpenPortInRange(8000..8080)
    private var server: NettyApplicationEngine? = null
    private val projectDirectory = System.getProperty("user.dir")
    private var localFile: File? = null

    override fun start(primaryStage: Stage) {
        val page =
            FXMLLoader.load<Parent>(javaClass.getResource("/start-dialog.fxml"))

        primaryStage.scene = Scene(page)
        primaryStage.show()

        (page.lookup("#button_file") as Button).onAction = EventHandler<ActionEvent> {
            val fileChooser = FileChooser()
            fileChooser.title = "Öffne XML Datei..."
            localFile = fileChooser.showOpenDialog(primaryStage)

            if(localFile != null) {

                println(localFile!!.inputStream().readBytes().toString(Charsets.UTF_8))

                (page.lookup("#label_file") as Label).text = localFile!!.name
            }
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
                val localBaseXPort = findOpenPortInRange(8081..8888)
                val basexLocal = LocalBaseXClient(localFile!!, localBaseXPort!!)
                server = createServer(basexLocal, webappPort!!)
                server!!.start()

                startWebView(primaryStage)
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

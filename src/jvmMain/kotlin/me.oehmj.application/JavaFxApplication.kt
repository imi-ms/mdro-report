package me.oehmj.application


import RestClient
import javafx.application.Application
import javafx.concurrent.Worker
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.StackPane
import javafx.scene.web.WebView
import javafx.stage.Stage
import java.io.File
import kotlin.random.Random
import kotlin.random.nextInt


//TODO: Make seperate Gradle module for this
//TODO: Figure out how to ship this as a executable with its own JDK
class JavaFxApplication : Application() {
    val port = Random.nextInt(8000..8080) //TODO: Improve this logic to find a unused port
    var server = createServer(baseXClient, port)
    override fun start(primaryStage: Stage) {
        val page =
            FXMLLoader.load<Parent>(File("C:\\Users\\oehmj\\IdeaProjects\\oegdReportTool\\src\\jvmMain\\resources\\de\\oehmj\\application\\start-dialog.fxml").toURL())

        val alert = Alert(Alert.AlertType.CONFIRMATION)
        alert.dialogPane.content = page
        val btnType = alert.showAndWait()
        if (!btnType.isPresent || btnType.get() == ButtonType.CANCEL) {
            stop()
        }
        val basex = RestClient(
            (page.findChildById("server") as TextField).text,
            (page.findChildById("username") as TextField).text,
            (page.findChildById("password") as PasswordField).text,
            (page.findChildById("database") as TextField).text
        )
        //TODO: Apply credentials
        //TODO: Handle "Durchsuchen..." click => Öffne Durchsuchen Dialog
        //TODO: Integrate BaseX

        server.start()
        primaryStage.title = "ÖGD-Report-Tool"

        val webView = WebView()
        val indicator = ProgressIndicator()
        primaryStage.scene = Scene(StackPane(webView, indicator), 1280.0, 800.0)
        webView.engine.load("http://localhost:$port/")
        indicator.visibleProperty().bind(webView.engine.loadWorker.stateProperty().isEqualTo(Worker.State.RUNNING))
        webView.contextMenuEnabledProperty().set(false)
        primaryStage.show()
    }

    override fun init() {
        super.init()
    }

    override fun stop() {
        super.stop()
        server.stop(1000, 1000)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(JavaFxApplication::class.java, *args)
        }
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

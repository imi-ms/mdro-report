package me.oehmj.application


import javafx.application.Application
import javafx.concurrent.Worker
import javafx.scene.Scene
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.StackPane
import javafx.scene.web.WebView
import javafx.stage.Stage
import kotlin.random.Random
import kotlin.random.nextInt


class JavaFxApplication : Application() {
    val port = Random.nextInt(8000..8080) //TODO: Improve this logic
    var server = createServer(baseXClient, port)
    override fun start(primaryStage: Stage) {
        println("port = $port")
        server.start()
        primaryStage.title = "ÖGD-Report-Tool"

//        val root = StackPane()
//        val btn = Button("Say 'Hello World'").apply {
//            setOnAction{
//                println("Hello World!")
//            }
//        }


//        root.children.add(btn)
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
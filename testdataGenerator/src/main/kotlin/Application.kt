package de.uni_muenster.imi.oegd.testdataGenerator

import javafx.application.Application
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.Slider
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
    private lateinit var directory: File

    override fun start(primaryStage: Stage) {
        var page = FXMLLoader.load<Parent>(javaClass.getResource("/testdataGenerator.fxml"))
        primaryStage.scene = Scene(page)
        primaryStage.title = "MDReport Testdata Generator"
        primaryStage.show()

        (page.lookup("#label_sliderValue") as Label).textProperty().bind(
            Bindings.format(
                "%.0f",
                (page.lookup("#slider_numberOfPatients") as Slider).valueProperty()
            )
        )

        (page.lookup("#button_ok") as Button).isDisable = true
        (page.lookup("#button_selectLocation") as Button).onAction = EventHandler {
            try {
                val directoryChooser = DirectoryChooser()
                directoryChooser.title = "Wählen Sie ein Verzeichnis..."
                directory = directoryChooser.showDialog(primaryStage)

                (page.lookup("#label_location") as Label).text = directory.absolutePath
                (page.lookup("#button_ok") as Button).isDisable = false
            } catch(e: Exception) {}
        }
        (page.lookup("#selectBox_yearStart") as ChoiceBox<String>).items = getYearsList()
        (page.lookup("#selectBox_yearStart") as ChoiceBox<String>).value = "2021"

        (page.lookup("#selectBox_yearEnd") as ChoiceBox<String>).items = getYearsList()
        (page.lookup("#selectBox_yearEnd") as ChoiceBox<String>).value = "2022"

        (page.lookup("#button_cancel") as Button).onAction = EventHandler {
            stop()
        }

        (page.lookup("#button_ok") as Button).onAction = EventHandler {
            val numberOfPatients = (page.lookup("#slider_numberOfPatients") as Slider).value
            val yearStart = (page.lookup("#selectBox_yearStart") as ChoiceBox<String>).value
            val yearEnd = (page.lookup("#selectBox_yearEnd") as ChoiceBox<String>).value
            val location = directory.absolutePath

            page = FXMLLoader.load(javaClass.getResource("/progressView.fxml"))
            primaryStage.scene = Scene(page)
            primaryStage.show()

            Thread {
                setStartYear(yearStart)
                setEndYear(yearEnd)

                for(i in 1..numberOfPatients.toInt()) {
                    generateTestdataFile(i, location)
                    Platform.runLater {
                        (page.lookup("#loadingBar") as ProgressBar).progress = i / numberOfPatients
                    }
                }
                stop()
            }.start()



        }
    }

    override fun init() {
        super.init()
    }

    override fun stop(): Nothing {
        super.stop()
        Platform.exit()
        exitProcess(0)
    }

    companion object {
        fun main(args: Array<String>) {
            launch(JavaFxApplication::class.java, *args)
        }
    }

    private fun getYearsList(): ObservableList<String> {
        return FXCollections.observableList(listOf(1990..2030).flatten().map {it.toString()})
    }
}

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
import javafx.scene.control.*
import javafx.scene.control.Alert.AlertType
import javafx.scene.image.Image
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

@Suppress("UNCHECKED_CAST")
fun <T : javafx.scene.Node> javafx.scene.Node.find(cssSelector: String) = this.lookup(cssSelector) as T

class JavaFxApplication : Application() {
    private lateinit var directory: File

    override fun start(primaryStage: Stage) {
        var page = FXMLLoader.load<Parent>(javaClass.getResource("/testdataGenerator.fxml"))
        primaryStage.scene = Scene(page)
        primaryStage.title = "MREReport Testdata Generator"
        primaryStage.icons.add(Image("label.png"))
        primaryStage.show()

        page.find<Label>("#label_sliderValue").textProperty().bind(
            Bindings.format("%.0f", (page.lookup("#slider_numberOfPatients") as Slider).valueProperty())
        )

        page.find<Button>("#button_ok").isDisable = true
        page.find<Button>("#button_selectLocation").onAction = EventHandler {
            try {
                directory = DirectoryChooser().showDialog(primaryStage)

                page.find<Label>("#label_location").text = directory.absolutePath
                page.find<Button>("#button_ok").isDisable = false
            } catch (e: Exception) {/*Nothing to do here*/
            }
        }
        page.find<ChoiceBox<Int>>("#selectBox_yearStart").apply {
            items = getYearsList()
            value = 2021
        }

        page.find<ChoiceBox<Int>>("#selectBox_yearEnd").apply {
            items = getYearsList()
            value = 2022
        }

        page.find<Button>("#button_cancel").onAction = EventHandler {
            stop()
        }

        page.find<Button>("#button_ok").onAction = EventHandler {
            val numberOfPatients = page.find<Slider>("#slider_numberOfPatients").value
            val yearStart = page.find<ChoiceBox<Int>>("#selectBox_yearStart").value
            val yearEnd = page.find<ChoiceBox<Int>>("#selectBox_yearEnd").value
            val location = directory.absolutePath
            val generator = TestdataGenerator()

            page = FXMLLoader.load(javaClass.getResource("/progressView.fxml"))
            primaryStage.scene = Scene(page)
            primaryStage.show()

            Thread {
                generator.setStartYear(yearStart)
                generator.setEndYear(yearEnd)

                for(i in 1..numberOfPatients.toInt()) {
                    try {
                        generator.createTestdataFile(location)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Platform.runLater {
                            Alert(AlertType.ERROR, e.toString()).showAndWait()
                        }
                    }
                    Platform.runLater {
                        page.find<ProgressBar>("#loadingBar").progress = i / numberOfPatients
                    }
                }
                Platform.runLater {
                    Alert(AlertType.INFORMATION).apply {
                        title = "Generierung erfolgreich"
                        headerText = "Die Generierung war erfolgreich"
                        contentText = "Die Testdaten wurden erfolgreich unter dem von " +
                                "Ihnen angegebenen Pfad generiert. Die Applikation wird nun beendet. " +
                                "Sollten Sie noch weitere Daten generieren wollen, " +
                                "führen Sie den Testdaten Generator erneut aus."
                    }.showAndWait()

                    stop()
                }
            }.start()
        }
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

    private fun getYearsList(): ObservableList<Int> {
        return FXCollections.observableList((1990..2030).toList())
    }
}

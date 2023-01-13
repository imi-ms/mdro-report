package de.uni_muenster.imi.oegd.testdataGenerator

import javafx.application.Application
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.Alert.AlertType
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import java.io.File
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
    private lateinit var directory: File
    private var language: LANGUAGE = LANGUAGE.values().find { it.locale.language == Locale.getDefault().language } ?: LANGUAGE.ENGLISH
    private lateinit var i18n: ResourceBundle


    override fun start(primaryStage: Stage) {
        drawStartDialog(primaryStage)
    }

    private fun drawStartDialog(primaryStage: Stage) {
        i18n = ResourceBundle.getBundle("testdataGeneratorMessages", language.locale)
        val page = FXMLLoader.load<Parent>(javaClass.getResource("/testdataGenerator.fxml"), i18n)
        primaryStage.scene = Scene(page)
        primaryStage.title = "MREReport Testdata Generator"
        primaryStage.icons.add(Image("label.png"))
        primaryStage.show()

        page.find<Label>("#label_sliderValue").textProperty().bind(
            Bindings.format("%.0f", page.find<Slider>("#slider_numberOfPatients").valueProperty())
        )

        page.find<ComboBox<Any>>("#language_comboBox").apply {

            items = FXCollections.observableArrayList<Any>().apply {
                add(createImageLabel(i18n.getString(LANGUAGE.GERMAN.languageCode), LANGUAGE.GERMAN.imgPath))
                add(createImageLabel(i18n.getString(LANGUAGE.ENGLISH.languageCode), LANGUAGE.ENGLISH.imgPath))
            }

            value = createImageLabel(i18n.getString(language.languageCode), language.imgPath).apply {
                style = "-fx-text-fill: black"
            }

            selectionModel.selectedItemProperty().addListener { _, _, newValue ->
                run {
                    language = LANGUAGE.values().find { i18n.getString(it.languageCode) == ((newValue as Label).text) }
                        ?: LANGUAGE.ENGLISH
                    drawStartDialog(primaryStage)
                }
            }
        }

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
            val numberOfPatients = page.find<Slider>("#slider_numberOfPatients").value.toInt()
            val yearStart = page.find<ChoiceBox<Int>>("#selectBox_yearStart").value
            val yearEnd = page.find<ChoiceBox<Int>>("#selectBox_yearEnd").value
            val location = directory.absolutePath
            generateData(primaryStage, yearStart, yearEnd, numberOfPatients, location)
        }
    }

    private fun generateData(
        primaryStage: Stage,
        yearStart: Int,
        yearEnd: Int,
        numberOfPatients: Int,
        location: String
    ) {
        val progressView = FXMLLoader.load<Parent>(javaClass.getResource("/progressView.fxml"), i18n)
        primaryStage.scene = Scene(progressView)
        primaryStage.show()

        val task = object : Task<Unit>() {
            override fun call() {
                val generator = TestdataGenerator()
                generator.setStartYear(yearStart)
                generator.setEndYear(yearEnd)

                for (i in 1..numberOfPatients) {
                    try {
                        generator.createTestdataFile(location)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Platform.runLater {
                            Alert(AlertType.ERROR, e.toString()).showAndWait()
                        }
                    }
                    updateProgress(i.toLong(), numberOfPatients.toLong())

                }
            }
        }
        task.onSucceeded = EventHandler {
            Alert(AlertType.INFORMATION).apply {
                title = i18n.getString("view.successfullyGenerated.title")
                headerText = i18n.getString("view.successfullyGenerated.header")
                contentText = i18n.getString("view.successfullyGenerated.content")
            }.showAndWait()

            stop()
        }
        progressView.find<ProgressBar>("#loadingBar").progressProperty().bind(task.progressProperty())
        Thread(task).start()
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

    private fun createImageLabel(label: String, imgPath: String): Label = Label(label).apply {
        graphic = ImageView(Image(imgPath)).apply {
            fitWidth = 21.6
            fitHeight = 21.6
        }
    }

    private enum class LANGUAGE(val languageCode: String, val locale: Locale, val imgPath: String) {
        GERMAN("language.de", Locale.GERMAN, "de.png"),
        ENGLISH("language.en", Locale.ENGLISH, "gb.png")
    }
}

package de.uni_muenster.imi.oegd.webapp

import de.uni_muenster.imi.oegd.webapp.model.IBaseXClient
import de.uni_muenster.imi.oegd.webapp.model.RestClient
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.util.*


private val log = KotlinLogging.logger { }

/**
 * Entry point for running standalone .jar file with internal Netty server
 */
fun main(args: Array<String>) {
    fun askUser(message: String): String {
        println(message)
        return readln()
    }

    val webappPort = findOpenPortInRange(8080..8888) ?: error("No free port available!")

    val baseXClient = RestClient(
        baseURL = args.getOrNull(0) ?: askUser("Bitten geben Sie die BaseX-URL an: "),
        username = args.getOrNull(1) ?: askUser("Bitte geben Sie den BaseX-Benutzer an: "),
        password = args.getOrNull(2) ?: System.console()?.readPassword("Bitte geben Sie das Passwort ein: \n")!!
            .concatToString(),
        database = args.getOrNull(3) ?: askUser("Bitte geben Sie den Datenbanknamen an: ")
    )

    //Test connnection
    runBlocking { baseXClient.executeXQuery("'Test'") == "Test" || error("Cannot connect to BaseX") }

    log.info { "Starting local webserver on port $webappPort" }

    val createServer = createServer(baseXClient, webappPort)
//    createServer.pipeline.execute(NettyApplicationCall())
    createServer.start(wait = true)
}

/**
 * Create internal Netty server instance (standalone .jar deployment or inside JavaFX GUI)
 */
fun createServer(baseXClient: IBaseXClient, port: Int = 8080, locale: Locale = Locale.getDefault()) =
    embeddedServer(
        Netty,
        host = "127.0.0.1",
        port = port,
        module = application(baseXClient, language = locale),
        watchPaths = listOf("classes", "de/uni_muenster/imi/oegd/webapp/viewni_muenster/imi/oegd/webapp/view")
    )

/**
 * Entrypoint for deployment as .war file (Tomcat, ...)
 * Config will be loaded from application.conf
 */
fun Application.warEntrypoint() {
    val baseXClient = with(environment.config.config("BaseX")) {
        RestClient(
            baseURL = property("baseUrl").getString(),
            username = property("username").getString(),
            password = property("password").getString(),
            database = property("database").getString()
        )
    }
    application(baseXClient, serverMode = true, language = Locale.getDefault())()
}




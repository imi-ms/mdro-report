package de.uni_muenster.imi.oegd.webapp

import de.uni_muenster.imi.oegd.common.BaseXQueries
import de.uni_muenster.imi.oegd.common.RestClient
import kotlin.system.measureTimeMillis

suspend fun main() {
    val restclient = RestClient("https://basex.ukmuenster.de/rest", "2021", "oehm", "censored")

    restclient.executeXQuery(BaseXQueries.getNasenRachenAbstriche())

    val time1 = measureTimeMillis {
        repeat(10) {
            restclient.executeXQuery(BaseXQueries.getNasenRachenAbstriche())
        }
    }
    println("time1 = $time1")

    restclient.executeXQuery(BaseXQueries.getNasenRachenAbstriche2())
    val time2 = measureTimeMillis {
        repeat(10) {
            restclient.executeXQuery(BaseXQueries.getNasenRachenAbstriche2())
        }
    }
    println("time2 = $time2")

    restclient.executeXQuery(BaseXQueries.getNasenRachenAbstriche2())
}
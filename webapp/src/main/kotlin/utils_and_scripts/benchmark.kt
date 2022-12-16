package de.uni_muenster.imi.oegd.webapp

import de.uni_muenster.imi.oegd.common.BaseXQueries
import de.uni_muenster.imi.oegd.common.RestClient
import kotlin.system.measureTimeMillis

/**
 * Utility to compare two different BaseX-Queries for performance
 */
suspend fun main() {
    val restclient = RestClient("https://basex.ukmuenster.de/rest", "2021", "oehm", "edQu5PyusVt11yg")

    val query1 = BaseXQueries.getFallzahlen()
    val query2 = BaseXQueries.getFallzahlen2()

    println("value1 = " + restclient.executeXQuery(query1))
    val time1 = measureTimeMillis {
        repeat(10) {
            restclient.executeXQuery(query1)
        }
    }
    println("time1 = $time1")

    println("value2 = " + restclient.executeXQuery(query2))
    val time2 = measureTimeMillis {
        repeat(10) {
            restclient.executeXQuery(query2)
        }
    }
    println("time2 = $time2")

    restclient.executeXQuery(query2)
}
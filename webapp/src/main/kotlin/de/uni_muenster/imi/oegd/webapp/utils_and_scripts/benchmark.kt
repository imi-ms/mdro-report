import de.uni_muenster.imi.oegd.webapp.model.BaseXQueries
import de.uni_muenster.imi.oegd.webapp.model.BaseXQueries.applyCaseTypeFilter
import de.uni_muenster.imi.oegd.webapp.model.CaseType
import de.uni_muenster.imi.oegd.webapp.model.FilterParams
import de.uni_muenster.imi.oegd.webapp.model.RestClient
import de.uni_muenster.imi.oegd.webapp.model.XQueryParams
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * Utility to compare two different BaseX-Queries for performance
 */
suspend fun main() {
    val restclient = RestClient("https://basex.ukmuenster.de/rest", "2024", "toennies", "BWqNlieaPMKMuTtyjIla")

    val folder = File("webapp/src/main/resources/queries_generic")
    val folder2 = File("webapp/src/main/resources/queries")

    val foo = File("webapp/src/main/resources/mdro-common.xqm").readText()

    val bar = $$"let $module-content := '$$foo'"



    for (file in folder.listFiles()) {

        val oldValue = """import module namespace mdro = "urn:mdro-report:common" at "../lib/mdro-common.xqm";"""
        val newValue = $$"""let $mdro := load-xquery-module("urn:mdro-report:common", map { "content": $module-content } )"""
        val r = Regex($$"(\\$?mdro:)")
        val q1 = bar + "\n" + file.readText().replace(oldValue, newValue).replace(r, "\\\$mdro:")
        val baz = FilterParams(caseTypes = CaseType.entries)

        val query1 =  applyCaseTypeFilter(applyYearFilter(q1), baz)
        val query2 =  applyCaseTypeFilter(applyYearFilter(File(folder2, file.name).readText()), baz)

        val a = restclient.executeXQuery(query1)
        println()
        println()
        val b = restclient.executeXQuery(query2)
        println()
        println(file.name)
        println(a == b)

        File("${file.name}_a1.txt").writeText(q1)
        File("${file.name}_a.txt").writeText(a)
        File("${file.name}_b.txt").writeText(b)
    }


//    val query1 = applyYearFilter(BaseXQueries.MRGN)
//    val query2 = applyYearFilter(BaseXQueries.MRGNv4)

//    println("value1 = " + restclient.executeXQuery(query1))
//    val time1 = measureTimeMillis {
//        repeat(10) {
//            restclient.executeXQuery(query1)
//        }
//    }
//    println("time1 = $time1")
//
//    println()
//    println()
//
//    println("value2 = " + restclient.executeXQuery(query2))
//    val time2 = measureTimeMillis {
//        repeat(10) {
//            restclient.executeXQuery(query2)
//        }
//    }
//    println("time2 = $time2")




}

private fun applyYearFilter(query: String, year: Int= 2024): String {
    val startDate = "${year}-01-01T00:00:00"
    val endDate = "${year}-12-31T23:59:59"

    return query
        .replace("#YEAR_START", startDate)
        .replace("#YEAR_END", endDate)
}
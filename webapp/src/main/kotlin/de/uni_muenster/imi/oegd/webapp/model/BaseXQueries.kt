package de.uni_muenster.imi.oegd.webapp.model

object BaseXQueries {
    val MRSA: String = readFile("mrsa_excelv3.xq")
    val MRGN: String = readFile("mrgn_excelv3.xq")
    val VRE: String = readFile("vre_excelv2.xq")
    val Falltage: String = readFile("Falltage.xq")
    val FalltageMRSA: String = readFile("Falltage_mrsa1.xq")
    val Fallzahlen: String = readFile("fallzahlen.xq")
    val Fallzahlen2: String = readFile("fallzahlen2.xq")
    val MRSABK: String = readFile("mrsa_bk.xq")
    val MSSABK: String = readFile("mssa_bk.xq")
    val NasenRachenAbstriche: String = readFile("naserachenabstrich.xq")
    val NasenRachenAbstriche2: String = readFile("nasenrachenabstrich2.xq")
    val AnzahlEFaecalis: String = readFile("anzahlEfaecalis.xq")
    val AnzahlEFaecalis2: String = readFile("anzahlEfaecalis2.xq")
    val AnzahlEFaecium: String = readFile("anzahlEfaecium.xq")
    val EfaeciumBK: String = readFile("efaecium_bk.xq")
    val EfaeciumBK2: String = readFile("efaecium_bk2.xq")
    val VREBK: String = readFile("vre_bk.xq")

    private fun readFile(filename: String) = javaClass.classLoader
        .getResourceAsStream("queries/$filename")!!
        .readBytes().toString(Charsets.UTF_8)

    fun applyParams(query: String, params: Params): String {
        return applyCaseTypeFilter(applyXQueryParams(query, params.xquery), params.filter)
    }

    fun applyXQueryParams(query: String, params: XQueryParams): String {
        return applyYearFilter(query, params)
    }


    private fun applyYearFilter(query: String, xQueryParams: XQueryParams): String {
        val startDate = "${xQueryParams.year}-01-01T00:00:00"
        val endDate = "${xQueryParams.year}-12-31T23:59:59"

        return query
            .replace("#YEAR_START", startDate)
            .replace("#YEAR_END", endDate)
    }


    // "STATIONAER" -> "S"
    //                "NACHSTATIONAER" -> "NS"
//                "VORSTATIONAER" -> "VS"
//                "TEILSTATIONAER" -> "TS"
//                "AMBULANT" -> "A"
//                "BEGLEITPERSON" -> "H"
//                "GEPLANTER_FALL" -> "P"
    private fun applyCaseTypeFilter(query: String, filterParams: FilterParams): String {
        //Add deprecated shortend version
        val caseTypes = filterParams.caseTypes.flatMap { it.basexName }
        return query.replace("#CASE_TYPE", caseTypes.joinToString("','", "('", "')"))
    }
}



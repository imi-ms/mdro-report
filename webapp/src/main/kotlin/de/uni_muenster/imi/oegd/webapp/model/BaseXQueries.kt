package de.uni_muenster.imi.oegd.webapp.model

object BaseXQueries {
    val MRSA: String = getXQuery("mrsa_excelv3.xq")
    val MRGN: String = getXQuery("mrgn_excelv3.xq")
//    val MRGNv4: String = getXQuery("mrgn_excelv4.xq")
    val VRE: String = getXQuery("vre_excelv2.xq")
    val Falltage: String = getXQuery("Falltage.xq")
    val FalltageMRSA: String = getXQuery("Falltage_mrsa1.xq")
    val Fallzahlen: String = getXQuery("fallzahlen.xq")
    val Fallzahlen2: String = getXQuery("fallzahlen2.xq")
    val MRSABK: String = getXQuery("mrsa_bk.xq")
    val MSSABK: String = getXQuery("mssa_bk.xq")
    val NasenRachenAbstriche: String = getXQuery("naserachenabstrich.xq")
    val NasenRachenAbstriche2: String = getXQuery("nasenrachenabstrich2.xq")
    val AnzahlEFaecalis: String = getXQuery("anzahlEfaecalis.xq")
    val AnzahlEFaecalis2: String = getXQuery("anzahlEfaecalis2.xq")
    val AnzahlEFaecium: String = getXQuery("anzahlEfaecium.xq")
    val EfaeciumBK: String = getXQuery("efaecium_bk.xq")
    val EfaeciumBK2: String = getXQuery("efaecium_bk2.xq")
    val VREBK: String = getXQuery("vre_bk.xq")



    private fun getXQuery(filename: String): String {
        fun readFile(filename: String) = try {
            javaClass.classLoader
                .getResourceAsStream("queries_generic/$filename")!!
                .readBytes().toString(Charsets.UTF_8)
        } catch (e: Exception) {
            throw Error("could not read $filename", e)
        }

        return readFile("lib.xq")+"\n\n"+readFile(filename)
    }

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


//     "STATIONAER" -> "S"
//     "NACHSTATIONAER" -> "NS"
//     "VORSTATIONAER" -> "VS"
//     "TEILSTATIONAER" -> "TS"
//     "AMBULANT" -> "A"
//     "BEGLEITPERSON" -> "H"
//     "GEPLANTER_FALL" -> "P"
    fun applyCaseTypeFilter(query: String, filterParams: FilterParams): String {
        //Add deprecated shortend version
        val caseTypes = filterParams.caseTypes.flatMap { it.basexName }
        println("caseTypes = $caseTypes")
        return query.replace("#CASE_TYPE", caseTypes.joinToString("','", "('", "')"))
    }
}



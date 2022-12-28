package model

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
    val EfaeciumBK: String = readFile("efaecium_bk.xq")
    val EfaeciumBK2: String = readFile("efaecium_bk2.xq")
    val VREBK: String = readFile("vre_bk.xq")

    private fun readFile(filename: String): String {
        return javaClass.classLoader
            .getResourceAsStream("queries/$filename")!!
            .readBytes().toString(Charsets.UTF_8)
    }

    fun applyParams(query: String, xQueryParams: XQueryParams): String {
        return applyYearFilter(query, xQueryParams)
//            .replace("STATIONAER", "S")
    }

    private fun applyYearFilter(query: String, xQueryParams: XQueryParams): String {
        val startDate = "${xQueryParams.year}-01-01T00:00:00"
        val endDate = "${xQueryParams.year}-12-31T23:59:59"

        return query
            .replace("#YEAR_START", startDate)
            .replace("#YEAR_END", endDate)
    }
}



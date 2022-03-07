package de.uni_muenster.imi.oegd.webapp

object DataProcessor {
    fun countMRSATotal(caseList: String) = WebappComponents.getMRSACSV(caseList).size

    fun countMRSANosokomial(caseList: String) = WebappComponents.getMRSACSV(caseList).filter { it["nosokomial?"] == "true" }.size

    fun countMRSAImported(caseList:String) = WebappComponents.getMRSACSV(caseList).filter { it["nosokomial?"] != "true" }.size

    fun countMRGN3Cases(caseList: String) = WebappComponents.getMRGACSV(caseList).filter {it["Piperacillin und Tazobactam Ergebnis"] == "MRGN3"}.size

    fun countMRGN4Cases(caseList: String) = WebappComponents.getMRGACSV(caseList).filter {it["Piperacillin und Tazobactam Ergebnis"] == "MRGN4"}.size

    fun countVREEfaeciumResistant(caseList: String) = WebappComponents.getVRECSV(caseList).filter {
        it["Vancomycin Ergebnis"] == "R" && it["Erreger"] == "Enterococcus faecium"
    }.size

    fun countVREEfaeciumTotal(caseList: String) = WebappComponents.getVRECSV(caseList).filter {
        (it["Vancomycin Ergebnis"] == "R" || it["Vancomycin Ergebnis"] == "S") && it["Erreger"] == "Enterococcus faecium"
    }.size

    fun countVREEfaecalisResistant(caseList: String) = WebappComponents.getVRECSV(caseList).filter {
        it["Vancomycin Ergebnis"] == "R" && it["Erreger"] == "Enterococcus faecalis"
    }.size


    fun countOtherCases(caseList: String) = WebappComponents.getVRECSV(caseList).filter {
        it["Erreger"] != "Enterococcus faecalis" && it["Erreger"] != "Enterococcus faecium"
    }.size
}
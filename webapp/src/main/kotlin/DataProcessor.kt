package de.uni_muenster.imi.oegd.webapp

object DataProcessor {
    fun countMRSATotal(caseList: List<Map<String, String>>) = caseList.size

    fun countMRSANosokomial(caseList: List<Map<String, String>>) =
        caseList.count { it["nosokomial?"] == "true" }

    fun countMRSAImported(caseList: List<Map<String, String>>) =
        caseList.count { it["nosokomial?"] != "true" }

    fun countMRGN3Cases(caseList: List<Map<String, String>>) =
        caseList.count { it["Klasse"] == "MRGN3" }

    fun countMRGN4Cases(caseList: List<Map<String, String>>) =
        caseList.count { it["Klasse"] == "MRGN4" }

    fun countVREEfaeciumResistant(caseList: List<Map<String, String>>) = caseList.count {
        it["Vancomycin Ergebnis"] == "R" && it["Erreger"] == "Enterococcus faecium"
    }

    fun countVREEfaeciumTotal(caseList: List<Map<String, String>>) = caseList.count {
        (it["Vancomycin Ergebnis"] == "R" || it["Vancomycin Ergebnis"] == "S") && it["Erreger"] == "Enterococcus faecium"
    }

    fun countVREEfaecalisResistant(caseList: List<Map<String, String>>) = caseList.count {
        it["Vancomycin Ergebnis"] == "R" && it["Erreger"] == "Enterococcus faecalis"
    }

    fun countOtherCases(caseList: List<Map<String, String>>) = caseList.count {
        it["Erreger"] != "Enterococcus faecalis" && it["Erreger"] != "Enterococcus faecium"
    }
}
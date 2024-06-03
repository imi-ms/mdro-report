package de.uni_muenster.imi.oegd.webapp.model

object DataProcessor {
    fun countMRSATotal(caseList: List<Map<String, String>>) = caseList.size

    fun countMRSANosokomial(caseList: List<Map<String, String>>) = caseList.count { it["nosocomial"] == "nosokomial" }

    fun countMRSAImported(caseList: List<Map<String, String>>) = caseList.count { it["nosocomial"] != "nosokomial" }

    fun countMRGN3Cases(caseList: List<Map<String, String>>) = caseList.count { it["class"] == "3MRGN" }

    fun countMRGN4Cases(caseList: List<Map<String, String>>) = caseList.count { it["class"] == "4MRGN" }

    fun countVREEfaeciumResistant(caseList: List<Map<String, String>>) = caseList.count {
        it["vancomycin"] == "R" && it["pathogen"] == "Enterococcus faecium"
    }

    fun countVREEfaeciumTotal(caseList: List<Map<String, String>>) = caseList.count {
        (it["vancomycin"] == "R" || it["vancomycin"] == "S") && it["pathogen"] == "Enterococcus faecium"
    }

    fun countVREEfaecalisResistant(caseList: List<Map<String, String>>) = caseList.count {
        it["vancomycin"] == "R" && it["pathogen"] == "Enterococcus faecalis"
    }

    fun countOtherCases(caseList: List<Map<String, String>>) = caseList.count {
        it["pathogen"] != "Enterococcus faecalis" && it["pathogen"] != "Enterococcus faecium"
    }
}
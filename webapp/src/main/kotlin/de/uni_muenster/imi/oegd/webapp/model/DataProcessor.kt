package de.uni_muenster.imi.oegd.webapp.model

object DataProcessor {
    fun countMRSATotal(caseList: List<Map<String, String>>) = caseList.size

    fun countMRSANosokomial(caseList: List<Map<String, String>>) = caseList.count { it["nosocomial"] == "nosokomial" }

    fun countMRSAImported(caseList: List<Map<String, String>>) = caseList.count { it["nosocomial"] != "nosokomial" }

    fun countMRGN3Cases(caseList: List<Map<String, String>>) = caseList.count { it["class"] == "3MRGN" }

    fun countMRGN4Cases(caseList: List<Map<String, String>>) = caseList.count { it["class"] == "4MRGN" }

    fun countVREEfaeciumResistant(caseList: List<Map<String, String>>) = caseList.count {
        it["vancomycin"] == "R" && "faecium" in it["pathogen"]!!
    }

    fun countVREEfaeciumTotal(caseList: List<Map<String, String>>) = caseList.count {
        (it["vancomycin"] == "R" || it["vancomycin"] == "S") && "faecium" in it["pathogen"]!!
    }

    fun countVREEfaecalisResistant(caseList: List<Map<String, String>>) = caseList.count {
        it["vancomycin"] == "R" && "faecalis" in it["pathogen"]!!
    }

    fun countOtherCases(caseList: List<Map<String, String>>) = caseList.count {
        "faecalis" in it["pathogen"]!! && "faecium" in it["pathogen"]!!
    }
}
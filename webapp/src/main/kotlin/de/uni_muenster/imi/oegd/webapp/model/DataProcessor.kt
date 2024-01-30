package de.uni_muenster.imi.oegd.webapp.model

object DataProcessor {
    fun countMRSATotal(caseList: List<Map<String, String>>) = caseList.size

    fun countMRSANosokomial(caseList: List<Map<String, String>>) = caseList.count { it["page.MRSA.caselist.nosocomial"] == "nosokomial" }

    fun countMRSAImported(caseList: List<Map<String, String>>) = caseList.count { it["page.MRSA.caselist.nosocomial"] != "nosokomial" }

    fun countMRGN3Cases(caseList: List<Map<String, String>>) =
        caseList.count { it["page.MRGN.caselist.class"] == "3MRGN" }

    fun countMRGN4Cases(caseList: List<Map<String, String>>) =
        caseList.count { it["page.MRGN.caselist.class"] == "4MRGN" }

    fun countVREEfaeciumResistant(caseList: List<Map<String, String>>) = caseList.count {
        it["page.VRE.caselist.vancomycin"] == "R" && it["page.VRE.caselist.pathogen"] == "Enterococcus faecium"
    }

    fun countVREEfaeciumTotal(caseList: List<Map<String, String>>) = caseList.count {
        (it["page.VRE.caselist.vancomycin"] == "R" || it["page.VRE.caselist.vancomycin"] == "S") && it["page.VRE.caselist.pathogen"] == "Enterococcus faecium"
    }

    fun countVREEfaecalisResistant(caseList: List<Map<String, String>>) = caseList.count {
        it["page.VRE.caselist.vancomycin"] == "R" && it["page.VRE.caselist.pathogen"] == "Enterococcus faecalis"
    }

    fun countOtherCases(caseList: List<Map<String, String>>) = caseList.count {
        it["page.VRE.caselist.pathogen"] != "Enterococcus faecalis" && it["page.VRE.caselist.pathogen"] != "Enterococcus faecium"
    }
}
package de.uni_muenster.imi.oegd.webapp

import de.uni_muenster.imi.oegd.baseX.BaseXQueries
import de.uni_muenster.imi.oegd.baseX.IBaseXClient
import de.uni_muenster.imi.oegd.baseX.parseCsv

object WebappComponents {
    fun getMRSACSV(mrsaList: String): List<Map<String, String>> {
        return parseCsv(
            mrsaList,
            listOf(
                "PID",
                "Abnahmezeitpunkt",
                "Probeart",
                "Infektion",
                "nosokomial?",
                "Einsender",
                "Einsender2",
                "Spa",
                "ClusterType"
            )
        )
    }

    fun getMRGACSV(mrgaList: String): List<Map<String, String>> {
        return parseCsv(
            mrgaList,
            listOf(
                "PID",
                "Abnahmezeitpunkt",
                "Probenart",
                "Einsender",
                "Einsender2",
                "Klasse",
                "Piperacillin und Tazobactam Ergebnis",
                "Cefotaxime Ergebnis",
                "cefTAZidime Ergebnis",
                "Cefepime Ergebnis",
                "Meropenem Ergebnis",
                "Imipenem Ergebnis",
                "Ciprofloxacin Ergebnis"
            )
        )
    }

    fun getVRECSV(vreList: String): List<Map<String, String>> {
        return parseCsv(
            vreList,
            listOf(
                "PID",
                "Abnahmezeitpunkt",
                "Probenart",
                "Einsender",
                "Einsender2",
                "Erreger",
                "Linezolid Ergebnis",
                "Tigecylin Ergebnis",
                "Vancomycin Ergebnis",
                "Teicoplanin Ergebnis",
                "Quinupristin und Dalfopristin Ergebnis"
            )
        )
    }

    //TODO: Übersicht auf Plausibilität checken / checken lassen

    suspend fun getMRSAOverview(baseXClient: IBaseXClient): List<OverviewEntry> {
        val fallzahlen = baseXClient.executeXQuery(BaseXQueries.getFallzahlen())
        val falltage = baseXClient.executeXQuery(BaseXQueries.getFalltage())
        val nasenrachenabstriche = baseXClient.executeXQuery(BaseXQueries.getNasenRachenAbstriche())
        val mssabk = baseXClient.executeXQuery(BaseXQueries.getMSSABK())
        val mrsabk = baseXClient.executeXQuery(BaseXQueries.getMRSABK())

        val fallliste = baseXClient.executeXQuery(BaseXQueries.getMRSA())
        val mrsaTotal = DataProcessor.countMRSATotal(fallliste)
        val mrsaNosokomial = DataProcessor.countMRSANosokomial(fallliste)
        val mrsaImported = DataProcessor.countMRSAImported(fallliste)

        return listOf(
            object: OverviewEntry("stationäre Fälle gesamt pro Erfassungszeitraum", BaseXQueries.getFallzahlen(), fallzahlen){},
            object: OverviewEntry("stationäre Falltage gesamt pro Erfassungszeitraum", BaseXQueries.getFalltage(), falltage){},
            object: OverviewEntry("Anzahl der Nasenabstriche bzw. kombinierte Nasen/Rachenabstiche pro Erfassungszeitraum", BaseXQueries.getNasenRachenAbstriche(), nasenrachenabstriche){},
            object: OverviewEntry("Anzahl aller S. aureus aus Blutkulturen (MSSA und MRSA)", BaseXQueries.getMSSABK(), mssabk){},
            object: OverviewEntry("Anzahl MRSA aus Blutkulturen", BaseXQueries.getMRSABK(), mrsabk){},
            object: OverviewEntry("Gesamtanzahl aller Fälle mit Methicillin Resistenten S. aureus (MRSA)", BaseXQueries.getMRSA(), "$mrsaTotal"){},
            object: OverviewEntry("Anzahl der importierten MRSA Fälle", BaseXQueries.getMRSA(), "$mrsaImported"){},
            object: OverviewEntry("Anzahl nosokomialer MRSA Fälle", BaseXQueries.getMRSA(), "$mrsaNosokomial"){},
            object: OverviewEntry("stationäre Falltage von MRSA-Fällen", BaseXQueries.getFallzahlen(), fallzahlen){}
        )
    }

    suspend fun getMRGNOverview(baseXClient: IBaseXClient): List<OverviewEntry> {
        val fallzahlen = baseXClient.executeXQuery(BaseXQueries.getFallzahlen())
        val falltage = baseXClient.executeXQuery(BaseXQueries.getFalltage())
        val fallliste = baseXClient.executeXQuery(BaseXQueries.getMRGN())
        val mrgn3Cases = DataProcessor.countMRGN3Cases(fallliste)
        val mrgn4Cases = DataProcessor.countMRGN4Cases(fallliste)

        return listOf(
            object: OverviewEntry("stationäre Fälle gesamt pro Erfassungszeitraum", BaseXQueries.getFallzahlen(), fallzahlen){},
            object: OverviewEntry("stationäre Falltage gesamt pro Erfassungszeitraum", BaseXQueries.getFalltage(), falltage){},
            object: OverviewEntry("Anzahl der 3MRGN Fälle", BaseXQueries.getMRGN(), "$mrgn3Cases"){},
            object: OverviewEntry("Anzahl der 4MRGN Fälle", BaseXQueries.getMRGN(), "$mrgn4Cases"){},
        )
    }

    suspend fun getVREOverview(baseXClient: IBaseXClient): List<OverviewEntry> {
        val fallzahlen = baseXClient.executeXQuery(BaseXQueries.getFallzahlen())
        val falltage = baseXClient.executeXQuery(BaseXQueries.getFalltage())
        val numEfaecalis = baseXClient.executeXQuery(BaseXQueries.getAnzahlEFaecalis())
        val caseList = baseXClient.executeXQuery(BaseXQueries.getVRE())
        val numEfaecalisResistant = DataProcessor.countVREEfaecalisResistant(caseList)
        val numEfaeciumTotal = DataProcessor.countVREEfaeciumTotal(caseList)
        val numEfaeciumResistant = DataProcessor.countVREEfaeciumResistant(caseList)
        val numOtherCases = DataProcessor.countOtherCases(caseList)
        val numEfaecium = baseXClient.executeXQuery(BaseXQueries.getEfaeciumBK())
        val numVREEfaecium = baseXClient.executeXQuery(BaseXQueries.getVREBK())

        return listOf(
            object: OverviewEntry("stationäre Fälle gesamt pro Erfassungszeitraum", BaseXQueries.getFallzahlen(), fallzahlen){},
            object: OverviewEntry("stationäre Falltage gesamt pro Erfassungszeitraum", BaseXQueries.getFalltage(), falltage){},
            object: OverviewEntry("Anzahl der gesamten E.faecalis Fälle (resistente und sensible)", BaseXQueries.getAnzahlEFaecalis(), numEfaecalis) {},
            object: OverviewEntry("Anzahl der VRE E.faecalis Fälle", BaseXQueries.getVRE(), "$numEfaecalisResistant") {},
            object: OverviewEntry("Anzahl der gesamten E.faecium Fälle (resistente und sensible)", BaseXQueries.getVRE(), "$numEfaeciumTotal") {},
            object: OverviewEntry("Anzahl der VRE E.faecium Fälle", BaseXQueries.getVRE(), "$numEfaeciumResistant") {},
            object: OverviewEntry("Anzahl sonstiger VRE Fälle", BaseXQueries.getVRE(), "$numOtherCases") {},
            object: OverviewEntry("Anzahl E.faecium Fälle (inkl. Vancomycin empfindliche und resistente Isolate) in Blutkulturen (Angabe nur einer 1 Kultur pro Patient)", BaseXQueries.getEfaeciumBK(), numEfaecium) {},
            object: OverviewEntry("Anzahl der VRE-E.faecium Fälle in Blutkulturen (Angabe nur einer 1 Kultur pro Patient)", BaseXQueries.getVREBK(), numVREEfaecium) {}

        )
    }
}
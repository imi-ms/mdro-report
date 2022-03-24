package de.uni_muenster.imi.oegd.webapp

import de.uni_muenster.imi.oegd.common.BaseXQueries
import de.uni_muenster.imi.oegd.common.GermType
import de.uni_muenster.imi.oegd.common.IBaseXClient
import de.uni_muenster.imi.oegd.common.parseCsv

object WebappComponents {
    suspend fun getCaseList(basexClient: IBaseXClient, germ: GermType, xQueryParams: XQueryParams) =
        when (germ) {
            GermType.MRSA -> getMRSACSV(basexClient, xQueryParams)
            GermType.MRGN -> getMRGACSV(basexClient, xQueryParams)
            GermType.VRE -> getVRECSV(basexClient, xQueryParams)
        }

    suspend fun getOverview(basexClient: IBaseXClient, germ: GermType, xQueryParams: XQueryParams) =
        when (germ) {
            GermType.MRSA -> getMRSAOverview(basexClient, xQueryParams)
            GermType.MRGN -> getMRGNOverview(basexClient, xQueryParams)
            GermType.VRE -> getVREOverview(basexClient, xQueryParams)
        }

    suspend fun getMRSACSV(baseXClient: IBaseXClient, xQueryParams: XQueryParams): List<Map<String, String>> {
        val mrsaList = baseXClient.executeXQuery(BaseXQueries.applyParams(BaseXQueries.getMRSA(), xQueryParams))
        return parseCsv(
            mrsaList,
            listOf(
                "FallID",
                "Abnahmezeitpunkt",
                "Probeart",
                "Infektion",
                "nosokomial?",
                "Einsender",
                "Fachabteilung zum Abnahmezeitpunkt",
                "Spa",
                "ClusterType"
            )
        )
    }

    suspend fun getMRGACSV(baseXClient: IBaseXClient, xQueryParams: XQueryParams): List<Map<String, String>> {
        val mrgaList = baseXClient.executeXQuery(BaseXQueries.applyParams(BaseXQueries.getMRGN(), xQueryParams))
        return parseCsv(
            mrgaList,
            listOf(
                "FallID",
                "Abnahmezeitpunkt",
                "Probenart",
                "Einsender",
                "Fachabteilung zum Abnahmezeitpunkt",
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

    suspend fun getVRECSV(baseXClient: IBaseXClient, xQueryParams: XQueryParams): List<Map<String, String>> {
        val vreList = baseXClient.executeXQuery(BaseXQueries.applyParams(BaseXQueries.getVRE(), xQueryParams))
        return parseCsv(
            vreList,
            listOf(
                "FallID",
                "Abnahmezeitpunkt",
                "Probenart",
                "Einsender",
                "Fachabteilung zum Abnahmezeitpunkt",
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

    suspend fun getGlobalStatistics(baseXClient: IBaseXClient, xQueryParams: XQueryParams): List<OverviewEntry> {
        return listOf(
            createBaseXOverviewEntry(
                "stationäre Fälle gesamt pro Erfassungszeitraum",
                BaseXQueries.getFallzahlen(),
                baseXClient,
                xQueryParams
            ),
            createBaseXOverviewEntry(
                "stationäre Falltage gesamt pro Erfassungszeitraum",
                BaseXQueries.getFalltage(),
                baseXClient,
                xQueryParams
            ),
        )
    }

    suspend fun getMRSAOverview(baseXClient: IBaseXClient, xQueryParams: XQueryParams): List<OverviewEntry> {
        val fallliste = getMRSACSV(baseXClient, xQueryParams)
        val mrsaTotal = DataProcessor.countMRSATotal(fallliste)
        val mrsaNosokomial = DataProcessor.countMRSANosokomial(fallliste)
        val mrsaImported = DataProcessor.countMRSAImported(fallliste)

        return listOf(
            createBaseXOverviewEntry(
                "Anzahl der Nasenabstriche bzw. kombinierte Nasen/Rachenabstiche pro Erfassungszeitraum",
                BaseXQueries.getNasenRachenAbstriche(),
                baseXClient, xQueryParams
            ),
            createBaseXOverviewEntry(
                "Anzahl aller S. aureus aus Blutkulturen (MSSA und MRSA)",
                BaseXQueries.getMSSABK(),
                baseXClient,
                xQueryParams
            ),
            createBaseXOverviewEntry(
                "Anzahl MRSA aus Blutkulturen",
                BaseXQueries.getMRSABK(),
                baseXClient,
                xQueryParams
            ),
            OverviewEntry(
                "Gesamtanzahl aller Fälle mit Methicillin Resistenten S. aureus (MRSA)",
                BaseXQueries.getMRSA(),
                "$mrsaTotal"
            ),
            OverviewEntry("Anzahl der importierten MRSA Fälle", BaseXQueries.getMRSA(), "$mrsaImported"),
            OverviewEntry("Anzahl nosokomialer MRSA Fälle", BaseXQueries.getMRSA(), "$mrsaNosokomial"),
            OverviewEntry("stationäre Falltage von MRSA-Fällen", BaseXQueries.getFallzahlen(), "TODO") //TODO
        )
    }

    suspend fun getMRGNOverview(baseXClient: IBaseXClient, xQueryParams: XQueryParams): List<OverviewEntry> {
        val fallliste = getMRGACSV(baseXClient, xQueryParams)
        val mrgn3Cases = DataProcessor.countMRGN3Cases(fallliste)
        val mrgn4Cases = DataProcessor.countMRGN4Cases(fallliste)

        return listOf(
            OverviewEntry("Anzahl der 3MRGN Fälle", BaseXQueries.getMRGN(), "$mrgn3Cases"),
            OverviewEntry("Anzahl der 4MRGN Fälle", BaseXQueries.getMRGN(), "$mrgn4Cases"),
        )
    }

    suspend fun getVREOverview(baseXClient: IBaseXClient, xQueryParams: XQueryParams): List<OverviewEntry> {
        suspend fun entry(name: String, query: String) =
            createBaseXOverviewEntry(name, query, baseXClient, xQueryParams)

        val caseList = getVRECSV(baseXClient, xQueryParams)
        val numEfaecalisResistant = DataProcessor.countVREEfaecalisResistant(caseList)
        val numEfaeciumTotal = DataProcessor.countVREEfaeciumTotal(caseList)
        val numEfaeciumResistant = DataProcessor.countVREEfaeciumResistant(caseList)
        val numOtherCases = DataProcessor.countOtherCases(caseList)

        return listOf(
            entry("Anzahl der gesamten E.faecalis Fälle (resistente und sensible)", BaseXQueries.getAnzahlEFaecalis()),
            OverviewEntry("Anzahl der VRE E.faecalis Fälle", BaseXQueries.getVRE(), "$numEfaecalisResistant"),
            OverviewEntry(
                "Anzahl der gesamten E.faecium Fälle (resistente und sensible)",
                BaseXQueries.getVRE(),
                "$numEfaeciumTotal"
            ),
            OverviewEntry("Anzahl der VRE E.faecium Fälle", BaseXQueries.getVRE(), "$numEfaeciumResistant"),
            OverviewEntry("Anzahl sonstiger VRE Fälle", BaseXQueries.getVRE(), "$numOtherCases"),
            entry(
                "Anzahl E.faecium Fälle (inkl. Vancomycin empfindliche und resistente Isolate) in Blutkulturen (Angabe nur einer 1 Kultur pro Patient)",
                BaseXQueries.getEfaeciumBK()
            ),
            entry(
                "Anzahl der VRE-E.faecium Fälle in Blutkulturen (Angabe nur einer 1 Kultur pro Patient)",
                BaseXQueries.getVREBK()
            )
        )
    }

    suspend fun createBaseXOverviewEntry(
        name: String,
        query: String,
        baseXClient: IBaseXClient,
        xQueryParams: XQueryParams
    ): OverviewEntry {
        val result = baseXClient.executeXQuery(query)
        val query2 = BaseXQueries.applyParams(query, xQueryParams)
        return OverviewEntry(name, query2, result)
    }
}
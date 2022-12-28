package model

import de.uni_muenster.imi.oegd.common.parseCsv
import java.time.LocalDateTime

object DataProvider {
    //TODO: Create constructor with BaseXClient?
    suspend fun getGermInfo(basexClient: IBaseXClient, germ: GermType, xQueryParams: XQueryParams): GermInfo {
        when (germ) {
            GermType.MRSA -> {
                val caseList = getMRSACSV(basexClient, xQueryParams)
                val overviewEntry = getMRSAOverview(basexClient, xQueryParams, caseList)
                return GermInfo(germ.germtype, overviewEntry, caseList, LocalDateTime.now().toString())
            }

            GermType.MRGN -> {
                val caseList = getMRGNCSV(basexClient, xQueryParams)
                val overviewEntry = getMRGNOverview(basexClient, xQueryParams, caseList)
                return GermInfo(germ.germtype, overviewEntry, caseList, LocalDateTime.now().toString())
            }

            GermType.VRE -> {
                val caseList = getVRECSV(basexClient, xQueryParams)
                val overviewEntry = getVREOverview(basexClient, xQueryParams, caseList)
                return GermInfo(germ.germtype, overviewEntry, caseList, LocalDateTime.now().toString())
            }
        }
    }


    suspend fun getMRSACSV(baseXClient: IBaseXClient, xQueryParams: XQueryParams): List<Map<String, String>> {
        val mrsaList = baseXClient.executeXQuery(BaseXQueries.applyParams(BaseXQueries.MRSA, xQueryParams))
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

    suspend fun getMRGNCSV(baseXClient: IBaseXClient, xQueryParams: XQueryParams): List<Map<String, String>> {
        val mrgnList = baseXClient.executeXQuery(BaseXQueries.applyParams(BaseXQueries.MRGN, xQueryParams))
        return parseCsv(
            mrgnList,
            listOf(
                "FallID",
                "Abnahmezeitpunkt",
                "Probenart",
                "Einsender",
                "Fachabteilung zum Abnahmezeitpunkt",
                "Erreger",
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
        val vreList = baseXClient.executeXQuery(BaseXQueries.applyParams(BaseXQueries.VRE, xQueryParams))
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

    suspend fun getGlobalStatistics(baseXClient: IBaseXClient, xQueryParams: XQueryParams): GlobalInfo {
        return GlobalInfo(
            listOf(
                createBaseXOverviewEntry(
                    "stationäre Fälle gesamt pro Erfassungszeitraum",
                    BaseXQueries.Fallzahlen,
                    baseXClient,
                    xQueryParams
                ),
                createBaseXOverviewEntry(
                    "stationäre Falltage gesamt pro Erfassungszeitraum",
                    BaseXQueries.Falltage,
                    baseXClient,
                    xQueryParams
                ),
            ), LocalDateTime.now().toString()
        )
    }

    suspend fun getMRSAOverview(
        baseXClient: IBaseXClient,
        xQueryParams: XQueryParams,
        caseList: List<Map<String, String>>
    ): List<OverviewEntry> {
        val mrsaTotal = DataProcessor.countMRSATotal(caseList)
        val mrsaNosokomial = DataProcessor.countMRSANosokomial(caseList)
        val mrsaImported = DataProcessor.countMRSAImported(caseList)

        return listOf(
            createBaseXOverviewEntry(
                "Anzahl der Nasenabstriche bzw. kombinierte Nasen/Rachenabstiche pro Erfassungszeitraum",
                BaseXQueries.NasenRachenAbstriche,
                baseXClient, xQueryParams
            ),
            createBaseXOverviewEntry(
                "Anzahl aller S. aureus aus Blutkulturen (MSSA und MRSA)",
                BaseXQueries.MSSABK,
                baseXClient,
                xQueryParams
            ),
            createBaseXOverviewEntry(
                "Anzahl MRSA aus Blutkulturen",
                BaseXQueries.MRSABK,
                baseXClient,
                xQueryParams
            ),
            OverviewEntry(
                "Gesamtanzahl aller Fälle mit Methicillin Resistenten S. aureus (MRSA)",
                BaseXQueries.MRSA,
                "$mrsaTotal"
            ),
            OverviewEntry("Anzahl der importierten MRSA Fälle", BaseXQueries.MRSA, "$mrsaImported"),
            OverviewEntry("Anzahl nosokomialer MRSA Fälle", BaseXQueries.MRSA, "$mrsaNosokomial"),
            createBaseXOverviewEntry(
                "stationäre Falltage von MRSA-Fällen",
                BaseXQueries.FalltageMRSA,
                baseXClient,
                xQueryParams
            )
        )
    }

    suspend fun getMRGNOverview(
        baseXClient: IBaseXClient,
        xQueryParams: XQueryParams,
        caseList: List<Map<String, String>>
    ): List<OverviewEntry> {
        val mrgn3Cases = DataProcessor.countMRGN3Cases(caseList)
        val mrgn4Cases = DataProcessor.countMRGN4Cases(caseList)

        return listOf(
            OverviewEntry("Anzahl der 3MRGN Fälle", BaseXQueries.MRGN, "$mrgn3Cases"),
            OverviewEntry("Anzahl der 4MRGN Fälle", BaseXQueries.MRGN, "$mrgn4Cases"),
        )
    }

    suspend fun getVREOverview(
        baseXClient: IBaseXClient, xQueryParams: XQueryParams, caseList: List<Map<String, String>>
    ): List<OverviewEntry> {
        suspend fun entry(name: String, query: String) =
            createBaseXOverviewEntry(name, query, baseXClient, xQueryParams)

        val numEfaecalisResistant = DataProcessor.countVREEfaecalisResistant(caseList)
        val numEfaeciumTotal = DataProcessor.countVREEfaeciumTotal(caseList)
        val numEfaeciumResistant = DataProcessor.countVREEfaeciumResistant(caseList)
        val numOtherCases = DataProcessor.countOtherCases(caseList)

        return listOf(
            entry("Anzahl der gesamten E.faecalis Fälle (resistente und sensible)", BaseXQueries.AnzahlEFaecalis),
            OverviewEntry("Anzahl der VRE E.faecalis Fälle", BaseXQueries.VRE, "$numEfaecalisResistant"),
            OverviewEntry(
                "Anzahl der gesamten E.faecium Fälle (resistente und sensible)",
                BaseXQueries.VRE,
                "$numEfaeciumTotal"
            ),
            OverviewEntry("Anzahl der VRE E.faecium Fälle", BaseXQueries.VRE, "$numEfaeciumResistant"),
            OverviewEntry("Anzahl sonstiger VRE Fälle", BaseXQueries.VRE, "$numOtherCases"),
            entry(
                "Anzahl E.faecium Fälle (inkl. Vancomycin empfindliche und resistente Isolate) in Blutkulturen (Angabe nur einer 1 Kultur pro Patient)",
                BaseXQueries.EfaeciumBK
            ),
            entry(
                "Anzahl der VRE-E.faecium Fälle in Blutkulturen (Angabe nur einer 1 Kultur pro Patient)",
                BaseXQueries.VREBK
            )
        )
    }

    suspend fun createBaseXOverviewEntry(
        name: String,
        query: String,
        baseXClient: IBaseXClient,
        xQueryParams: XQueryParams
    ): OverviewEntry {
        val query2 = BaseXQueries.applyParams(query, xQueryParams)
        val result = baseXClient.executeXQuery(query2)
        return OverviewEntry(name, query2, result)
    }
}
package de.uni_muenster.imi.oegd.testdataGenerator

import  de.uni_muenster.imi.oegd.testdataGenerator.AntibioticType.*

fun getMRSAAntibiotics(): List<AntibioticType> {
    return listOf(
        AMOXICILLIN_CLAVULANSAEURE, AMPICILLIN_SULBACTAM, AZITHROMYCIN, BENZYLPENICILLIN, CEFACLOR, CEFAZOLIN,
        CEFOXITIN, CLARITHROMYCIN, CLINDAMYCIN, DAPTOMYCIN, ERYTHROMYCIN, FOSFOMYCIN, FUSIDINSAEURE, GENTAMICIN,
        IMIPENEM, INDUCED_CLINDAMYCIN, LEVOFLOXACIN, LINEZOLID, MEROPENEM, MUPIROCIN, OXACILLIN, PIPERACILLIN,
        PIPERACILLIN_TAZOBACTAM, RIFAMPICIN, TEICOPLANIN, TETRACYCLIN, TIGECYCLIN, TRIMETHOPRIM_SULFAMETHOXAZOL,
        VANCOMYCIN
    )
}

fun getMRGNAntibiotics(): List<AntibioticType> {
    return listOf(PIPERACILLIN_TAZOBACTAM, CEFOTAXIM, CEFTAZIDIM, CEFEPIM, MEROPENEM, IMIPENEM, CIPROFLOXACIN)
}

fun getMRGNGermTypes(): List<GermType> {
    return listOf(
        GermType.A_BAUMANNII, GermType.E_COLI,
        GermType.E_HERMANNII, GermType.K_AEROGENES,
        GermType.K_OXYTOCA, GermType.M_MORGANII,
        GermType.P_AERUGINOSA, GermType.P_MIRABILIS
    )
}

fun getVREGermTypes(): List<GermType> {
    return listOf(
        GermType.E_FAECIUM, GermType.E_FAECALIS
    )
}

fun getVREAntibiotics(): List<AntibioticType> {
    return listOf(LINEZOLID, TIGECYCLIN, VANCOMYCIN, TEICOPLANIN, QUINUPRISTIN_DALFOPRISTIN)
}

fun getSensibleOrIntermediaryRandomly(): AntibioticsResult {
    return listOf(AntibioticsResult.SENSIBLE, AntibioticsResult.INTERMEDIARY).random()
}

interface AntibioticsProbability {
    val antibioticType: AntibioticType
    val rProbability: Double
    val sProbability: Double
    val iProbability: Double
}

enum class VREAntibioticsProbability(
    override val antibioticType: AntibioticType,
    override val rProbability: Double,
    override val sProbability: Double,
    override val iProbability: Double
) : AntibioticsProbability {
    LINEZOLID(AntibioticType.LINEZOLID, 0.05, 0.95, 0.0),
    TIGECYCLIN(AntibioticType.TIGECYCLIN, 0.05, 0.95, 0.0),
    VANCOMYCIN(AntibioticType.VANCOMYCIN, 1.0, 0.0, 0.0),
    TEICOPLANIN(AntibioticType.TEICOPLANIN, 0.37, 0.63, 0.0),
    QUINUPRISTIN(QUINUPRISTIN_DALFOPRISTIN, 0.03, 0.72, 0.24)
}
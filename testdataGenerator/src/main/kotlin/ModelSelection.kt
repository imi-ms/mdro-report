package de.uni_muenster.imi.oegd.testdataGenerator

import  de.uni_muenster.imi.oegd.testdataGenerator.AntibioticType.*
import de.uni_muenster.imi.oegd.testdataGenerator.GermType.*

val MRSAAntibiotics = listOf(
    AMOXICILLIN_CLAVULANSAEURE, AMPICILLIN_SULBACTAM, AZITHROMYCIN, BENZYLPENICILLIN, CEFACLOR, CEFAZOLIN,
    CEFOXITIN, CLARITHROMYCIN, CLINDAMYCIN, DAPTOMYCIN, ERYTHROMYCIN, FOSFOMYCIN, FUSIDINSAEURE, GENTAMICIN,
    IMIPENEM, INDUCED_CLINDAMYCIN, LEVOFLOXACIN, LINEZOLID, MEROPENEM, MUPIROCIN, OXACILLIN, PIPERACILLIN,
    PIPERACILLIN_TAZOBACTAM, RIFAMPICIN, TEICOPLANIN, TETRACYCLIN, TIGECYCLIN, TRIMETHOPRIM_SULFAMETHOXAZOL,
    VANCOMYCIN
)

val MRGNAntibiotics =
    listOf(PIPERACILLIN_TAZOBACTAM, CEFOTAXIM, CEFTAZIDIM, CEFEPIM, MEROPENEM, IMIPENEM, CIPROFLOXACIN)

val MRGNGermTypes =
    listOf(A_BAUMANNII, E_COLI, E_HERMANNII, K_AEROGENES, K_OXYTOCA, M_MORGANII, P_AERUGINOSA, P_MIRABILIS)

val VREGermTypes = listOf(E_FAECIUM, E_FAECALIS)

val VREAntibiotics = listOf(LINEZOLID, TIGECYCLIN, VANCOMYCIN, TEICOPLANIN, QUINUPRISTIN_DALFOPRISTIN)

fun getSensibleOrIntermediaryRandomly() = listOf(AntibioticsResult.SENSIBLE, AntibioticsResult.INTERMEDIARY).random()

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
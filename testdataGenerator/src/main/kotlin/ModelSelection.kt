package de.uni_muenster.imi.oegd.testdataGenerator

fun getMRSAAntibiotics(): List<AntibioticType> {
    return listOf(
        AntibioticType.AMOXICILLIN_CLAVULANSAEURE, AntibioticType.AMPICILLIN_SULBACTAM,
        AntibioticType.AZITHROMYCIN, AntibioticType.BENZYLPENICILLIN,
        AntibioticType.CEFACLOR, AntibioticType.CEFAZOLIN,
        AntibioticType.CEFOXITIN, AntibioticType.CLARITHROMYCIN,
        AntibioticType.CLINDAMYCIN, AntibioticType.DAPTOMYCIN,
        AntibioticType.ERYTHROMYCIN, AntibioticType.FOSFOMYCIN,
        AntibioticType.FUSIDINSAEURE, AntibioticType.GENTAMICIN,
        AntibioticType.IMIPENEM, AntibioticType.INDUCED_CLINDAMYCIN,
        AntibioticType.LEVOFLOXACIN, AntibioticType.LINEZOLID,
        AntibioticType.MEROPENEM, AntibioticType.MUPIROCIN,
        AntibioticType.OXACILLIN, AntibioticType.PIPERACILLIN,
        AntibioticType.PIPERACILLIN_TAZOBACTAM, AntibioticType.RIFAMPICIN,
        AntibioticType.TEICOPLANIN, AntibioticType.TETRACYCLIN,
        AntibioticType.TIGECYCLIN, AntibioticType.TRIMETHOPRIM_SULFAMETHOXAZOL,
        AntibioticType.VANCOMYCIN
    )
}

fun getMRGNAntibiotics(): List<AntibioticType> {
    return listOf(
        AntibioticType.PIPERACILLIN_TAZOBACTAM, AntibioticType.CEFOTAXIM,
        AntibioticType.CEFTAZIDIM, AntibioticType.CEFEPIM,
        AntibioticType.MEROPENEM, AntibioticType.IMIPENEM,
        AntibioticType.CIPROFLOXACIN

    )
}

fun getMRGNGermTypes(): List<GermType> {
    return listOf(
        GermType.E_COLI, GermType.E_HERMANNII,
        GermType.K_AEROGENES, GermType.K_OXYTOCA,
        GermType.M_MORGANII, GermType.P_AERUGINOSA,
        GermType.P_MIRABILIS
    )
}

fun getEnterobacteralesGerms(): List<GermType> {
    return listOf(
        GermType.E_COLI, GermType.E_HERMANNII,
        GermType.K_AEROGENES, GermType.K_OXYTOCA,
        GermType.M_MORGANII, GermType.P_MIRABILIS
    )
}

fun getVREGermTypes(): List<GermType> {
    return listOf(
        GermType.E_FAECIUM, GermType.E_FAECALIS
    )
}

fun getVREAntibiotics(): List<AntibioticType> {
    return listOf(
        AntibioticType.LINEZOLID, AntibioticType.TIGECYCLIN,
        AntibioticType.VANCOMYCIN, AntibioticType.TEICOPLANIN,
        AntibioticType.QUINUPRISTIN_DALFOPRISTIN
    )
}

fun getSensibleOrIntermediaryRandomly(): AntibioticsResult {
    return listOf(AntibioticsResult.SENSIBLE, AntibioticsResult.INTERMEDIARY).random()
}
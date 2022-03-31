package de.uni_muenster.imi.oegd.testdataGenerator

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random

fun generateStartAndEnddate(startTimeRange: LocalDate, endTimeRange: LocalDate): CaseDate {
    val startEpochDay = startTimeRange.toEpochDay()
    val endEpochDay = endTimeRange.toEpochDay()
    val randomStartDay = LocalDate.ofEpochDay(
        ThreadLocalRandom
            .current()
            .nextLong(startEpochDay, endEpochDay)
    )
    val randomEndDay = randomStartDay
        .plusWeeks(Random.nextLong(1, 4))
        .plusDays(Random.nextLong(0, 6))

    val randomStartTime = LocalTime.of(
        Random.nextInt(1, 24),
        Random.nextInt(1, 60),
        Random.nextInt(1, 60)
    )

    val randomEndTime = LocalTime.of(
        Random.nextInt(1, 24),
        Random.nextInt(1, 60),
        Random.nextInt(1, 60)
    )

    val startDateTime = LocalDateTime.of(randomStartDay, randomStartTime)
    val endDateTime = LocalDateTime.of(randomEndDay, randomEndTime)

    return CaseDate(startDateTime, endDateTime)
}

data class CaseDate(val startTimeDate: LocalDateTime, val endDateTime: LocalDateTime)


fun generateAntibioticsAnalysis(caseInfo: CaseInfo): List<AntibioticsAnalysis> {
    return when (caseInfo.caseScope) {
        CaseScope.MRSA -> generateRandomAntibioticsAnalysis(getMRSAAntibiotics()) //TODO: Add MRSA Logic
        CaseScope.MRGN3 -> generateMRGNAntibioticsAnalysis(3, getMRGNAntibiotics(), caseInfo.germType)
        CaseScope.MRGN4 -> generateMRGNAntibioticsAnalysis(4, getMRGNAntibiotics(), caseInfo.germType)
        CaseScope.VRE -> generateVREAntibioticsAnalysis(getVREAntibiotics())
    }
}

fun generateRandomAntibioticsAnalysis(antibiotics: List<AntibioticType>): List<AntibioticsAnalysis> {
    val result: MutableList<AntibioticsAnalysis> = mutableListOf()
    for (antibiotic in antibiotics) {
        val randomResult = AntibioticsResult.values().random()
        result.add(AntibioticsAnalysis(antibiotic, randomResult))
    }
    return result
}

fun generateMRGNAntibioticsAnalysis(
    numberOfResistances: Int,
    antibiotics: List<AntibioticType>,
    germType: GermType
): List<AntibioticsAnalysis> {
    when (numberOfResistances) {
        3 -> {
            when (germType) {
                in getEnterobacteralesGerms() -> return getEnterobacterAntibioticsAnalysis(antibiotics)
                GermType.P_AERUGINOSA -> return getPseudomonasAntibioticsAnalysis(antibiotics.toMutableList())
                //TODO: Add Acinetobacter baumannii
                else -> {} //TODO: Fallback?
            }
        }
        4 -> return getResistantAntibioticsAnalysis(antibiotics)
    }
    return listOf()
}

fun getEnterobacterAntibioticsAnalysis(antibiotics: List<AntibioticType>): List<AntibioticsAnalysis> {
    val result = mutableListOf<AntibioticsAnalysis>()
    antibiotics.forEach {
        when (it) {
            AntibioticType.IMIPENEM, AntibioticType.MEROPENEM -> {
                result.add(AntibioticsAnalysis(it, getSensibleOrIntermediaryRandomly()))
            }
            else -> {
                result.add(AntibioticsAnalysis(it, AntibioticsResult.RESISTANT))
            }
        }
    }
    return result
}

fun getPseudomonasAntibioticsAnalysis(antibiotics: MutableList<AntibioticType>): List<AntibioticsAnalysis> {
    val result = mutableListOf<AntibioticsAnalysis>()

    val randomSelection = antibiotics.random()
    antibiotics.remove(randomSelection)

    result.add(AntibioticsAnalysis(randomSelection, getSensibleOrIntermediaryRandomly()))

    for (antibiotic in antibiotics) {
        result.add(AntibioticsAnalysis(antibiotic, AntibioticsResult.RESISTANT))
    }

    return result
}

fun generateVREAntibioticsAnalysis(antibiotics: List<AntibioticType>): List<AntibioticsAnalysis> {
    val result = mutableListOf<AntibioticsAnalysis>()
    for (antibiotic in antibiotics) {
        when (antibiotic) {
            AntibioticType.LINEZOLID -> result.add(
                getRandomAntibioticsAnalysisWithProbability(VREAntibioticsProbability.LINEZOLID)
            )
            AntibioticType.TIGECYCLIN -> result.add(
                getRandomAntibioticsAnalysisWithProbability(VREAntibioticsProbability.TIGECYCLIN)
            )
            AntibioticType.VANCOMYCIN -> result.add(
                getRandomAntibioticsAnalysisWithProbability(VREAntibioticsProbability.VANCOMYCIN)
            )
            AntibioticType.TEICOPLANIN -> result.add(
                getRandomAntibioticsAnalysisWithProbability(VREAntibioticsProbability.TEICOPLANIN)
            )
            AntibioticType.QUINUPRISTIN_DALFOPRISTIN -> result.add(
                getRandomAntibioticsAnalysisWithProbability(VREAntibioticsProbability.QUINUPRISTIN)
            )
            else -> result.add(AntibioticsAnalysis(antibiotic, AntibioticsResult.UNKNOWN))
        }
    }
    return result
}

fun getResistantAntibioticsAnalysis(antibiotics: List<AntibioticType>): List<AntibioticsAnalysis> {
    val result = mutableListOf<AntibioticsAnalysis>()
    antibiotics.forEach {
        result.add(AntibioticsAnalysis(it, AntibioticsResult.RESISTANT))
    }
    return result
}

fun getRandomTypeWithProbability(typeList: List<ProbabilityEnum>): ProbabilityEnum? {
    val p = Random.nextDouble(0.0, 1.0)
    var cumulativeProbability = 0.0

    for (type in typeList) {
        cumulativeProbability += type.relativeProbability
        if (p <= cumulativeProbability) {
            return type
        }
    }
    return null //Should never be reached
}

fun getRandomAntibioticsAnalysisWithProbability(antibioticWithProbability: AntibioticsProbability): AntibioticsAnalysis {
    val p = Random.nextDouble(0.0, 1.0)
    var cumulativeProbability = 0.0

    cumulativeProbability += antibioticWithProbability.sProbability
    if (p <= cumulativeProbability) {
        return AntibioticsAnalysis(antibioticWithProbability.antibioticType, AntibioticsResult.SENSIBLE)
    }

    cumulativeProbability += antibioticWithProbability.rProbability
    if (p <= cumulativeProbability) {
        return AntibioticsAnalysis(antibioticWithProbability.antibioticType, AntibioticsResult.RESISTANT)
    }

    cumulativeProbability += antibioticWithProbability.iProbability
    if (p <= cumulativeProbability) {
        return AntibioticsAnalysis(antibioticWithProbability.antibioticType, AntibioticsResult.INTERMEDIARY)
    }

    //Should never be reached if probabilities are correct
    return AntibioticsAnalysis(antibioticWithProbability.antibioticType, AntibioticsResult.UNKNOWN)
}
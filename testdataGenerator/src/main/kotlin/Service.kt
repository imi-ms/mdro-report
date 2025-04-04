package de.uni_muenster.imi.oegd.testdataGenerator

import de.uni_muenster.imi.oegd.testdataGenerator.AntibioticType.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.random.Random

fun generateStartAndEnddate(startTimeRange: LocalDate, endTimeRange: LocalDate): Pair<LocalDateTime, LocalDateTime> {
    val randomStartDay = LocalDate.ofEpochDay(
        Random.nextLong(startTimeRange.toEpochDay(), endTimeRange.toEpochDay())
    )
    val randomEndDay = randomStartDay
        .plusWeeks(Random.nextLong(1, 4))
        .plusDays(Random.nextLong(0, 6))

    val startDateTime = LocalDateTime.of(randomStartDay, randomTime())
    val endDateTime = LocalDateTime.of(randomEndDay, randomTime())

    return Pair(startDateTime, endDateTime)
}

private fun randomTime() = LocalTime.of(
    Random.nextInt(1, 24),
    Random.nextInt(1, 60),
    Random.nextInt(1, 60)
)


fun generateAntibioticsAnalysis(caseInfo: CaseInfo): List<AntibioticsAnalysis> {
    return when (caseInfo.caseScope) {
        CaseScope.MRSA -> generateRandomAntibioticsAnalysis(MRSAAntibiotics) //TODO: Add Logic
        CaseScope.MRGN3 -> generateMRGNAntibioticsAnalysis(3, MRGNAntibiotics, caseInfo.germType)
        CaseScope.MRGN4 -> generateMRGNAntibioticsAnalysis(4, MRGNAntibiotics, caseInfo.germType)
        CaseScope.VRE -> generateVREAntibioticsAnalysis(caseInfo, VREAntibiotics)
    }
}

fun generateRandomAntibioticsAnalysis(antibiotics: List<AntibioticType>): List<AntibioticsAnalysis> {
    return antibiotics.map {
        AntibioticsAnalysis(it, AntibioticsResult.entries.random())
    }
}

fun generateMRGNAntibioticsAnalysis(
    numberOfResistances: Int,
    antibiotics: List<AntibioticType>,
    germType: GermType
): List<AntibioticsAnalysis> {
    return when (numberOfResistances) {
        3 -> when (germType) {
            GermType.P_AERUGINOSA -> generatePseudomonasAntibioticsAnalysis(antibiotics.toMutableList())
            else -> generateDefaultMRGNAntibioticsAnalysis(antibiotics)
        }
        4 -> generateResistantAntibioticsAnalysis(antibiotics)
        else -> emptyList()
    }
}

fun generateDefaultMRGNAntibioticsAnalysis(antibiotics: List<AntibioticType>): List<AntibioticsAnalysis> {
    return antibiotics.map {
        when (it) {
            IMIPENEM, MEROPENEM -> AntibioticsAnalysis(it, getSensibleOrIntermediaryRandomly())
            else -> AntibioticsAnalysis(it, AntibioticsResult.RESISTANT)
        }
    }
}

fun generatePseudomonasAntibioticsAnalysis(antibiotics: MutableList<AntibioticType>): List<AntibioticsAnalysis> {
    val randomSelection = antibiotics.random()
    antibiotics.remove(randomSelection)

    val result = mutableListOf<AntibioticsAnalysis>()
    result.add(AntibioticsAnalysis(randomSelection, getSensibleOrIntermediaryRandomly()))

    for (antibiotic in antibiotics) {
        result.add(AntibioticsAnalysis(antibiotic, AntibioticsResult.RESISTANT))
    }

    return result
}

fun generateVREAntibioticsAnalysis(caseInfo: CaseInfo, antibiotics: List<AntibioticType>): List<AntibioticsAnalysis> {
    return antibiotics.map {
        when (it) {
            LINEZOLID -> getRandomAntibioticsAnalysisWithProbability(VREAntibioticsProbability.LINEZOLID)
            TIGECYCLIN -> getRandomAntibioticsAnalysisWithProbability(VREAntibioticsProbability.TIGECYCLIN)
            VANCOMYCIN -> if (caseInfo.germType == GermType.E_FAECALIS) {
                AntibioticsAnalysis(VANCOMYCIN, AntibioticsResult.SENSIBLE) // falls E.faecalis immer sensibel
            } else getRandomAntibioticsAnalysisWithProbability(VREAntibioticsProbability.VANCOMYCIN)

            TEICOPLANIN -> getRandomAntibioticsAnalysisWithProbability(VREAntibioticsProbability.TEICOPLANIN)
            QUINUPRISTIN_DALFOPRISTIN -> getRandomAntibioticsAnalysisWithProbability(VREAntibioticsProbability.QUINUPRISTIN)
            else -> AntibioticsAnalysis(it, AntibioticsResult.UNKNOWN)
        }
    }
}

fun generateResistantAntibioticsAnalysis(antibiotics: List<AntibioticType>): List<AntibioticsAnalysis> {
    return antibiotics.map { AntibioticsAnalysis(it, AntibioticsResult.RESISTANT) }
}

fun <T : ProbabilityEnum> getRandomTypeWithProbability(typeList: List<T>): T {
    val p = Random.nextDouble(0.0, typeList.sumOf { it.relativeProbability })
    var cumulativeProbability = 0.0
    for (type in typeList) {
        cumulativeProbability += type.relativeProbability
        if (p <= cumulativeProbability) {
            return type
        }
    }
    error("Should never be reached")
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
package de.uni_muenster.imi.oegd.testdataGenerator
import org.redundent.kotlin.xml.Node
import org.redundent.kotlin.xml.xml
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random


class TestdataGenerator {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            fun askUser(message: String): String {
                println(message)
                return readLine()!!
            }

            if (!File("testdata").exists()) {
                File("testdata").mkdir()
            }

            for (i in 1..Integer.parseInt(askUser("How many patients should be generated?"))) {
                val result = createPatient()
                File("testdata/Patient$i").writeText(result)
            }
        }
    }
}

private val unusedIds = (1000000..99999999).toMutableList()
private val startTimeRange = LocalDate.of(2021,1,1)
private val endTimeRange = LocalDate.of(2022,2,28)



fun createTestdata(numberOfTestdata: Int): List<String> {
    val result = mutableListOf<String>()
    for(i in 1..numberOfTestdata) {
        result.add(createPatient())
    }
    return result
}

fun createPatient(): String {
    val patient = xml("patient") {
        attribute("birthYear", "${Random.nextInt(1940, 2010)}")
        attribute("sex", "${if (Random.nextBoolean()) 'F' else 'M'}")
        attribute("id", "${getUniqueId()}")
        addNode(createMRSACase())
    }
    return patient.toString(true)
}

fun createMRSACase(): Node {
    val randomClinic = Department.values().random()
    val startAndEndday = generateStartAndEnddate()
    val randomBodySite = SmearType.values().random()
    val germ = GermType.S_AUREUS
    val antibiotics = getMRSAAntibiotics()

    val case = xml("case") {
        attribute("id", "${getUniqueId()}")
        attribute("from", "${startAndEndday.first}")
        attribute("till", "${startAndEndday.second}")
        attribute("type", "S") //TODO Implement Casetypes and Admission Reason
        "location" {
            attribute("id", "${getUniqueId()}")
            attribute("from", "${startAndEndday.first}")
            attribute("till", "${startAndEndday.second}")
            attribute("clinic", randomClinic.fa_code)
        }
        "labReport" {
            attribute("id", "${getUniqueId()}")
            attribute("source", "MIBI")
            "request" {
                attribute("from", "${startAndEndday.first.plusDays(1)}") //Request always one day after start
                attribute("sender", randomClinic.clinic)
                -"MRSA"
            }
            "sample" {
                attribute("from", "${startAndEndday.first.plusDays(1)}") //Sample always one day after start
                attribute("bodySiteDisplay", randomBodySite.bodySiteDisplay)
                attribute("display", randomBodySite.display)
                "comment" {
                    -"Nachweis MRSA"
                }
                "analysis" {
                    attribute("type", "germ")
                    attribute("OPUS", "amrsa")
                    attribute("display", "Selektivagar MRSA")
                    "result"{
                        attribute("OPUS", "positiv")
                    }
                }
                "germ" {

                    attribute("id", "${getUniqueId()}")
                    attribute("SNOMED", germ.SNOMED)
                    attribute("display", germ.display)
                    attribute("class", "MRSA")
                    "comment"{
                        attribute("class", "MRSA") //TODO: Currently xquery searches for this
                        -germ.comment
                    }
                    for(antibioticAnalysis in generateRandomAntibioticsAnalysis(antibiotics)) {
                        "antibiotic" {
                            attribute("LOINC", antibioticAnalysis.antibiotic.LOINC)
                            attribute("display", antibioticAnalysis.antibiotic.display)
                            "result" {
                                attribute("string", antibioticAnalysis.antibioticsResult.result)
                                attribute("LOINC", antibioticAnalysis.antibioticsResult.LOINC)
                            }
                        }
                    }
                }
            }
        }
    }
    return case
}

private fun generateStartAndEnddate(): Pair<LocalDateTime, LocalDateTime> {
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

    val randomStartTime = LocalTime.of(Random.nextInt(1, 24),
                                        Random.nextInt(1, 60),
                                        Random.nextInt(1, 60))

    val randomEndTime = LocalTime.of(Random.nextInt(1, 24),
        Random.nextInt(1, 60),
        Random.nextInt(1, 60))

    val startDateTime = LocalDateTime.of(randomStartDay, randomStartTime)
    val endDateTime = LocalDateTime.of(randomEndDay, randomEndTime)

    return Pair(startDateTime, endDateTime)
}

private fun getUniqueId(): Int {
    val id = unusedIds.random()
    unusedIds.remove(id)
    return id
}

//TODO: Antibiogram Logic needs more work
private fun generateRandomAntibioticsAnalysis(antibiotics: List<AntibioticType>): List<AntibioticsAnalysis> {
    val result: MutableList<AntibioticsAnalysis> = mutableListOf()

    for(antibiotic in antibiotics) {
        val randomResult = AntibioticsResult.values().random()
        result.add(AntibioticsAnalysis(antibiotic, randomResult))
    }
    return result
}

private fun getMRSAAntibiotics(): List<AntibioticType> {
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


enum class Casetype(val type: String) {
    MRSA("MRSA"),
    MRGN("MRGN"),
    VRE("VRE")
}

enum class Department(val clinic: String, val fa_code: String) {
    GYNAEKOLOGIE("Klinik für Gynäkologie", "FA_GYN"),

    HNO("Hals- Nasen- Ohrenklinik", "FA_HNO"),

    AUGE("Augenklinik", "FA_AUGE"),

    ANASESTHESIE("Anästhesie" , "FA_ANAES"),

    NEUROLOGIE("Neurologie", "FA_NEURO"),

    NEUROCHIRURGIE("Neurochirurgie", "FA_NEUCH"),

    KARDIOLOGIE("Department für Kardiologie u. Angiologie", "FA_KARD"),

    HAUT("Hautklinik", "FA_HAUT"),

    MEDIZINISCHE_KLINIK_D("Medizinische Klinik D", "FA_MEDD"),

    KINDERKLINIK("Kinderklinik, Schulkinder-Stration", "FA_KIALL")
}

enum class SmearType(val bodySiteDisplay: String, val display: String) {
    NASE("Nase", "Abstrich-oberflächlich"),

    RACHEN("Rachen", "Abstrich-oberflächlich"),

    NASE_RACHEN("Nase und Rachen", "Abstrich-oberflächlich"),

    BLUT_PERIPHER("Blut-peripher entnommen", "Blutkultur"),

    BLUT_ZENTRAL("Blut-zentral entnommen", "Blutkultur"),

    MITTELSTRAHLURIN("Mittelstrahl-Urin", "Mittelstrahl-Urin")

}

enum class GermType(val display: String, val SNOMED: String, val comment: String){
    S_AUREUS("Staphylococcus aureus", "3092008", "Nachweis von Methicillin-resistentem S.aureus (MRSA). Hygienemaßnahmen gemäß Infektionshandbuch erforderlich!"),
    P_AERUGINOSA("Pseudomonas aeruginosa", "52499004", ""),
    E_FAECALIS("Enterococcus faecalis", "78065002", ""),
    E_FAECIUM("Enterococcus faecium", "90272000", "")
}

enum class AntibioticType(val LOINC: String, val display: String) {
    AMOXICILLIN_CLAVULANSAEURE("18862-3", "Amoxicillin/Clavulansäure"),

    AMPICILLIN_SULBACTAM("18865-6", "Ampicillin/Sulbactam"),

    AZITHROMYCIN("18866-4", "Azithromycin"),

    AZTREONAM("18868-0", "Aztreonam"),

    BENZYLPENICILLIN("18964-7", "Benzylpenicillin"),

    CEFACLOR("18874-8", "Cefaclor"),

    CEFAZOLIN("18878-9", "Cefazolin"),

    CEFEPIM("18879-7", "Cefepim"),

    CEFOXITIN("18888-8", "Cefoxitin Screen"),

    CEFTAZIDIM("18893-8", "Ceftazidim"),

    CEFTOLOZAN_TAZOBACTAM("73602-5", "Ceftolozan/Tazobactam"),

    CIPROFLOXACIN("18906-8", "Ciprofloxacin"),

    CLARITHROMYCIN("18907-6", "Clarithromycin"),

    CLINDAMYCIN("18908-4", "Clindamycin"),

    COLISTIN("18912-6", "Colistin"),

    DAPTOMYCIN("35789-7", "Daptomycin"),

    ERYTHROMYCIN("18919-1", "Erythromycin"),

    FOSFOMYCIN("25596-8", "Fosfomycin"),

    FUSIDINSAEURE("18927-4", "Fusidinsäure"),

    GENTAMICIN("18928-2", "Gentamicin"),

    IMIPENEM("18932-4", "Imipenem"),

    INDUCED_CLINDAMYCIN("61188-9", "Induzierbare Clindamycin-Resistenz"),

    LEVOFLOXACIN("20629-2", "Levofloxacin"),

    LINEZOLID("29258-1", "Linezolid"),

    MEROPENEM("18943-1", "Meropenem"),

    MUPIROCIN("20389-3", "Mupirocin"),

    OXACILLIN("18961-3", "Oxacillin"),

    PIPERACILLIN("18969-6", "Piperacillin"),

    PIPERACILLIN_TAZOBACTAM("18970-4", "Piperacillin/Tazobactam"),

    RIFAMPICIN("", "Rifampicin"),

    TEICOPLANIN("18989-4", "Teicoplanin"),

    TETRACYCLIN("18993-6", "Tetracyclin"),

    TIGECYCLIN("42357-4", "Tigecyclin"),

    TOBRAMYCIN("18996-9", "Tobramycin"),

    TRIMETHOPRIM_SULFAMETHOXAZOL("18998-5", "Trimethoprim/Sulfamethoxazol"),

    VANCOMYCIN("19000-9", "Vancomycin")
}

enum class AntibioticsResult(val LOINC: String, val result: String) {
    RESISTANT("LA6676-6", "R"),
    SENSIBLE("LA24225-7", "S"),
    INTERMEDIARY("", "I")
}

data class AntibioticsAnalysis(val antibiotic: AntibioticType, val antibioticsResult: AntibioticsResult)



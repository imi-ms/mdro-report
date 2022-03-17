import org.redundent.kotlin.xml.xml
import java.time.LocalDate
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random

class TestdataGenerator {

    private val unusedIds = (1000000..99999999).toMutableList()
    private val startTimeRange = LocalDate.of(2000,1,1)
    private val endTimeRange = LocalDate.of(2022,2,28)

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val generator = TestdataGenerator()
            print(generator.createMRSAPatient())
        }
    }

    fun createMRSAPatient(): String {
        val randomClinic = FACHABTEILUNG.values().random()
        val startAndEndday = generateStartAndEnddate()

        val patient = xml("patient") {
            attribute(
                "birthYear",
                "${Random.nextInt(1940, 2022)}"
            )
            attribute(
                "sex",
                "${if(Random.nextBoolean()) 'F' else 'M'}"
            )
            attribute(
                "id",
                "${getUniqueId()}"
            )
            "case" {
                attribute(
                    "id",
                    "${getUniqueId()}"
                )
                attribute(
                    "from",
                    "${startAndEndday.first}"
                )
                attribute(
                    "till",
                    "${startAndEndday.second}"
                )
                "location" {
                    attribute(
                        "id",
                        "${getUniqueId()}"
                    )
                    attribute(
                        "from",
                        "${startAndEndday.first}"
                    )
                    attribute(
                        "till",
                        "${startAndEndday.second}"
                    )

                    attribute(
                        "clinic",
                        randomClinic.fa_code
                    )
                }
                "labReport" {
                    attribute(
                        "id",
                        "${getUniqueId()}"
                    )
                    attribute(
                        "source",
                        "MIBI"
                    )
                    "request" {
                        attribute(
                            "from",
                            "${startAndEndday.first.plusDays(1)}" //Request always one day after start
                        )
                        attribute(
                            "sender",
                            randomClinic.clinic
                        )
                        -"MRSA"
                    }
                    "sample" {
                        attribute(
                            "from",
                            "${startAndEndday.first.plusDays(1)}" //Sample always one day after start
                        )
                        val randomBodySite = SMEARTYPE.values().random()
                        attribute(
                            "bodySiteDisplay",
                            randomBodySite.bodySiteDisplay
                        )
                        attribute(
                            "display",
                            randomBodySite.display
                        )
                        "comment" {
                            -"Nachweis MRSA"
                        }
                        "analysis" {
                            attribute(
                                "type",
                                "germ"
                            )
                            attribute(
                                "OPUS",
                                "amrsa"
                            )
                            attribute(
                                "display",
                                "Selektivagar MRSA"
                            )
                            "result"{
                                attribute(
                                    "OPUS",
                                    "positiv"
                                )
                            }
                        }
                        "germ" {
                            val germ = MRSA_GERM.values().random()
                            attribute(
                                "id",
                                "${getUniqueId()}"
                            )
                            attribute(
                                "SNOMED",
                                germ.SNOMED
                            )
                            attribute(
                                "display",
                                germ.display
                            )
                            "comment"{
                                attribute(
                                    "class",
                                    "MRSA"
                                )
                                -germ.comment
                            }
                        }
                    }
                }
            }
        }
        return patient.toString(true)
    }

    private fun generateStartAndEnddate(): Pair<LocalDate, LocalDate> {
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

        return Pair(randomStartDay, randomEndDay)

    }

    private fun getUniqueId(): Int {
        val id = unusedIds.random()
        unusedIds.remove(id)
        return id
    }

}

enum class FACHABTEILUNG(val clinic: String, val fa_code: String) {
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

enum class SMEARTYPE(val bodySiteDisplay: String, val display: String) {
    NASE("Nase", "Abstrich-oberflächlich"),

    RACHEN("Rachen", "Abstrich-oberflächlich"),

    NASE_RACHEN("Nase und Rachen", "Abstrich-oberflächlich"),

    BLUT_PERIPHER("Blut-peripher entnommen", "Blutkultur"),

    BLUT_ZENTRAL("Blut-zentral entnommen", "Blutkultur"),

    MITTELSTRAHLURIN("Mittelstrahl-Urin", "Mittelstrahl-Urin")

}

enum class MRSA_GERM(val display: String, val SNOMED: String, val comment: String){
    S_AUREUS("Staphylococcus aureus", "3092008", "Nachweis von Methicillin-resistentem S.aureus (MRSA). Hygienemaßnahmen gemäß Infektionshandbuch erforderlich!"),


}

enum class ANTIBIOTICS(val LOINC: String, val display: String) {
    AMOXICILLIN_CLAVULANSAEURE("18862-3", "Amoxicillin/Clavulansäure"),

    AMPICILLIN_SULBACTAM("18865-6", "Ampicillin/Sulbactam"),

    AZITHROMYCIN("18866-4", "Azithromycin"),

    BENZYLPENICILLIN("18964-7", "Benzylpenicillin"),

    CEFACLOR("18874-8", "Cefaclor"),

    CEFAZOLIN("18878-9", "Cefazolin"),

    CEFOXITIN("18888-8", "Cefoxitin Screen"),

    CLARITHROMYCIN("18907-6", "Clarithromycin"),

    CLINDAMYCIN("18908-4", "Clindamycin"),

    DAPTOMYCIN("35789-7", "Daptomycin"),

    Erythromycin("18919-1", "Erythromycin"),

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




}



fun <T : Enum<*>?> randomEnum(clazz: Class<T>): T {
    val x: Int = Random.nextInt(clazz.enumConstants.size)
    return clazz.enumConstants[x]
}

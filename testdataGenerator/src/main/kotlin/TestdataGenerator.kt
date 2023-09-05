package de.uni_muenster.imi.oegd.testdataGenerator

import mu.KotlinLogging
import org.redundent.kotlin.xml.Node
import org.redundent.kotlin.xml.xml
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

private val log = KotlinLogging.logger { }

fun main(args: Array<String>) {
    fun askUser(message: String): String {
        println(message)
        return readLine()!!
    }

    if (!File("testdata").exists()) {
        File("testdata").mkdir()
    }

    val cnt = args.firstOrNull()?.toIntOrNull() ?: askUser("How many patients should be generated?").toInt()
    val patients = TestdataGenerator().createTestdata(cnt)
    patients.forEachIndexed { index, patient ->
        File("testdata/Patient$index").writeText(patient)
    }
}

class TestdataGenerator {
    private var startTimeRange = LocalDate.of(2021, 1, 1)
    private var endTimeRange = LocalDate.of(2022, 2, 28)

    fun setStartYear(year: Int) {
        startTimeRange = LocalDate.of(year, 1, 1)
    }

    fun setEndYear(year: Int) {
        endTimeRange = LocalDate.of(year, 1, 1).minusDays(1)
    }

    fun getStartAndEndYear(): Pair<LocalDateTime, LocalDateTime> {
        return generateStartAndEnddate(startTimeRange, endTimeRange)
    }

    fun createTestdataFile(location: String) {
        val caseScope = CaseScope.values().random()
        val patient = createPatient(caseScope)
        val id = patient.get<Int>("id").toString()
        File("$location/$id.xml").writeText(patient.toString())
    }

    fun createTestdata(numberOfTestdata: Int): List<String> {
        val result = mutableListOf<String>()
        for (i in 1..numberOfTestdata) {
            val caseScope = CaseScope.values().random()
            result.add(createPatient(caseScope).toString())
            log.info("Created new Patient with $caseScope case. Patient no. $i")
        }
        return result
    }

    fun createPatient(caseScope: CaseScope): Node {
        val caseInfo = CaseInfo(caseScope, this)
        val patient = xml("patient") {
            attribute("birthYear", Random.nextInt(1940, 2010))
            attribute("sex", if (Random.nextBoolean()) 'F' else 'M')
            attribute("id", caseInfo.patientId)
            addCase(caseInfo)
        }
        return patient
    }

    fun Node.addCase(caseInfo: CaseInfo) {
        addNode(xml("case") {
            attribute("id", caseInfo.caseId)
            attribute("from", caseInfo.startDateTime)
            attribute("till", caseInfo.endDateTime)
            attribute("type", Casetype.STATIONAER.type)
            //TODO: Add AdmissionCause and state
            "location" {
                attribute("id", caseInfo.locationId)
                attribute("from", caseInfo.startDateTime)
                attribute("till", caseInfo.endDateTime)
                attribute("clinic", caseInfo.clinic.fa_code)
            }
            "labReport" {
                attribute("id", caseInfo.labReportId)
                attribute("source", "MIBI")
                "request" {
                    attribute("from", caseInfo.requestDateTime)
                    attribute("sender", caseInfo.clinic.clinic)
                }
                "sample" {
                    attribute("from", caseInfo.requestDateTime)
                    attribute("bodySiteDisplay", caseInfo.bodySite.bodySiteDisplay)
                    attribute("display", caseInfo.bodySite.display)
                    "comment" {
                        -"No comment"
                    }
                    "germ" {
                        attribute("id", caseInfo.germId)
                        attribute("SNOMED", caseInfo.germType.SNOMED)
                        attribute("display", caseInfo.germType.display)
                        attribute("class", caseInfo.caseScope.type)
                        "comment"{
                            attribute("class", caseInfo.caseScope.type) //TODO: Currently xquery searches for this
                            -"Germ generated by Testdata Generator"
                        }
                        if (caseInfo.caseScope == CaseScope.MRSA) {
                            addPCRMetaNode("PatientID", "${caseInfo.patientId}")
                            addPCRMetaNode("CaseID", "${caseInfo.caseId}")
                            addPCRMetaNode("SampleID", "${caseInfo.sampleId}")
                            addPCRMetaNode("CollectionDate", "${caseInfo.startDateTime}")
                            addPCRMetaNode("Spa", "${caseInfo.spaType?.type}")
                            addPCRMetaNode("ClusterType", "${caseInfo.clusterType}")
                        }


                        for ((antibiotic, result) in generateAntibioticsAnalysis(caseInfo)) {
                            "antibiotic" {
                                attribute("LOINC", antibiotic.LOINC)
                                attribute("display", antibiotic.display)
                                "result" {
                                    attribute("LOINC", result.LOINC)
                                    attribute("string", result.result)
                                }
                            }
                        }
                    }
                }
            }
            if (caseInfo.caseScope == CaseScope.MRSA) {
                "hygiene-message" {
                    attribute("germ-name", caseInfo.germType.display)
                    attribute("nosocomial", "${caseInfo.nosocomial}")
                    attribute("infection", "${caseInfo.infection}")
                    attribute("MRG-class", caseInfo.caseScope)
                }
            }
        })
    }


    fun Node.addPCRMetaNode(k: String, v: Any) {
        addNode(xml("pcr-meta") {
            attribute("k", k)
            attribute("v", v)
        })
    }

}

data class CaseInfo(val caseScope: CaseScope, val generator: TestdataGenerator) {
    val patientId: Int = idGenerator.getUniqueId()
    val caseId: Int = idGenerator.getUniqueId()
    val locationId: Int = idGenerator.getUniqueId()
    val labReportId: Int = idGenerator.getUniqueId()
    val sampleId: Int = idGenerator.getUniqueId()
    val germId: Int = idGenerator.getUniqueId()

    val clinic: Department = Department.values().random()
    val bodySite: SmearType = SmearType.values().random()

    private val startAndEndDateTime = generator.getStartAndEndYear()
    val startDateTime: LocalDateTime = startAndEndDateTime.first
    val endDateTime: LocalDateTime = startAndEndDateTime.second
    val requestDateTime: LocalDateTime = startDateTime.plusDays(1) //Request always one day after start

    var germType: GermType
    var spaType: SpaType? = null
    var clusterType: ClusterType? = null
    var nosocomial: Boolean? = null
    var infection: Boolean? = null

    init {
        when (caseScope) {
            CaseScope.MRSA -> {
                germType = GermType.S_AUREUS
                spaType = getRandomTypeWithProbability(SpaType.values().toList())
                clusterType = getRandomTypeWithProbability(ClusterType.values().toList())
                nosocomial = Random.nextBoolean()
                infection = Random.nextBoolean()
            }
            CaseScope.MRGN3, CaseScope.MRGN4 -> {
                germType = MRGNGermTypes.random()
            }
            CaseScope.VRE -> {
                germType = VREGermTypes.random()
            }
        }
    }
}



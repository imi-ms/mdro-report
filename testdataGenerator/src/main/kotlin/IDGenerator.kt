package de.uni_muenster.imi.oegd.testdataGenerator

val idGenerator = IDGenerator()

class IDGenerator {
    private val unusedIds = (1000000..99999999).toMutableList()

    fun getUniqueId(): Int {
        val id = unusedIds.random()
        unusedIds.remove(id)
        return id
    }
}
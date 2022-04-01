package de.uni_muenster.imi.oegd.testdataGenerator

import kotlin.random.Random

val idGenerator = IDGenerator()

class IDGenerator {
    private val usedIds = hashSetOf<Int>()

    fun getUniqueId(): Int {
        var id: Int
        do {
            id = Random.nextInt(1000000, 99999999)
            val wasAdded = usedIds.add(id)
        } while (!wasAdded)
        return id
    }
}
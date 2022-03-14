package de.uni_muenster.imi.oegd.common

object BaseXQueries {

    fun getMRSA() = javaClass.classLoader.getResourceAsStream("queries/mrsa_excelv3.xq")!!.readBytes().toString(Charsets.UTF_8)

    fun getMRGN() = javaClass.classLoader.getResourceAsStream("queries/mrgn_excelv3.xq")!!.readBytes().toString(Charsets.UTF_8)

    fun getVRE() = javaClass.classLoader.getResourceAsStream("queries/vre_excelv2.xq")!!.readBytes().toString(Charsets.UTF_8)

    fun getFalltage() = javaClass.classLoader.getResourceAsStream("queries/Falltage.xq")!!.readBytes().toString(Charsets.UTF_8)

    fun getFallzahlen() = javaClass.classLoader.getResourceAsStream("queries/fallzahlen.xq")!!.readBytes().toString(Charsets.UTF_8)

    fun getMRSABK() = javaClass.classLoader.getResourceAsStream("queries/mrsa_bk.xq")!!.readBytes().toString(Charsets.UTF_8)

    fun getMSSABK() = javaClass.classLoader.getResourceAsStream("queries/mssa_bk.xq")!!.readBytes().toString(Charsets.UTF_8)

    fun getNasenRachenAbstriche() = javaClass.classLoader.getResourceAsStream("queries/naserachenabstrich.xq")!!.readBytes().toString(Charsets.UTF_8)

    fun getAnzahlEFaecalis() = javaClass.classLoader.getResourceAsStream("queries/anzahlEfaecalis.xq")!!.readBytes().toString(Charsets.UTF_8)

    fun getEfaeciumBK() = javaClass.classLoader.getResourceAsStream("queries/efaecium_bk.xq")!!.readBytes().toString(Charsets.UTF_8)

    fun getVREBK() = javaClass.classLoader.getResourceAsStream("queries/vre_bk.xq")!!.readBytes().toString(Charsets.UTF_8)


}




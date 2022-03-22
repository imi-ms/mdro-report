package de.uni_muenster.imi.oegd.common

object BaseXQueries {
    fun getMRSA() = readFile("mrsa_excelv3.xq")
    fun getMRGN() = readFile("mrgn_excelv3.xq")
    fun getVRE() = readFile("vre_excelv2.xq")
    fun getFalltage() = readFile("Falltage.xq")
    fun getFallzahlen() = readFile("fallzahlen.xq")
    fun getFallzahlen2() = readFile("fallzahlen2.xq")
    fun getMRSABK() = readFile("mrsa_bk.xq")
    fun getMSSABK() = readFile("mssa_bk.xq")
    fun getNasenRachenAbstriche() = readFile("naserachenabstrich.xq")
    fun getNasenRachenAbstriche2() = readFile("nasenrachenabstrich2.xq")
    fun getAnzahlEFaecalis() = readFile("anzahlEfaecalis.xq")
    fun getAnzahlEFaecalis2() = readFile("anzahlEfaecalis2.xq")
    fun getEfaeciumBK() = readFile("efaecium_bk.xq")
    fun getEfaeciumBK2() = readFile("efaecium_bk2.xq")
    fun getVREBK() = readFile("vre_bk.xq")

    private fun readFile(filename: String) =
        javaClass.classLoader.getResourceAsStream("queries/$filename")!!.readBytes().toString(Charsets.UTF_8)
}




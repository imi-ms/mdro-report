package de.uni_muenster.imi.oegd.baseX

object BaseXQueries {

    //language=XPath
    //fun getMRSA() =
    //    "for \$x in /patient/case/labReport/sample/germ/comment[contains(@class,\"MRSA\")]\nwhere \$x/../../../../@type=\"S\"\n\nlet \$ids:=\$x/../../../../@id\ngroup by \$ids\nlet \$station := string-join(\$x/../../../../location[@till > subsequence(\$x/../../../sample/@from,1,1) and @from < subsequence(\$x/../../../sample/@from,1,1)]/@clinic,'; ')\n\nreturn \n(\$x/../../../../@id || \"&#9;||&#9;\" || subsequence(\$x/../../../request/@from,1,1) || \"&#9;||&#9;\"  || subsequence(\$x/../../../sample/@display,1,1) || \"&#9;||&#9;\"  || subsequence(\$x/../../../../hygiene-message/@infection,1,1) || \"&#9;||&#9;\"  || subsequence(\$x/../../../../hygiene-message/@nosocomial,1,1) || \"&#9;||&#9;\" || subsequence(\$x/../../../request/@sender,1,1)|| \"&#9;||&#9;\" || \$station|| \"&#9;||&#9;\" ||   subsequence(\$x/../../germ/pcr-meta[@k=\"Spa\"]/@v,1,1)|| \"&#9;||&#9;\" ||subsequence(\$x/../../germ/pcr-meta[@k=\"ClusterType\"]/@v,1,1) )\n"
    fun getMRSA() = javaClass.classLoader.getResourceAsStream("queries/mrsa_excelv3.xq")!!.readBytes().toString(Charsets.UTF_8)

    fun getMRGN() = javaClass.classLoader.getResourceAsStream("queries/mrgn_excelv3.xq")!!.readBytes().toString(Charsets.UTF_8)

    fun getVRE() = javaClass.classLoader.getResourceAsStream("queries/vre_excelv2.xq")!!.readBytes().toString(Charsets.UTF_8)

}


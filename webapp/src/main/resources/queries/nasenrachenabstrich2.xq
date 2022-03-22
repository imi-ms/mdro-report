(: simpler version of nasenrachenabstraich.xq - seems to return the same results but about 35 % faster :)
count(
    distinct-values(
        /patient/case[
            @type="S" and
            (xs:dateTime(@from) > xs:dateTime("#YEAR_START") and xs:dateTime(@from) < xs:dateTime("#YEAR_END")) and
            labReport/sample[
                @bodySiteDisplay="Nase" or @bodySiteDisplay="Nase und Rachen" or @bodySiteDisplay="Rachen"
            ]
        ]/@id/data()
    )
)


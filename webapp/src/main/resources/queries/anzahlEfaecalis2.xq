(: 17 % faster :)
count(
    distinct-values(
        patient/case[
            @type="S" and
            (xs:dateTime(@from) > xs:dateTime("#YEAR_START") and xs:dateTime(@from) < xs:dateTime("#YEAR_END")) and
            labReport/sample/germ/@display="Enterococcus faecalis"
        ]/@id
    )
)

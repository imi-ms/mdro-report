(: 11 % faster :)
count(
    distinct-values(
        patient/case[
            @type="STATIONAER" and
            (xs:dateTime(@from) > xs:dateTime("#YEAR_START") and xs:dateTime(@from) < xs:dateTime("#YEAR_END")) and
            labReport/sample[
                (@bodySiteDisplay="Blut-peripher entnommen" or @bodySiteDisplay="Blut-zentral entnommen") and
                germ/@display="Enterococcus faecium"
            ]
        ]/@id
    )
)
(: 26% schneller :)
count(
    patient/case[
        @type="STATIONAER" and
        (xs:dateTime(@from) > xs:dateTime("#YEAR_START") and xs:dateTime(@from) < xs:dateTime("#YEAR_END"))
    ]/@id
)
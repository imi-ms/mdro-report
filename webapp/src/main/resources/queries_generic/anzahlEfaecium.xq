count(
  distinct-values(
    patient/case[
      case-matches(., #CASE_TYPE) and
      (xs:dateTime(@from) > xs:dateTime("#YEAR_START") and xs:dateTime(@from) < xs:dateTime("#YEAR_END")) and
      labReport/sample[has-organism(., $E_FAECIUM)]
    ]/@id
  )
)

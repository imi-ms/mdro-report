for $g in /patient/case/labReport/sample/germ[has-mdro-category(.)]
where (xs:dateTime($g/../@from) > xs:dateTime("#YEAR_START") and xs:dateTime($g/../@from) < xs:dateTime("#YEAR_END"))
let $category := $g/resistancePhenotype[@system=$LOINC and @code = ($MRGN3, $MRGN4)]/@display
group by $id := $g/../../../@id, $name := $g/@display, $class := $category
let $requestDate := subsequence($g/../../request/@from,1,1)
let $station := string-join($g/../../../location[@till > $requestDate and @from < $requestDate]/@clinic,'; ')
return <data
  caseID="{$g/../../../@id}"
  caseType="{$g/../../../@encounterClassCode}"
  samplingDate="{$requestDate}"
  sampleType="{subsequence($g/../@specimenDisplay,1,1)} {subsequence($g/../@bodySiteDisplay,1,1)}"
  sender="{subsequence($g/../../request/@sender,1,1)}"
  department="{($station otherwise "prestationary")}"
  pathogen="{subsequence($g/@display,1,1)}"
  class="{$class}"
  piperacillin="{if(subsequence($g/antibiotic[@LOINC="18970-4"]/result/@string,1,1) = "R") then "R" else ""}"
  cefotaxime="{subsequence($g/antibiotic[@LOINC="18886-2"]/result/@string,1,1)}"
  cefTAZidime="{subsequence($g/antibiotic[@LOINC="18893-8"]/result/@string,1,1)}"
  cefepime="{subsequence($g/antibiotic[@LOINC="18879-7"]/result/@string,1,1)}"
  meropenem="{subsequence($g/antibiotic[@LOINC="18943-1"]/result/@string,1,1)}"
  imipenem="{subsequence($g/antibiotic[@LOINC="18932-4"]/result/@string,1,1)}"
  ciprofloxacin="{subsequence($g/antibiotic[@LOINC="18906-8"]/result/@string,1,1)}"
  stType="{subsequence($g/pcr-meta[@k="ST"]/@v,1,1)}"
/>

for $x in /patient/case/labReport/sample/germ/comment[contains(@class,"MRGN")]
(: where $x/../../../../@type=#CASE_TYPE :)
where (xs:dateTime($x/../../../sample/@from) > xs:dateTime("#YEAR_START") and xs:dateTime($x/../../../sample/@from) < xs:dateTime("#YEAR_END"))

group by $id:=$x/../../../../@id, $name:=$x/../@display, $class:=$x/@class
let $station := string-join($x/../../../../location[@till > subsequence($x/../../../request/@from,1,1) and @from < subsequence($x/../../../request/@from,1,1)]/@clinic,'; ')

return (<data
    caseID="{$x/../../../../@id}"
    caseType="{$x/../../../../@type}"
    samplingDate="{subsequence($x/../../../request/@from,1,1)}"
    sampleType="{subsequence($x/../../../sample/@display,1,1)}"
    sender="{subsequence($x/../../../request/@sender,1,1)}"
    department="{($station ?: "Prästationär")}"
    pathogen="{subsequence($x/../@display,1,1)}"
    class="{subsequence($x/@class,1,1)}"
    piperacillin="{if(subsequence($x/../antibiotic[@LOINC="18970-4"]/result/@string,1,1) = "R") then "R" else ""}"
    cefotaxime="{subsequence($x/../antibiotic[@LOINC="18886-2"]/result/@string,1,1)}"
    cefTAZidime="{subsequence($x/../antibiotic[@LOINC="18893-8"]/result/@string,1,1)}"
    cefepime="{subsequence($x/../antibiotic[@LOINC="18879-7"]/result/@string,1,1)}"
    meropenem="{subsequence($x/../antibiotic[@LOINC="18943-1"]/result/@string,1,1)}"
    imipenem="{subsequence($x/../antibiotic[@LOINC="18932-4"]/result/@string,1,1)}"
    ciprofloxacin="{subsequence($x/../antibiotic[@LOINC="18906-8"]/result/@string,1,1)}"
 />)

 (: 18970-4 = Piperacillin+Tazobactam, so if resistant against both, it is resistant against Piperacillin, otherwise, we cannot make any assumption:)
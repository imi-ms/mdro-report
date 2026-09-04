for $g in /patient/case/labReport/sample/germ[contains(comment/@class,"MRGN")]
where (xs:dateTime($g/../../sample/@from) > xs:dateTime("#YEAR_START") and xs:dateTime($g/../../sample/@from) < xs:dateTime("#YEAR_END"))

group by $id:=$g/../../../@id, $name:=$g/@display, $class:=$g/comment/@class
let $sample := $g/..
let $labReport := $sample/..
let $case := $labReport/..
let $samplingDate := subsequence($labReport/request/@from, 1, 1)
let $station := string-join($case/location[@till > $samplingDate and @from < $samplingDate]/@clinic,'; ')

return (<data
    caseID="{$case/@id}"
    caseType="{$case/@type}"
    samplingDate="{$samplingDate}"
    sampleType="{subsequence($sample/@display,1,1)}"
    sender="{subsequence($labReport/request/@sender,1,1)}"
    department="{($station otherwise "Prästationär")}"
    pathogen="{subsequence($g/@display,1,1)}"
    class="{subsequence($g/comment/@class,1,1)}"
    piperacillin="{if(subsequence($g/antibiotic[@LOINC="18970-4"]/result/@string,1,1) = "R") then "R" else ""}"
    cefotaxime="{subsequence($g/antibiotic[@LOINC="18886-2"]/result/@string,1,1)}"
    cefTAZidime="{subsequence($g/antibiotic[@LOINC="18893-8"]/result/@string,1,1)}"
    cefepime="{subsequence($g/antibiotic[@LOINC="18879-7"]/result/@string,1,1)}"
    meropenem="{subsequence($g/antibiotic[@LOINC="18943-1"]/result/@string,1,1)}"
    imipenem="{subsequence($g/antibiotic[@LOINC="18932-4"]/result/@string,1,1)}"
    ciprofloxacin="{subsequence($g/antibiotic[@LOINC="18906-8"]/result/@string,1,1)}"
    stType="{subsequence($g/pcr-meta[@k="ST"]/@v,1,1)}"
 />)

 (: 18970-4 = Piperacillin+Tazobactam, so if resistant against both, it is resistant against Piperacillin, otherwise, we cannot make any assumption:)
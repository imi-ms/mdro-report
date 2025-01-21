declare function local:quartery($value as xs:string?, $trueValue as xs:string?, $falseValue as xs:string?, $nullValue as xs:string?)
as xs:string? {
	if($value = "true") then ($trueValue) else (if($value = "false") then $falseValue else $nullValue)
};

declare function local:index-of-first($seq as item()*, $criterion as function(item()) as xs:boolean) as xs:integer? {
  head(for $i in 1 to count($seq) return if ($criterion($seq[$i])) then $i)
};

for $x in /patient/case/labReport/sample/germ/comment[contains(@class,"VRE")]
(: where $x/../../../../@type=#CASE_TYPE :)
where (xs:dateTime($x/../../../sample/@from) > xs:dateTime("#YEAR_START") and xs:dateTime($x/../../../sample/@from) < xs:dateTime("#YEAR_END"))

let $ids:=$x/../../../../@id
group by $ids

let $probenarten := $x/../../../sample/@display
let $idx := local:index-of-first($probenarten, function($it) { contains($it, "Blut") }) otherwise 1

let $station := string-join($x/../../../../location[@till > subsequence($x/../../../sample/@from,$idx,1) and @from < subsequence($x/../../../sample/@from,$idx,1)]/@clinic,'; ')


let $probenart :=  subsequence($probenarten, $idx, 1)

return <data
    caseID="{$x/../../../../@id}"
    caseType="{$x/../../../../@type}"
    samplingDate="{subsequence($x/../../../request/@from,$idx,1)}"
    sampleType="{$probenart}"
    sender="{subsequence($x/../../../request/@sender,$idx,1)}"
    department="{($station otherwise "Prästationär")}"
    pathogen="{subsequence($x/../@display,$idx,1)}"
    linezolid="{subsequence($x/../antibiotic[@LOINC="29258-1"]/result/@string,$idx,1)}"
    tigecylin="{subsequence($x/../antibiotic[@LOINC="42357-4"]/result/@string,$idx,1)}"
    vancomycin="{subsequence($x/../antibiotic[@LOINC="19000-9"]/result/@string,$idx,1)}"
    teicoplanin="{subsequence($x/../antibiotic[@LOINC="18989-4"]/result/@string,$idx,1) }"
    quinupristinAndDalfopristin="{subsequence($x/../antibiotic[@LOINC="23640-6"]/result/@string,$idx,1)}"
/>



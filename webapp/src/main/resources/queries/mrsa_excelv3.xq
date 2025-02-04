declare function local:quartery($value as xs:string?, $trueValue as xs:string?, $falseValue as xs:string?, $nullValue as xs:string?)
as xs:string? {
	if($value = "true") then ($trueValue) else (if($value = "false") then $falseValue else $nullValue)
};

declare function local:otherwise($value as xs:string?, $value2 as xs:string?) as xs:string? {
	if($value != "") then ($value) else ($value2)
};

declare function local:index-of-first($seq as item()*, $criterion as function(item()) as xs:boolean) as xs:integer? {
  head(for $i in 1 to count($seq) return if ($criterion($seq[$i])) then $i)
};

for $x in /patient/case/labReport/sample/germ/comment[contains(@class,"MRSA")]
(: where $x/../../../../@type=#CASE_TYPE :)
where (xs:dateTime($x/../../../sample/@from) > xs:dateTime("#YEAR_START") and xs:dateTime($x/../../../sample/@from) < xs:dateTime("#YEAR_END"))

let $ids:=$x/../../../../@id
group by $ids
let $probenarten := $x/../../../sample/@display
let $idx := local:index-of-first($probenarten, function($it) { contains($it, "Blut") }) otherwise 1

let $station := string-join($x/../../../../..//location[@till > subsequence($x/../../../sample/@from,$idx,1) and @from < subsequence($x/../../../sample/@from,$idx,1)]/@clinic,'; ')

let $infection := local:quartery(subsequence($x/../../../../hygiene-message/@infection,1,1), "Infektion", "Besiedlung", "unbekannt")
let $nosocomial := local:quartery(subsequence($x/../../../../hygiene-message/@nosocomial,1,1), "importiert", "nosokomial", "importiert")
let $spa :=  local:otherwise(subsequence($x/../../germ/pcr-meta[@k="SpaType"]/@v,$idx,1), subsequence($x/../../germ/pcr-meta[@k="Spa"]/@v,$idx,1))
let $cluster := subsequence($x/../../germ/pcr-meta[@k="ClusterType"]/@v,$idx,1)
let $stType := subsequence($x/../../germ/pcr-meta[@k="ST"]/@v,$idx,1)

return <data
 caseID="{$x/../../../../@id}"
 caseType="{$x/../../../../@type}"
 samplingDate="{subsequence($x/../../../request/@from,$idx,1)}"
 sampleType="{subsequence($x/../../../sample/@display,$idx,1)}"
 infection="{$infection}"
 nosocomial="{$nosocomial}"
 sender="{subsequence($x/../../../request/@sender,$idx,1)}"
 department="{$station otherwise "Prästationär"}"
 spa="{$spa}"
 clustertype="{$cluster}"
 stType="{$stType}"
 />


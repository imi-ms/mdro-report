declare function local:quartery($value as xs:string?, $trueValue as xs:string?, $falseValue as xs:string?, $nullValue as xs:string?)
as xs:string? {
	if($value = "true") then ($trueValue) else (if($value = "false") then $falseValue else $nullValue)
};

declare function local:otherwise($value as xs:string?, $value2 as xs:string?) as xs:string? {
	if($value != "") then ($value) else ($value2)
};

for $x in /patient/case/labReport/sample/germ/comment[contains(@class,"MRSA")]
where $x/../../../../@type=#CASE_TYPE
where (xs:dateTime($x/../../../../@from) > xs:dateTime("#YEAR_START") and xs:dateTime($x/../../../../@from) < xs:dateTime("#YEAR_END"))

let $ids:=$x/../../../../@id
group by $ids
let $station := string-join($x/../../../../..//location[@till > subsequence($x/../../../sample/@from,1,1) and @from < subsequence($x/../../../sample/@from,1,1)]/@clinic,'; ')

let $pid := $x/../../../../@id
let $from := subsequence($x/../../../request/@from,1,1)
let $material := subsequence($x/../../../sample/@display,1,1)
let $infection := local:quartery(subsequence($x/../../../../hygiene-message/@infection,1,1), "Infektion", "Besiedlung", "unbekannt")
let $nosocomial := local:quartery(subsequence($x/../../../../hygiene-message/@nosocomial,1,1), "importiert", "nosokomial", "importiert")
let $sender := subsequence($x/../../../request/@sender,1,1)
let $spa :=  local:otherwise(subsequence($x/../../germ/pcr-meta[@k="SpaType"]/@v,1,1), subsequence($x/../../germ/pcr-meta[@k="Spa"]/@v,1,1))
let $cluster := subsequence($x/../../germ/pcr-meta[@k="ClusterType"]/@v,1,1)
return 
($pid || "&#9;||&#9;" || $from  || "&#9;||&#9;"  || $material || "&#9;||&#9;"  || $infection || "&#9;||&#9;"  || $nosocomial || "&#9;||&#9;" || $sender || "&#9;||&#9;" || ($station ?: "Prästationär") || "&#9;||&#9;" ||  $spa || "&#9;||&#9;" || $cluster)

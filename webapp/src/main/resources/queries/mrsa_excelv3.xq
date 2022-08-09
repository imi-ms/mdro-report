declare function local:quartery($value as xs:string?, $trueValue as xs:string?, $falseValue as xs:string?, $nullValue as xs:string?)
as xs:string? {
	if($value = "true") then ($trueValue) else (if($value = "false") then $falseValue else $nullValue)
};

for $x in /patient/case/labReport/sample/germ/comment[contains(@class,"MRSA")]
where $x/../../../../@type="STATIONAER"
where (xs:dateTime($x/../../../../@from) > xs:dateTime("#YEAR_START") and xs:dateTime($x/../../../../@from) < xs:dateTime("#YEAR_END"))

let $ids:=$x/../../../../@id
group by $ids
let $station := string-join($x/../../../../..//location[@till > subsequence($x/../../../sample/@from,1,1) and @from < subsequence($x/../../../sample/@from,1,1)]/@clinic,'; ')

return 
($x/../../../../@id || "&#9;||&#9;" || subsequence($x/../../../request/@from,1,1)  || "&#9;||&#9;"  || subsequence($x/../../../sample/@display,1,1) || "&#9;||&#9;"  || local:quartery(subsequence($x/../../../../hygiene-message/@infection,1,1), "Infektion", "Besiedlung", "unbekannt") || "&#9;||&#9;"  || local:quartery(subsequence($x/../../../../hygiene-message/@nosocomial,1,1), "importiert", "nosokomial", "importiert") || "&#9;||&#9;" || subsequence($x/../../../request/@sender,1,1)|| "&#9;||&#9;" || ($station ?: "Prästationär") || "&#9;||&#9;" ||   subsequence($x/../../germ/pcr-meta[@k="Spa"]/@v,1,1)|| "&#9;||&#9;" ||subsequence($x/../../germ/pcr-meta[@k="ClusterType"]/@v,1,1) )

declare function local:quartery($value as xs:string?, $trueValue as xs:string?, $falseValue as xs:string?, $nullValue as xs:string?)
as xs:string? {
	if($value = "true") then ($trueValue) else (if($value = "false") then $falseValue else $nullValue)
};

for $x in /patient/case/labReport/sample/germ/comment[contains(@class,"VRE")]
where $x/../../../../@type="S"
where (xs:dateTime($x/../../../../@from) > xs:dateTime("#YEAR_START") and xs:dateTime($x/../../../../@from) < xs:dateTime("#YEAR_END"))

let $ids:=$x/../../../../@id
group by $ids
let $station := string-join($x/../../../../location[@till > subsequence($x/../../../sample/@from,1,1) and @from < subsequence($x/../../../sample/@from,1,1)]/@clinic,'; ')

return 
$x/../../../../@id || "&#9;||&#9;" || subsequence($x/../../../request/@from,1,1) || "&#9;||&#9;"  || subsequence($x/../../../sample/@display,1,1) || "&#9;||&#9;" || subsequence($x/../../../request/@sender,1,1)|| "&#9;||&#9;" || ($station ?: "Prästationär") || "&#9;||&#9;" || subsequence($x/../@display,1,1) ||"&#9;||&#9;" ||   subsequence($x/../antibiotic[@LOINC="29258-1"]/result/@string,1,1) ||"&#9;||&#9;" || subsequence($x/../antibiotic[@LOINC="42357-4"]/result/@string,1,1)||"&#9;||&#9;" || subsequence($x/../antibiotic[@LOINC="19000-9"]/result/@string,1,1)||"&#9;||&#9;" || subsequence($x/../antibiotic[@LOINC="18989-4"]/result/@string,1,1) ||"&#9;||&#9;" || subsequence($x/../antibiotic[@LOINC="23640-6"]/result/@string,1,1)
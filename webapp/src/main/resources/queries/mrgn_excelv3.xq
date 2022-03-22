for $x in /patient/case/labReport/sample/germ/comment[contains(@class,"MRGN")]
where $x/../../../../@type="S"
where (xs:dateTime($x/../../../../@from) > xs:dateTime("#YEAR_START") and xs:dateTime($x/../../../../@from) < xs:dateTime("#YEAR_END"))

group by $id:=$x/../../../../@id, $name:=$x/../@display, $class:=$x/@class
let $station := string-join($x/../../../../location[@till > subsequence($x/../../../request/@from,1,1) and @from < subsequence($x/../../../request/@from,1,1)]/@clinic,'; ')

return 
$x/../../../../@id || "&#9;||&#9;" || subsequence($x/../../../request/@from,1,1) || "&#9;||&#9;"   || subsequence($x/../../../sample/@display,1,1) || "&#9;||&#9;" ||  subsequence($x/../../../request/@sender,1,1)||"&#9;||&#9;"  ||  $station ||"&#9;||&#9;"  ||subsequence($x/../@display,1,1) ||"&#9;||&#9;" ||subsequence($x/@class,1,1)||"&#9;||&#9;" || subsequence($x/../antibiotic[@LOINC="18970-4"]/result/@string,1,1)||"&#9;||&#9;" || subsequence($x/../antibiotic[@LOINC="18886-2"]/result/@string,1,1) ||"&#9;||&#9;" || subsequence($x/../antibiotic[@LOINC="18893-8"]/result/@string,1,1)||"&#9;||&#9;" || subsequence($x/../antibiotic[@LOINC="18879-7"]/result/@string,1,1)||"&#9;||&#9;" || subsequence($x/../antibiotic[@LOINC="18943-1"]/result/@string,1,1)||"&#9;||&#9;" || subsequence($x/../antibiotic[@LOINC="18932-4"]/result/@string,1,1)||"&#9;||&#9;" || subsequence($x/../antibiotic[@LOINC="18906-8"]/result/@string,1,1)
let $input := 
<patientenzahlen>
{
for $x in /patient/case
where (xs:dateTime($x/@from) > xs:dateTime("#YEAR_START") and xs:dateTime($x/@from) < xs:dateTime("#YEAR_END"))
where $x/@type="S"
return 
<caseID>
$x/@id
</caseID>
}
</patientenzahlen>
return count($input/caseID)

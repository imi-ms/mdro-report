let $input := 
<patientenzahlen>
{
for $x in /patient/case
where substring($x/@from,1,4)="2021"
where $x/@type="S"
return 
<caseID>
$x/@id
</caseID>
}
</patientenzahlen>
return count($input/caseID)

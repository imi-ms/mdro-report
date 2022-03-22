let $input :=
<mssabk>
{
for $x in /patient/case/labReport/sample
where $x/../../@type="S"
where (xs:dateTime($x/../../@from) > xs:dateTime("#YEAR_START") and xs:dateTime($x/../../@from) < xs:dateTime("#YEAR_END"))
where $x/@bodySiteDisplay="Blut-peripher entnommen" or $x/@bodySiteDisplay="Blut-zentral entnommen"
where $x/germ/@display="Enterococcus faecium" 
let $ids:=$x/../../@id
group by $ids
return
<patientID>{$x/../../@id}</patientID>
}
</mssabk>

return count($input/patientID)
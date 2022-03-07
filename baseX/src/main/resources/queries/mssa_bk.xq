let $input :=
<mssabk>
{
for $x in /patient/case/labReport/sample
where $x/../../@type="S"
where $x/@bodySiteDisplay="Blut-peripher entnommen" or $x/@bodySiteDisplay="Blut-zentral entnommen"
where $x/germ/@display="Staphylococcus aureus"
let $ids:=$x/../../@id
group by $ids
return
<patientID>{$x/../../@id}</patientID>
}
</mssabk>

return count($input/patientID)
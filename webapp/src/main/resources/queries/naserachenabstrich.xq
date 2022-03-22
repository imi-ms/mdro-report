let $input :=
<naserachen>
{
for $x in /patient/case/labReport/sample
where $x/../../@type="S"
where $x/@bodySiteDisplay="Nase" or $x/@bodySiteDisplay="Nase und Rachen" or $x/@bodySiteDisplay="Rachen" 
let $ids:=$x/../../@id
group by $ids
return
<patientID>{data($x/../../@id)}</patientID>
}
</naserachen>

return count($input/patientID)
 
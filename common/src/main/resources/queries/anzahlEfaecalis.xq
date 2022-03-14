let $input :=
<mssabk>
{
for $x in /patient/case/labReport/sample
where $x/../../@type="S"
where $x/germ/@display="Enterococcus faecalis"
let $ids:=$x/../../@id
group by $ids
return
<patientID>{$x/../../@id}</patientID>
}
</mssabk>

return count($input/patientID)
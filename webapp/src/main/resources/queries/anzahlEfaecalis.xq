let $input :=
<mssabk>
{
for $x in /patient/case/labReport/sample
where (xs:dateTime($x/../../@from) > xs:dateTime("#YEAR_START") and xs:dateTime($x/../../@from) < xs:dateTime("#YEAR_END"))
where $x/../../@type=#CASE_TYPE
where $x/germ/@display="Enterococcus faecalis"
let $ids:=$x/../../@id
group by $ids
return
<patientID>{$x/../../@id}</patientID>
}
</mssabk>

return count($input/patientID)
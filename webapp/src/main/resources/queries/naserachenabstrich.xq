let $input :=
<naserachen>
{
for $x in /patient/case/labReport/sample
where $x/../../@type="STATIONAER"
where (xs:dateTime($x/../../@from) > xs:dateTime("#YEAR_START") and xs:dateTime($x/../../@from) < xs:dateTime("#YEAR_END"))
where $x/@bodySiteDisplay="Nase" or $x/@bodySiteDisplay="Nase und Rachen" or $x/@bodySiteDisplay="Rachen"
let $ids:=$x/../../@id
group by $ids
return
<patientID>{data($x/../../@id)}</patientID>
}
</naserachen>

return count($input/patientID)
 
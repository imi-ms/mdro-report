
let $input :=
<mssabk>
{
for $x in /patient/case/labReport/sample
where (xs:dateTime($x/../../@from) > xs:dateTime("#YEAR_START") and xs:dateTime($x/../../@from) < xs:dateTime("#YEAR_END"))
where case-matches($x/../.., #CASE_TYPE)
where has-organism($x, $E_FAECALIS)
let $ids := $x/../../@id
group by $ids
return <patientID>{$x/../../@id}</patientID>
}
</mssabk>
return count($input/patientID)

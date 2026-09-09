let $input :=
<naserachen>
{
for $x in /patient/case/labReport/sample
where case-matches($x/../.., #CASE_TYPE)
where (xs:dateTime($x/../../@from) > xs:dateTime("#YEAR_START") and xs:dateTime($x/../../@from) < xs:dateTime("#YEAR_END"))
where is-upper-respiratory-swab($x)
let $ids := $x/../../@id
group by $ids
return <patientID>{data($x/../../@id)}</patientID>
}
</naserachen>
return count($input/patientID)

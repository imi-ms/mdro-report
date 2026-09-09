
(:
 : This intentionally preserves the current query semantics: it counts blood
 : culture cases with S. aureus, including MRSA. See REVIEW_NOTES.md before
 : interpreting it as a true MSSA-only metric.
 :)
let $input :=
<mssabk>
{
for $x in /patient/case/labReport/sample
where case-matches($x/../.., #CASE_TYPE)
where (xs:dateTime($x/../../@from) > xs:dateTime("#YEAR_START") and xs:dateTime($x/../../@from) < xs:dateTime("#YEAR_END"))
where is-blood-sample($x)
where has-organism($x, $STAPH_AUREUS)
let $ids := $x/../../@id
group by $ids
return <patientID>{$x/../../@id}</patientID>
}
</mssabk>
return count($input/patientID)

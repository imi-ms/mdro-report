let $input :=
<Aufnahme20Entlassung21>
{
for $x in db:open("2021")/patient/case
where substring($x/@from,1,4)="2020"
where substring($x/@till,1,4)="2021"
where $x/@type="S"
where $x/labReport/sample/germ/comment[contains(@class,"MRSA")]
let $date1:=xs:dateTime("2021-01-01T00:00:01")
let $date2:=xs:dateTime($x/@till)
let $datediff:=ceiling(($date2 - $date1)div xs:dayTimeDuration("P1D"))
return
<dates>
{$datediff}
</dates>
}
</Aufnahme20Entlassung21>

let $input2 :=
<Aufnahme21Entlassung21>
{
for $x in db:open("2021")/patient/case
where substring($x/@from,1,4)="2021"
where substring($x/@till,1,4)="2021"
where $x/@type="S"
where $x/labReport/sample/germ/comment[contains(@class,"MRSA")]
let $date1:=xs:dateTime($x/@from)
let $date2:=xs:dateTime($x/@till)
let $datediff:=ceiling(($date2 - $date1)div xs:dayTimeDuration("P1D"))
return
<dates>
{$datediff}
</dates>
}
</Aufnahme21Entlassung21>

let $input3 :=
<Aufnahme21Entlassung22>
{
for $x in db:open("2021")/patient/case
where substring($x/@from,1,4)="2021"
where substring($x/@till,1,4)="2022"
where $x/@type="S"
where $x/labReport/sample/germ/comment[contains(@class,"MRSA")]
let $date1:=xs:dateTime($x/@from)
let $date2:=xs:dateTime("2021-12-31T23:59:00")
let $datediff:=ceiling(($date2 - $date1)div xs:dayTimeDuration("P1D"))
return
<dates>
{$datediff}
</dates>
}
</Aufnahme21Entlassung22>

let $input4 :=
<Aufnahme21NichtEntlassen>
{
for $x in db:open("2021")/patient/case
where substring($x/@from,1,4)="2021"
where not($x/@till)
where $x/@type="S"
where $x/labReport/sample/germ/comment[contains(@class,"MRSA")]
let $date1:=xs:dateTime($x/@from)
let $date2:=xs:dateTime("2021-12-31T23:59:00")
let $datediff:=ceiling(($date2 - $date1)div xs:dayTimeDuration("P1D"))
return
<dates>
{$datediff}
</dates>
}
</Aufnahme21NichtEntlassen>



return ( sum($input/dates)+sum($input2/dates)+sum($input3/dates)+ sum($input4/dates) )
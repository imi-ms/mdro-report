let $input := 
<Aufnahme20Entlassung21>
{
for $x in /patient/case
where xs:dateTime($x/@from) < xs:dateTime("#YEAR_START")
where xs:dateTime($x/@till) > xs:dateTime("#YEAR_START")
where $x/@type="STATIONAER"
let $date1:=xs:dateTime("#YEAR_START")
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
for $x in /patient/case
where (xs:dateTime($x/@from) >= xs:dateTime("#YEAR_START") and xs:dateTime($x/@from) <= xs:dateTime("#YEAR_END"))
where (xs:dateTime($x/@till) >= xs:dateTime("#YEAR_START") and xs:dateTime($x/@till) <= xs:dateTime("#YEAR_END"))
where $x/@type="STATIONAER"
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
for $x in /patient/case
where (xs:dateTime($x/@from) >= xs:dateTime("#YEAR_START") and xs:dateTime($x/@from) <= xs:dateTime("#YEAR_END"))
where xs:dateTime($x/@till) > xs:dateTime("#YEAR_END")
where $x/@type="STATIONAER"
let $date1:=xs:dateTime($x/@from)
let $date2:=xs:dateTime("#YEAR_END")
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
for $x in /patient/case
where (xs:dateTime($x/@from) >= xs:dateTime("#YEAR_START") and xs:dateTime($x/@from) <= xs:dateTime("#YEAR_END"))
where not($x/@till)
where $x/@type="STATIONAER"
let $date1:=xs:dateTime($x/@from)
let $date2:=xs:dateTime("#YEAR_END")
let $datediff:=ceiling(($date2 - $date1)div xs:dayTimeDuration("P1D"))
return 
<dates>
{$datediff}
</dates>
}
</Aufnahme21NichtEntlassen>



return ( sum($input/dates)+sum($input2/dates)+sum($input3/dates)+ sum($input4/dates) )
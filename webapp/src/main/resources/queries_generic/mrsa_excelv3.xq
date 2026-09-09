for $g in /patient/case/labReport/sample/germ[is-mrsa-germ(.)]
let $samples := $g/..
where (xs:dateTime($g/../@from) > xs:dateTime("#YEAR_START") and xs:dateTime($g/../@from) < xs:dateTime("#YEAR_END"))
let $ids := $g/../../../@id
group by $ids

let $bloodIndex := index-of-first($samples, function($it) { is-blood-sample($it) })
let $idx := if (exists($bloodIndex)) then $bloodIndex else 1
let $sample := subsequence($samples,$idx,1)
let $case := $sample/../..
let $selectedGerm := $sample/germ (: [is-mrsa-germ(.)][1] :)
(: Patient-level location lookup retained to preserve the original query behavior.:)
let $station := string-join($case/..//location[@till > $sample/@from and @from < $sample/@from]/@clinic,'; ')
let $infection := quartery(subsequence($case/hygiene-message/@infection,1,1), "Infektion", "Besiedlung", "unbekannt")
(: The original true/false mapping is retained for result equivalence; see REVIEW_NOTES.md.:)
let $nosocomial := quartery(subsequence($case/hygiene-message/@nosocomial,1,1), "importiert", "nosokomial", "importiert")
let $spa := otherwise(subsequence($selectedGerm/pcr-meta[@k="SpaType"]/@v,1,1), subsequence($selectedGerm/pcr-meta[@k="Spa"]/@v,1,1))
let $cluster := subsequence($selectedGerm/pcr-meta[@k="ClusterType"]/@v,1,1)
let $stType := subsequence($selectedGerm/pcr-meta[@k="ST"]/@v,1,1)
return <data
  caseID="{$case/@id}"
  caseType="{$case/@encounterClassCode}"
  samplingDate="{$sample/../request/@from}"
  sampleType="{$sample/@specimenDisplay} {$sample/@bodySiteDisplay}"
  infection="{$infection}"
  nosocomial="{$nosocomial}"
  sender="{$sample/../request/@sender}"
  department="{$station otherwise "prestationary"}"
  spa="{$spa}"
  clustertype="{$cluster}"
  stType="{$stType}"
/>

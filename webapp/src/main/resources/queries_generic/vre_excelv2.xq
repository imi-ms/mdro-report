for $g in /patient/case/labReport/sample/germ[is-vre-germ(.)]
where (xs:dateTime($g/../@from) > xs:dateTime("#YEAR_START") and xs:dateTime($g/../@from) < xs:dateTime("#YEAR_END"))
let $ids := $g/../../../@id
group by $ids
let $samples := $g/..
let $bloodIndex := index-of-first($samples, function($it) { is-blood-sample($it) })
let $idx := if (exists($bloodIndex)) then $bloodIndex else 1
let $sample := subsequence($samples,$idx,1)
let $case := $sample/../..
let $selectedGerm := $sample/germ[is-vre-germ(.)][1]
let $station := string-join($case/location[@till > $sample/@from and @from < $sample/@from]/@clinic,'; ')
return <data
  caseID="{$case/@id}"
  caseType="{$case/@encounterClassCode}"
  samplingDate="{$sample/../request/@from}"
  sampleType="{$sample/@specimenDisplay} {$sample/@bodySiteDisplay}"
  sender="{$sample/../request/@sender}"
  department="{($station otherwise "prestationary")}"
  pathogen="{$selectedGerm/@display}"
  linezolid="{$selectedGerm/antibiotic[@LOINC="29258-1"]/result/@string}"
  tigecylin="{$selectedGerm/antibiotic[@LOINC="42357-4"]/result/@string}"
  vancomycin="{$selectedGerm/antibiotic[@LOINC="19000-9"]/result/@string}"
  teicoplanin="{$selectedGerm/antibiotic[@LOINC="18989-4"]/result/@string}"
  quinupristinAndDalfopristin="{$selectedGerm/antibiotic[@LOINC="23640-6"]/result/@string}"
  stType="{$selectedGerm/pcr-meta[@k="ST"]/@v}"
  vanA="{$selectedGerm/pcr-meta[@k="vanA"]/@v}"
  vanB="{$selectedGerm/pcr-meta[@k="vanB"]/@v}"
/>

xquery version "3.1";
module namespace mdro = "urn:mdro-report:common";

(: Canonical terminology systems :)
declare variable $mdro:SNOMED := "http://snomed.info/sct";
declare variable $mdro:HL7_ACTCODE := "http://terminology.hl7.org/CodeSystem/v3-ActCode";
declare variable $mdro:MDRO_CATEGORY_SYSTEM := "urn:mdro-report:mdro-category";

(: Organisms, SNOMED CT :)
declare variable $mdro:STAPH_AUREUS := "3092008";
declare variable $mdro:E_FAECIUM := "90272000";
declare variable $mdro:E_FAECALIS := "78065002";

(: Resistant organisms/phenotypes, SNOMED CT :)
declare variable $mdro:MRSA := "115329001";
declare variable $mdro:VRE := "113727004";

(: Specimens, SNOMED CT :)
declare variable $mdro:BLOOD_SPECIMEN := "119297000";
declare variable $mdro:UPPER_RESPIRATORY_SWABS := (
  "445297001", (: Swab of internal nose :)
  "258529004", (: Throat swab :)
  "309164002", (: Upper respiratory swab sample :)
  "258500001"  (: Nasopharyngeal swab :)
);

(:
 : Migration-compatible encounter matching.
 : New installations should preferably pass standardized encounter values
 : (IMP, AMB) or the normalized type values below. The German values are kept
 : here only so the current report parameter substitution can migrate without
 : changing all callers at once.
 :)
declare function mdro:case-matches($case as element(case), $requested as xs:string?) as xs:boolean {
  let $value := upper-case(normalize-space(string($requested)))
  return
    if ($value = ("STATIONAER", "INPATIENT")) then
      $case/@encounterTypeCode = "INPATIENT"
    else if ($value = ("AMBULANT", "OUTPATIENT")) then
      $case/@encounterTypeCode = "OUTPATIENT"
    else if ($value = ("TEILSTATIONAER", "DAY_CASE")) then
      $case/@encounterTypeCode = "DAY_CASE"
    else if ($value = ("IMP", "AMB", "OBSENC", "EMER", "VR", "HH")) then
      $case/@encounterClassSystem = $mdro:HL7_ACTCODE and
      $case/@encounterClassCode = $value
    else false()
};

declare function mdro:is-blood-sample($sample as element(sample)?) as xs:boolean {
  exists($sample[
    @specimenSystem = $mdro:SNOMED and
    @specimenCode = $mdro:BLOOD_SPECIMEN
  ])
};

declare function mdro:is-upper-respiratory-swab($sample as element(sample)?) as xs:boolean {
  exists($sample[
    @specimenSystem = $mdro:SNOMED and
    @specimenCode = $mdro:UPPER_RESPIRATORY_SWABS
  ])
};

declare function mdro:has-organism($sample as element(sample)?, $snomed as xs:string) as xs:boolean {
  exists($sample/germ[
    @organismSystem = $mdro:SNOMED and
    @organismCode = $snomed
  ])
};

declare function mdro:is-mrsa-germ($germ as element(germ)?) as xs:boolean {
  exists($germ/resistancePhenotype[
    @system = $mdro:SNOMED and
    @code = $mdro:MRSA
  ])
};

declare function mdro:is-vre-germ($germ as element(germ)?) as xs:boolean {
  exists($germ/resistancePhenotype[
    @system = $mdro:SNOMED and
    @code = $mdro:VRE
  ])
};

declare function mdro:has-mdro-category($germ as element(germ)?, $prefix as xs:string) as xs:boolean {
  exists($germ/mdroCategory[
    @system = $mdro:MDRO_CATEGORY_SYSTEM and starts-with(@code, $prefix)
  ])
};

declare function mdro:index-of-first($seq as item()*, $criterion as function(item()) as xs:boolean) as xs:integer? {
  head(for $i in 1 to count($seq) return if ($criterion($seq[$i])) then $i else ())
};

declare function mdro:quartery($value as xs:string?, $trueValue as xs:string?, $falseValue as xs:string?, $nullValue as xs:string?) as xs:string? {
  if ($value = "true") then $trueValue
  else if ($value = "false") then $falseValue
  else $nullValue
};

declare function mdro:otherwise($value as xs:string?, $value2 as xs:string?) as xs:string? {
  if ($value != "") then $value else $value2
};

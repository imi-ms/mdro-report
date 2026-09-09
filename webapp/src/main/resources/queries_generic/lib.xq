xquery version "3.1";

(: Canonical terminology systems :)
declare variable $SNOMED := "http://snomed.info/sct";
declare variable $LOINC := "http://loinc.org";
declare variable $HL7_ACTCODE := "http://terminology.hl7.org/CodeSystem/v3-ActCode";

(: Organisms, SNOMED CT :)
declare variable $STAPH_AUREUS := "3092008";
declare variable $E_FAECIUM := "90272000";
declare variable $E_FAECALIS := "78065002";

(: Resistant organisms/phenotypes, SNOMED CT or LOINC :)
declare variable $MRSA := "115329001";
declare variable $VRE := "113727004";

declare variable $MRGN3 := "LA33215-7";
declare variable $MRGN4 := "LA33216-5";

(: Specimens, SNOMED CT :)
declare variable $BLOOD_SPECIMEN := "119297000";
declare variable $BLOOD_PERIPHER_SPECIMEN := "122551003";
declare variable $UPPER_RESPIRATORY_SWABS := (
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
declare function case-matches($case as element(case), $requested as xs:string?) as xs:boolean {
  let $value := upper-case(normalize-space(string($requested)))
  return $case/@encounterClassSystem = $HL7_ACTCODE and $case/@encounterClassCode = $value
};

declare function is-blood-sample($sample as element(sample)?) as xs:boolean {
  exists($sample[
    @specimenSystem = $SNOMED and @specimenCode = ($BLOOD_SPECIMEN, $BLOOD_PERIPHER_SPECIMEN)
  ])
};

declare function is-upper-respiratory-swab($sample as element(sample)?) as xs:boolean {
  exists($sample[
    @specimenSystem = $SNOMED and @specimenCode = $UPPER_RESPIRATORY_SWABS
  ])
};

declare function has-organism($sample as element(sample)?, $snomed as xs:string) as xs:boolean {
  exists($sample/germ[ @SNOMED = $snomed ])
};

declare function is-mrsa-germ($germ as element(germ)?) as xs:boolean {
  exists($germ/resistancePhenotype[ @system = $SNOMED and @code = $MRSA ])
};

declare function is-vre-germ($germ as element(germ)?) as xs:boolean {
  exists($germ/resistancePhenotype[ @system = $SNOMED and @code = $VRE ])
};

declare function has-mdro-category($germ as element(germ)?) as xs:boolean {
  exists($germ/resistancePhenotype[ @system = $LOINC and @code = ($MRGN3, $MRGN4) ])
};

declare function index-of-first($seq as item()*, $criterion as function(item()) as xs:boolean) as xs:integer? {
  head(for $i in 1 to count($seq) return if ($criterion($seq[$i])) then $i else ())
};

declare function quartery($value as xs:string?, $trueValue as xs:string?, $falseValue as xs:string?, $nullValue as xs:string?) as xs:string? {
  if ($value = "true") then $trueValue
  else if ($value = "false") then $falseValue
  else $nullValue
};

declare function otherwise($value as xs:string?, $value2 as xs:string?) as xs:string? {
  if ($value != "") then $value else $value2
};

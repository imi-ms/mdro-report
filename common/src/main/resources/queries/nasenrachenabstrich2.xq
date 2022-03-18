(: simpler version of nasenrachenabstraich.xq - seems to return the same results but about 35 % faster :)
count(distinct-values(/patient/case[@type="S" and labReport/sample[@bodySiteDisplay="Nase" or @bodySiteDisplay="Nase und Rachen" or @bodySiteDisplay="Rachen"]]/@id/data()))



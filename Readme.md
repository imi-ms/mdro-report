# MD-Report

This tool is used to create the surveillance report according
to [§ 23 IfSG](https://www.gesetze-im-internet.de/ifsg/__23.html).

# TODO Include fancy screenshot and concept art

# TODO Licence

## Download

You can download the pre-build binaries
from [our institute's GitLab Release page](https://imigitlab.uni-muenster.de/MeDIC/etl/oegd-report/-/releases). You can
decide between a

* Pre-build executable .jar file (*MDReport-Full.jar*, requires an installation of JRE)
* an installer, which install MDReport alongside its own JRE
* A .jar file, that does not include BaseX or JavaFX (*MDReport-Light.jar*, requires separately installed JRE and BaseX,
  run with command line)

## Building

Run

* `./gradlew shadowJar` to create an executable .jar file.
* `./gradlew CreateEXE` to create a Windows installer, that will also install JRE. Note you have to
  install [WiX](https://github.com/wixtoolset/wix3) first.

If you only want to use the web interface and connect to a separate running BaseX-instance, run:

* `./gradlew :webapp:war` to create a .war file.
* `./gradlew :webapp:shadowJar` to create an executable .jar with built-in Netty server.

### Server Deployment

1. You need to have installed a BaseX instance, which is constantly fed the data from the ETL process.
2. Setup Tomcat server, I strongly recommend also installing a reverse proxy with some sort of password protection.
3. **Either**: Checkout the source code, edit `webapp/src/main/resources/application.conf`,
   execute `./gradlew :webapp:war`. <br>
   **Or**: Download .war file from releases, unzip, edit `WEB-INF/classes/application.conf`, rezip and deploy.
4. Have fun!

## XML Format description

//TODO: Testdata is also provided here: 

- The basic patient record will follow a specific XML format that will look similar to this example: 
  
  ```XML
  <patient birthYear="2000" sex="M" id="123456">
    <case id="123456" from="2022-03-10T10:10:10" till="2022-03-10T10:10:10" type="S" admissionCause="V" admissionReason301="01" dischargeType301="01" state="E">
        <location id="111111" from="2022-03-10T10:10:10" till="2022-03-10T10:10:10" clinic="CLINIC" clinicP21="0100" ward="WARD"/>
        <location id="111112"/>
        <!--...-->
        <location id="111113"/>
  
        <labReport id="123456" source="SOURCE">
            <comment>This is a comment</comment>
            <request from="2022-03-10T10:10:10" sender="SENDER">VRE</request>
            <sample from="2022-03-10T10:10:10" bodySite="BODYSITE" bodySiteDisplay="BODYSITE" bodySiteLaterality="NONE" OPUS="ao" display="Anzeigename">
                <comment>A comment for the sample</comment>
                <analysis OPUS="avre" display="Selektivagar VRE">
                    <result OPUS="positiv" openTerm="pos"/>
                </analysis>
                <germ id="123456" number="1" SNOMED="90272000" openTerm="enco.faci" display="Enterococcus faecium">
                    <comment>A comment for the germ detection</comment>
                    <analysis><!--further analysis--></analysis>
                    <!--...-->
                    <!--antibiotic tests against the sample-->
                    <antibiotic LOINC="18862-3" openTerm="amoxi.cil   clav.aci" display="Amoxicillin/Clavulansäure">
                        <result string="R" LOINC="LA6676-6"/>
                    </antibiotic>
                    <antibiotic LOINC="18864-9" openTerm="ampi.cil" display="Ampicillin">
                        <result string="R" LOINC="LA6676-6"/>
                    </antibiotic>
                    <antibiotic LOINC="18865-6" openTerm="ampi.cil   sul.bac" display="Ampicillin/Sulbactam">
                        <result string="R" LOINC="LA6676-6"/>
                    </antibiotic>
                    <antibiotic LOINC="18906-8" openTerm="cipro.flox" display="Ciprofloxacin">
                        <result string="R" LOINC="LA6676-6"/>
                    </antibiotic>
                    <antibiotic LOINC="20629-2" openTerm="levo.flox" display="Levofloxacin">
                        <result string="R" LOINC="LA6676-6"/>
                    </antibiotic>
                    <antibiotic LOINC="29258-1" openTerm="line.zol" display="Linezolid">
                        <result string="R" LOINC="LA24225-7"/>
                    </antibiotic>
                    <antibiotic display="Norfloxacin">
                        <result string="R" LOINC="LA6676-6"/>
                    </antibiotic>
                    <antibiotic LOINC="18969-6" openTerm="pipera.cil" display="Piperacillin">
                        <result string="R" LOINC="LA6676-6"/>
                    </antibiotic>
                    <antibiotic LOINC="18970-4" openTerm="pipera.cil   tazo.bac" display="Piperacillin/Tazobactam">
                        <result string="R" LOINC="LA6676-6"/>
                    </antibiotic>
                    <antibiotic LOINC="18989-4" openTerm="teico" display="Teicoplanin">
                        <result string="R" LOINC="LA6676-6"/>
                    </antibiotic>
                    <antibiotic LOINC="42357-4" openTerm="tige.cyc" display="Tigecyclin">
                        <result string="R" LOINC="LA24225-7"/>
                    </antibiotic>
                    <antibiotic LOINC="19000-9" openTerm="vanco.myc" display="Vancomycin">
                        <result string="R" LOINC="LA6676-6"/>
                    </antibiotic>
                </germ>
            </sample>
        </labReport>
    </case>
  </patient>
  ```


In order to be usable for this project the record has to at least contain the following objects: 
- A patient with an id `<patient id="">`
- A corresponding case with an id `<case id="">`
- A lab report for this case with an id `<labReport id="">...</labReport>`
- The lab report has to contain information about the sender of the request
    ```XML
    <request from="2022-03-10T10:10:10" sender="SENDER">VRE</request>
    ```
- The lab report has to contain information about the sample. It will display the sample type and time the sample was
  collected
    ```XML
    <sample from="2022-03-10T10:10:10" bodySite="BODYSITE" bodySiteDisplay="BODYSITE" bodySiteLaterality="NONE" OPUS="ao" display="Anzeigename">...</sample>
    ```
- The sample has to contain a positive analysis for MRSA, MRGN or VRE //TODO: Muss es nicht, wird dann nur nicht
  gezaehlt

  ```XML
  <analysis OPUS="avre" display="Selektivagar VRE">
      <result OPUS="positiv" openTerm="pos"/>
  </analysis>
  ```

- The lab report has to have a germ with an id and the antibiotics analysis
  ```XML
      <germ id="" SNOMED="" openTerm="" display="">
          <antibiotic LOINC="" openTerm="" display="">
              <result string="" LOINC=""/>
          </antibiotic>
          ....
      </germ>
  ```
`antibiotic/result/@string` might be "R" (resistent), "S" (sensibel) or "I" (intermediär)

    
  


# ÖGD Report Tool

This tool is used to create the surveillance report according
to [§ 23 IfSG](https://www.gesetze-im-internet.de/ifsg/__23.html).

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
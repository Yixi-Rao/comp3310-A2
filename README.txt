How to run

a. Environment

    - my coding enviroument is Win10 and java version is openjdk version "11.0.4", 2019-07-16 OpenJDK Runtime Environment AdoptOpenJDK (build 11.0.4+11) OpenJDK 64-Bit Server VM AdoptOpenJDK (build 11.0.4+11, mixed mode)
    
    - And I choose the "Java HTML Parser" to parse the html, so this project will include a "jsoup-1.13.1.jar" in the lib.
      This is the jsoup website where I download the jar: https://jsoup.org/download

b. Instruction

    0. using the "javac" to packaging the all the code in src and also the "jar" in the lib

    1a. To run this web proxy, you should run this command line in terminal: java web_proxy www.bom.gov.au(or another host name) or
    
    1b. Run it in the Vscode or other IDE (I recommend Vscode), by using the setting to configue the input argument. Please add the jsoup.jar to the Referenced Libraries.

    2. open a browser or telnet, type http://localhost:3310/http://www.bom.gov.au/ and then the web_proxy will work.

c. Note

    - the javascript of this website will not work because I delete all the '<script>' in the html file.

This is a sample program for Wajam API. 

It performs the OAuth authentication for a Twitter or a Facebook user. If the user does not have a Wajam account, an account is created.

Clone this repository first, then make sure to replace the API_KEY and API_SECRET in src/com/wajam/api/sample/WajamApiSample.java with the values provided by Wajam.

To compile:
javac -classpath libs/scribe-1.3.0.jar:libs/commons-codec-1.4.jar src/com/wajam/api/sample/WajamApiSample.java

To run:
java -classpath libs/scribe-1.3.0.jar:libs/commons-codec-1.4.jar:src com.wajam.api.sample.WajamApiSample

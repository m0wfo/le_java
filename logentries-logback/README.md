logentries-logback
==================

To configure logback, you will need to perform the following:

    * (1) Install Logback (if you are not already using it).
    * (2) Install the Logentries Logback plugin.
    * (3) Configure the Logentries Logback plugin.

Maven Users
-----------

Place this in your pom.xml

	<dependencies>
	    <dependency>
	        <groupId>org.slf4j</groupId>
		<artifactId>slf4j-api</artifactId>
		<version>1.7.5</version>
	    </dependency>
	    <dependency>
    		<groupId>ch.qos.logback</groupId>
    		<artifactId>logback-classic</artifactId>
    		<version>0.9.30</version>
		</dependency>
	    <dependency>
	        <groupId>com.logentries</groupId>
	        <artifactId>logentries-appender</artifactId>
	        <version>RELEASE</version>
	    </dependency>
	</dependencies>

Configure the logback plugin
----------------------------

Download the required logback.xml config file from <a href="https://github.com/logentries/le_java/raw/master/configFiles/logback.xml">here</a>

Add this file to your project as it is the config which adds the plugin for logback to send logs to Logentries. This file should be in added to the classpath.

In this file, you will see the following:

	<?xml version="1.0" encoding="UTF-8" ?>
	<configuration>

  		<appender name="LE"
    		class="com.logentries.logback.LogentriesAppender">
			<Debug>False</Debug>
    		<Token>LOGENTRIES_TOKEN</Token>
    		<Ssl>False</Ssl>
    		<facility>USER</facility>
    		<layout>
      			<pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    		</layout>
  		</appender>

  		<root level="debug">
    		<appender-ref ref="LE" />
  		</root>
	</configuration>

Replace the value "LOGENTRIES_TOKEN" with the token UUID that is to the right of your newly created logfile.
    
For debugging purposes set the debug parameter to true.

Logging Messages
----------------

With that done, you are ready to send logs to Logentries.

In each class you wish to log from, enter the following using directives at the top if not already there:

	import org.slf4j.Logger;
	import org.slf4j.LoggerFactory;

Then create this object at class-level:

	private static Logger log = LoggerFactory.getLogger("logentries");

Now within your code in that class, you can log using logback as normal and it will log to Logentries.

Example:

	log.debug("Debugging Message");
	log.info("Informational message");
	log.warn("Warning Message");
package com.logentries.core;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Logentries Asynchronous Logger for integration with Java logging frameworks.
 *
 * VERSION: 1.1.9
 *
 * @author Viliam Holub
 * @author Mark Lacomber
 *
 */

public class AsyncLogger {

	/*
	 * Constants
	 */

	/** Current Version number of library/ */
	static final String VERSION = "1.1.9";
	/** Size of the internal event queue. */
	private static final int QUEUE_SIZE = 32768;
	/** UTF-8 output character set. */
	private static final Charset UTF8 = Charset.forName( "UTF-8");
	/** ASCII character set used by HTTP. */
	private static final Charset ASCII = Charset.forName( "US-ASCII");
	/** Minimal delay between attempts to reconnect in milliseconds. */
	private static final int MIN_DELAY = 100;
	/** Maximal delay between attempts to reconnect in milliseconds. */
	private static final int MAX_DELAY = 10000;
	/** LE appender signature - used for debugging messages. */
	private static final String LE = "LE ";
	/** Error message displayed when invalid API key is detected. */
	private static final String INVALID_TOKEN = "\n\nIt appears your LOGENTRIES_TOKEN parameter in log4j.xml is incorrect!\n\n";
	/** Key Value for Token Environment Variable. */
	private static final String CONFIG_TOKEN = "LOGENTRIES_TOKEN";
	/** Platform dependent line separator to check for. Supported in Java 1.6+ */
	private static final String LINE_SEP = System.getProperty("line_separator", "\n");
	/** Error message displayed when queue overflow occurs */
    private static final String QUEUE_OVERFLOW = "\n\nLogentries Buffer Queue Overflow. Message Dropped!\n\n";

    /*
	 * Fields
	 */

	/** Destination Token. */
	String token = "";
	/** Account Key. */
	String key = "";
	/** Account Log Location. */
	String location = "";
	/** HttpPut flag. */
	boolean httpPut = false;
	/** SSL/TLS flag. */
	boolean ssl = false;
	/** Debug flag. */
	boolean debug;
	/** Make local connection only. */
	boolean local;
	/** Indicator if the socket appender has been started. */
	boolean started;

	/** Asynchronous socket appender. */
	SocketAppender appender;
	/** Message queue. */
	ArrayBlockingQueue<String> queue;

	/*
	 * Public methods for parameters
	 */
	/**
	 * Sets the token
	 *
	 * @param token
	 */
	public void setToken( String token) {
		this.token = token;
		dbg( "Setting token to " + token);
	}

	/**
	 * Returns current token.
	 *
	 * @return current token
	 */
	public String getToken() {
		return token;
	}

	/**
	 *  Sets the HTTP PUT boolean flag. Send logs via HTTP PUT instead of default Token TCP
	 *
	 *  @param httpput HttpPut flag to set
	 */
	public void setHttpPut( boolean HttpPut) {
		this.httpPut = HttpPut;
	}

	/**
	 * Returns current HttpPut flag.
	 *
	 * @return true if HttpPut is enabled
	 */
	public boolean getHttpPut() {
		return this.httpPut;
	}

	/** Sets the ACCOUNT KEY value for HTTP PUT
	 *
	 * @param account_key
	 */
	public void setKey( String account_key)
	{
		this.key = account_key;
	}

	/**
	 * Gets the ACCOUNT KEY value for HTTP PUT
	 *
	 * @return key
	 */
	public String getKey()
	{
		return this.key;
	}

	/**
	 * Gets the LOCATION value for HTTP PUT
	 *
	 * @param log_location
	 */
	public void setLocation( String log_location)
	{
		this.location = log_location;
	}

	/**
	 * Gets the LOCATION value for HTTP PUT
	 *
	 * @return location
	 */
	public String getLocation()
	{
		return location;
	}

	/**
	 * Sets the SSL boolean flag
	 *
	 * @param ssl
	 */
	public void setSsl( boolean ssl)
	{
		this.ssl = ssl;
	}

	/**
	 * Gets the SSL boolean flag
	 *
	 * @return ssl
	 */
	public boolean getSsl()
	{
		return this.ssl;
	}

	/**
	 * Sets the debug flag. Appender in debug mode will print error messages on
	 * error console.
	 *
	 * @param debug debug flag to set
	 */
	public void setDebug( boolean debug) {
		this.debug = debug;
		dbg( "Setting debug to " + debug);
	}

	/**
	 * Returns current debug flag.
	 *
	 * @return true if debugging is enabled
	 */
	public boolean getDebug() {
		return debug;
	}

	/**
	 * Initializes asynchronous logging.
	 *
	 * @param local make local connection to API server for testing
	 */
	AsyncLogger( boolean local) {
		this.local = local;

		queue = new ArrayBlockingQueue<String>( QUEUE_SIZE);

		appender = new SocketAppender();
	}

	/**
	 * Initializes asynchronous logging.
	 */
	public AsyncLogger() {
		this( false);
	}

	/**
	 * Checks that the UUID is valid
	 */
	boolean checkValidUUID( String uuid){
		if("".equals(uuid))
			return false;

		UUID u = UUID.fromString(uuid);

		return u.toString().equals(uuid);
	}

	/**
	 * Try and retrieve environment variable for given key, return empty string if not found
	 */

	String getEnvVar( String key)
	{
		String envVal = System.getenv(key);

		return envVal != null ? envVal : "";
	}

	/**
	 * Checks that key and location are set.
	 */
	boolean checkCredentials() {


		if(!httpPut)
		{
			if (token.equals(CONFIG_TOKEN) || token.equals(""))
			{
				//Check if set in an environment variable, used with PaaS providers
				String envToken = getEnvVar( CONFIG_TOKEN);

				if (envToken == ""){
					dbg(INVALID_TOKEN);
					return false;
				}

				this.setToken(envToken);
			}

			return checkValidUUID(this.getToken());
		}else{
			if ( !checkValidUUID(this.getKey()) || this.getLocation().equals(""))
				return false;

			return true;
		}
	}

	/**
	 * Adds the data to internal queue to be sent over the network.
	 *
	 * It does not block. If the queue is full, it removes latest event first to
	 * make space.
	 *
	 * @param line line to append
	 */
	public void addLineToQueue( String line) {

		// Check that we have all parameters set and socket appender running
		if (!this.started && this.checkCredentials()) {
			dbg( "Starting Logentries asynchronous socket appender");
			appender.start();
			started = true;
		}

		dbg( "Queueing " + line);

		// Try to append data to queue
		if(!queue.offer( line))
		{
			queue.poll();
			if(!queue.offer( line))
				dbg( QUEUE_OVERFLOW);
		}
	}

	/**
	 * Closes all connections to Logentries.
	 */
	public void close() {
		appender.interrupt();
		started = false;
		dbg( "Closing Logentries asynchronous socket appender");
	}

	/**
	 * Prints the message given. Used for internal debugging.
	 *
	 * @param msg message to display
	 */
	void dbg( String msg) {
		if (debug)
            return;
//			LogLog.error( LE + msg);
	}

	/**
	 * Asynchronous over the socket appender.
	 *
	 * @author Viliam Holub
	 *
	 */
	class SocketAppender extends Thread {
		/** Random number generator for delays between reconnection attempts. */
		final Random random = new Random();
		/** Logentries Client for connecting to Logentries via HTTP or TCP. */
		LogentriesClient le_client;

		/**
		 * Initializes the socket appender.
		 */
		SocketAppender() {
			super( "Logentries Socket appender");
			// Don't block shut down
			setDaemon( true);
		}

		/**
		 * Opens connection to Logentries.
		 *
		 * @throws IOException
		 */
		void openConnection() throws IOException {
			try{
				if(this.le_client == null)
					this.le_client = new LogentriesClient(httpPut, ssl);

				this.le_client.connect();

				if(httpPut){
					final String f = "PUT /%s/hosts/%s/?realtime=1 HTTP/1.1\r\n\r\n";
					final String header = String.format( f, key, location);
					byte[] temp = header.getBytes( ASCII);
					this.le_client.write( temp, 0, temp.length);
				}

			}catch(Exception e){

			}
		}

		/**
		 * Tries to opens connection to Logentries until it succeeds.
		 *
		 * @throws InterruptedException
		 */
		void reopenConnection() throws InterruptedException {
			// Close the previous connection
			closeConnection();

			// Try to open the connection until we get through
			int root_delay = MIN_DELAY;
			while (true) {
				try {
					openConnection();

					// Success, leave
					return;
				} catch (IOException e) {
					// Get information if in debug mode
					if (debug) {
						dbg( "Unable to connect to Logentries");
						e.printStackTrace();
					}
				}

				// Wait between connection attempts
				root_delay *= 2;
				if (root_delay > MAX_DELAY)
					root_delay = MAX_DELAY;
				int wait_for = root_delay + random.nextInt( root_delay);
				dbg( "Waiting for " + wait_for + "ms");
				Thread.sleep( wait_for);
			}
		}

		/**
		 * Closes the connection. Ignores errors.
		 */
		void closeConnection() {

			if (this.le_client != null)
				this.le_client.close();

		}

		/**
		 * Initializes the connection and starts to log.
		 *
		 */
		@Override
		public void run() {
			try {
				// Open connection
				reopenConnection();

				// Send data in queue
				while (true) {
					// Take data from queue
					String data = queue.take();

					// Replace platform-independent carriage return with unicode line separator character to format multi-line events nicely in Logentries UI
					data = data.replace(LINE_SEP, "\u2028");

					String final_data = (!httpPut ? token + data : data) + '\n';

					// Get bytes of final event
					byte[] finalLine = final_data.getBytes(UTF8);

					// Send data, reconnect if needed
					while (true) {
						try {
							this.le_client.write( finalLine, 0, finalLine.length);
						} catch (IOException e) {
							// Reopen the lost connection
							reopenConnection();
							continue;
						}
						break;
					}
				}
			} catch (InterruptedException e) {
				// We got interrupted, stop
				dbg( "Asynchronous socket writer interrupted");
			}

			closeConnection();
		}
	}
}

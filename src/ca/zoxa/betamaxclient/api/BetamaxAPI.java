/**
 * Betamax API implementation
 */
package ca.zoxa.betamaxclient.api;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class BetamaxAPI implements Runnable
{
	// internal variables for api calls
	private final String url_sms   = "https://www.%s//myaccount/sendsms.php?username=%s&password=%s&from=%s&to=%s&text=%s";
	private final String url_phone = "https://www.%s//myaccount/makecall.php?username=%s&password=%s&from=%s&to=%s";

	// final api url to be called
	private String url;

	// Handler to respond for the thread
	private Handler handler;

	public static int ERROR_CODE_FAIL = 1;
	public static int ERROR_CODE_NOHOST = ERROR_CODE_FAIL + 1;

	public static String MSG_DATA_KEY_RESPONSE = "RESPONSE";
	public static String MSG_DATA_KEY_ERROR = "ERROR";

	// 3 second pause(,) to allow other phone number to pick up
	// in worse case scenario will need to use ; for wait
	private final String access_number_prefix = ",,,";
	private final String access_number_suffix = "#";

	// credentials
	private String service;
	private String username;
	private String password;
	private String number_from;
	private String country_prefix;
	private String access_number;

	public BetamaxAPI(final String service, final String username, final String password, final String number_from,
	        final String country_prefix)
	{
		this.service = service;
		this.username = username;
		this.password = password;
		this.number_from = number_from;
		this.country_prefix = country_prefix;
	}

	public BetamaxAPI(final String access_number, final String country_prefix)
	{
		this.country_prefix = country_prefix;
		this.access_number = access_number;
	}

	/**
	 * Correct phone number before calling Betamax Api
	 * 
	 * @param String
	 *            number
	 * @return String
	 */
	private String correctNumber(String number)
	{
		// remove
		number = number.replace( "smsto:", "" ).replace( "sms:", "" ).replace( " ", "" ).replace( "(0)", "" ).replace(
		        "-", "" ).replace( ".", "" ).replace( "(", "" ).replace( ")", "" );

		// replace european format with international
		if (number.startsWith( "00" ))
		{
			number = "+" + number.substring( 2, number.length() - 2 );
		}

		// short phone number style, without country code
		if (number.length() == 10 || !number.startsWith( "+" ))
		{
			number = this.country_prefix + number;
		}
		return number;
	}

	/**
	 * Public function to send SMS
	 * 
	 * @param String
	 *            sms_to number where sms should be send
	 * @param String
	 *            sms_message message that should be send
	 * @return void
	 */
	public void sendSMS(final String sms_to, final String sms_message, final Handler handler)
	{
		// build URL that should be called via httpGet
		this.url = String.format( url_sms, service, Uri.encode( username ), Uri.encode( password ),
		        correctNumber( number_from ), correctNumber( sms_to ), Uri.encode( sms_message ) );

		this.handler = handler;

		Thread thread = new Thread( this );
		thread.start();
	}

	/**
	 * Public function to initiate Phone to Phone calling
	 * 
	 * @param String
	 *            call_to number to call
	 * @return void
	 */
	public void makeCall(final String call_to, final Handler handler)
	{
		// build URL that should be called via httpGet
		this.url = String.format( url_phone, service, Uri.encode( username ), Uri.encode( password ),
		        correctNumber( number_from ), correctNumber( call_to ) );

		this.handler = handler;

		Thread thread = new Thread( this );
		thread.start();
	}

	/**
	 * Call API URL using HTTPGet and return response to handler
	 */
	public void run()
	{
		HttpClient httpClient = new DefaultHttpClient();

		Bundle data = new Bundle();

		try
		{
			HttpGet httpGet = new HttpGet( url );
			ResponseHandler<String> rh = new BasicResponseHandler();

			data.putString( MSG_DATA_KEY_RESPONSE, httpClient.execute( httpGet, rh ) );
		}
		catch (ClientProtocolException e)
		{
			data.putInt( MSG_DATA_KEY_ERROR, ERROR_CODE_FAIL );
		}
		catch (IOException e)
		{
			data.putInt( MSG_DATA_KEY_ERROR, ERROR_CODE_NOHOST );
		}

		Message msg = new Message();
		msg.setData( data );
		handler.sendMessage( msg );
	}

	/**
	 * Dial number via access number
	 * 
	 * @param Activity
	 *            act that will be used to call
	 * @param String
	 *            number
	 * @return void
	 */
	public void dialNumber(Activity act, String number)
	{
		// remove
		number = number.replace( " ", "" ).replace( "(0)", "" ).replace( "-", "" ).replace( ".", "" ).replace( "(", "" )
		        .replace( ")", "" );

		// short phone number style, without country code
		if (number.length() == 10 || !number.startsWith( "+" ))
		{
			number = this.country_prefix + number;
		}

		// replace international format with european
		if (number.startsWith( "+" ))
		{
			number = "00" + number.substring( 1, number.length() - 1 );
		}

		StringBuilder phonenumber = new StringBuilder( "tel:" );
		phonenumber.append( access_number );
		phonenumber.append( access_number_prefix );
		phonenumber.append( number );
		phonenumber.append( access_number_suffix );

		act.startActivityForResult( new Intent( Intent.ACTION_CALL, Uri.parse( "tel:" + phonenumber ) ), 0 );
	}

}

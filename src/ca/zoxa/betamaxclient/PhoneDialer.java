package ca.zoxa.betamaxclient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import ca.zoxa.betamaxclient.api.BetamaxAPI;

public class PhoneDialer extends Activity
{
	private TextView number;

	// DIALOG settings
	private final int DIALOG_WAIT = 1;
	private final int DIALOG_RESULT = DIALOG_WAIT + 1;
	private StringBuilder call_result;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.dialer );

		number = (TextView) findViewById( R.id.dialerNumber );

		((Button) findViewById( R.id.dialerBtnN1 )).setOnClickListener( new DialerBtnListener( "1" ) );
		((Button) findViewById( R.id.dialerBtnN2 )).setOnClickListener( new DialerBtnListener( "2" ) );
		((Button) findViewById( R.id.dialerBtnN3 )).setOnClickListener( new DialerBtnListener( "3" ) );
		((Button) findViewById( R.id.dialerBtnN4 )).setOnClickListener( new DialerBtnListener( "4" ) );
		((Button) findViewById( R.id.dialerBtnN5 )).setOnClickListener( new DialerBtnListener( "5" ) );
		((Button) findViewById( R.id.dialerBtnN6 )).setOnClickListener( new DialerBtnListener( "6" ) );
		((Button) findViewById( R.id.dialerBtnN7 )).setOnClickListener( new DialerBtnListener( "7" ) );
		((Button) findViewById( R.id.dialerBtnN8 )).setOnClickListener( new DialerBtnListener( "8" ) );
		((Button) findViewById( R.id.dialerBtnN9 )).setOnClickListener( new DialerBtnListener( "9" ) );
		((Button) findViewById( R.id.dialerBtnN0 )).setOnClickListener( new DialerBtnListener( "0" ) );
		((Button) findViewById( R.id.dialerBtnDel )).setOnClickListener( new DialerBtnListener( "c" ) );

		((Button) findViewById( R.id.dialerBtnDial )).setOnClickListener( new DialBtnListener() );
		((Button) findViewById( R.id.dialerBtnP2P )).setOnClickListener( new ConnectBtnListener() );

	}

	private class DialerBtnListener implements OnClickListener
	{
		private final String n;

		public DialerBtnListener(final String n)
		{
			this.n = n;
		}

		public void onClick(View v)
		{
			if (n == "c")
			{
				CharSequence tmp = number.getText();
				if (tmp.length() > 1)
				{
					number.setText( tmp.subSequence( 0, tmp.length() - 1 ) );
				}
			}
			else
			{
				number.setText( number.getText() + n );
			}
		}
	}

	private class DialBtnListener implements OnClickListener
	{
		public void onClick(View v)
		{
			String phone_number = number.getText().toString();

			if (phone_number.length() > 0)
			{
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( PhoneDialer.this );
				BetamaxAPI api = new BetamaxAPI( prefs.getString( "service", "" ), prefs.getString( "username", "" ),
				        prefs.getString( "password", "" ), prefs.getString( "number", "" ), prefs.getString(
				                "country_code", "" ) );

				api.dialNumber( PhoneDialer.this, phone_number );
			}
		}
	}

	private class ConnectBtnListener implements OnClickListener
	{
		public void onClick(View v)
		{
			String number_to = number.getText().toString().trim();
			if (number_to.length() > 0)
			{
				showDialog( DIALOG_WAIT );

				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( PhoneDialer.this );
				BetamaxAPI api = new BetamaxAPI( prefs.getString( "service", "" ), prefs.getString( "username", "" ),
				        prefs.getString( "password", "" ), prefs.getString( "number", "" ), prefs.getString(
				                "country_code", "" ) );

				api.makeCall( number_to, new ConnectHandler() );
			}
			else
			{
				if (number_to.length() == 0)
				{
					Toast.makeText( PhoneDialer.this, getResources().getString( R.string.error_dial_number_empty ),
					        Toast.LENGTH_SHORT ).show();
				}
			}
		}
	}

	private class ConnectHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			call_result = new StringBuilder();

			Bundle data = msg.getData();
			int error = data.getInt( BetamaxAPI.MSG_DATA_KEY_ERROR );
			if (error == 0)
			{
				String response = data.getString( BetamaxAPI.MSG_DATA_KEY_RESPONSE );
				Boolean success = response.indexOf( "success" ) > 0;
				if (success)
				{
					call_result.append( getString( R.string.response_ok_call ) );
				}
				else
				{
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( PhoneDialer.this );

					Pattern pattern = Pattern.compile( "<description>(.*?)</description>" );
					Matcher matcher = pattern.matcher( response );
					String error_desr = null;
					if (matcher.find())
					{
						error_desr = matcher.group( 1 );
					}
					else
					{
						error_desr = "unknown";
					}

					call_result.append( getString( R.string.error_response_msg, prefs.getString( "service", "" ), prefs
					        .getString( "username", "" ), prefs.getString( "number", "" ), number.getText().toString()
					        .trim(), error_desr ) );
				}
			}
			else if (error == BetamaxAPI.ERROR_CODE_FAIL)
			{
				call_result.append( getString( R.string.error_exc_host_fail ) );
			}
			else if (error == BetamaxAPI.ERROR_CODE_NOHOST)
			{
				call_result.append( getString( R.string.error_exc_host_fail ) );
			}

			try
			{
				// try to close old dialog
				dismissDialog( DIALOG_WAIT );
			}
			catch (IllegalArgumentException e)
			{
			}

			showDialog( DIALOG_RESULT );

			// Toast.makeText( PhoneDialer.this, call_result.toString(),
			// Toast.LENGTH_LONG ).show();
		}
	}

	@Override
	protected Dialog onCreateDialog(int id)
	{
		switch (id) {
			case DIALOG_WAIT: {
				ProgressDialog dialog = new ProgressDialog( this );
				dialog.setTitle( R.string.dialog_dial );
				dialog.setMessage( getString( R.string.dialog_wait ) );
				dialog.setIndeterminate( true );
				dialog.setCancelable( true );
				return dialog;
			}
			case DIALOG_RESULT: {
				AlertDialog dialog = new AlertDialog.Builder( this ).create();
				dialog.setTitle( R.string.dialog_dial );
				dialog.setMessage( call_result.toString() );
				dialog.setCancelable( true );
				return dialog;
			}

		}
		return null;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog)
	{
		if (id == DIALOG_RESULT)
		{
			((AlertDialog) dialog).setMessage( call_result.toString() );
		}
	}
}

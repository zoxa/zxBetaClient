package ca.zoxa.betamaxclient;

import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import ca.zoxa.betamaxclient.api.BetamaxAPI;

public class SmsCompose extends Activity
{
	public EditText sms_msg;
	public TextView chars_count;
	public EditText number;

	// DIALOG settings
	private final int DIALOG_WAIT = 1;
	private final int DIALOG_RESULT = DIALOG_WAIT + 1;
	private StringBuilder call_result;

	private final int phoneRequestCode = 0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );

		// start new sms sender
		composeNewMessage();
	}

	private void composeNewMessage()
	{
		setContentView( R.layout.sms_composer );

		// bind number
		number = (AutoCompleteTextView) findViewById( R.id.number );

		// bind AutoComplete for number
		// AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(
		// R.id.number );
		// textView.setAdapter( new ContactListAdapter( this, null ) );

		// if Activity was called via intent fill the number
		Intent intent = getIntent();
		if (intent.getData() != null)
		{
			String param = this.correctPhone( intent.getDataString() );
			number.setText( URLDecoder.decode( param ) );
		}

		chars_count = (TextView) findViewById( R.id.chars_count );

		sms_msg = (EditText) findViewById( R.id.sms_message );
		SMSWatcher smswatch = new SMSWatcher();
		sms_msg.addTextChangedListener( smswatch );
		smswatch.onTextChanged( sms_msg.getText().toString(), 0, 0, 0 );
		

		ImageButton getContactsButton = (ImageButton) findViewById( R.id.contacts_button );
		getContactsButton.setOnClickListener( new OnClickListener()
		{
			public void onClick(View v)
			{
				startActivityForResult( new Intent( Intent.ACTION_GET_CONTENT, Phone.CONTENT_URI ), phoneRequestCode );
			}
		} );

		Button sendMessageButton = (Button) findViewById( R.id.btnSend );
		sendMessageButton.setOnClickListener( new SendSMS() );

	}

	private String correctPhone(String cNumber)
	{
		return cNumber.replace( "smsto:", "" ).replace( "sms:", "" ).replace( " ", "" ).replace( "(0)", "" ).replace(
		        "-", "" ).replace( ".", "" ).replace( "(", "" ).replace( ")", "" );
	}

	/**
	 * Listener for Send Button
	 * 
	 * @author zoxa
	 */
	private class SendSMS implements OnClickListener
	{

		public void onClick(View v)
		{
			String sms_to = number.getText().toString().trim();
			String sms_text = sms_msg.getText().toString().trim();

			if (sms_to.length() > 0 && sms_text.length() > 0)
			{
				showDialog( DIALOG_WAIT );

				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( SmsCompose.this );
				BetamaxAPI api = new BetamaxAPI( prefs.getString( "service", "" ), prefs.getString( "username", "" ),
				        prefs.getString( "password", "" ), prefs.getString( "number", "" ), prefs.getString(
				                "country_code", "" ) );

				api.sendSMS( sms_to, sms_text, new SMSHandler() );
			}
			else
			{
				if (sms_to.length() == 0)
				{
					Toast.makeText( SmsCompose.this, getResources().getString( R.string.error_sms_to_empty ),
					        Toast.LENGTH_SHORT ).show();
				}
				if (sms_text.length() == 0)
				{
					Toast.makeText( SmsCompose.this, getResources().getString( R.string.error_sms_msg_empty ),
					        Toast.LENGTH_SHORT ).show();
				}
			}
		}
	}

	private class SMSHandler extends Handler
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
					call_result.append( getString( R.string.response_ok_sms ) );
				}
				else
				{
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( SmsCompose.this );

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

			// Toast.makeText( SmsList.this, call_result.toString(),
			// Toast.LENGTH_LONG ).show();
		}
	}

	/**
	 * Watcher for SMS Message box
	 * 
	 * @author zoxa
	 */
	private class SMSWatcher implements TextWatcher
	{
		private int smsLength = 160;

		public void onTextChanged(CharSequence s, int start, int before, int count)
		{
			int length = s.toString().trim().length();

			// TODO: smsLength should be replaced with proper calculation
			int page = length == 0 ? 0 : (1 + length / this.smsLength);

			chars_count.setText( getString( R.string.sms_chars_count, length, page ) );
		}

		public void afterTextChanged(Editable s)
		{
		}

		public void beforeTextChanged(CharSequence s, int start, int count, int after)
		{
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == phoneRequestCode)
		{
			if (resultCode == RESULT_OK)
			{
				// get phone number associated with contact row picket
				Cursor cur = this.managedQuery( data.getData(), new String[] { Phone.NUMBER }, null, null, null );
				if (cur.moveToFirst())
				{
					String cNumber = correctPhone( cur.getString( cur.getColumnIndex( Phone.NUMBER ) ) );
					number.setText( cNumber );
				}
				else
				{
					number.setText( "" );
				}
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id)
	{
		switch (id) {
			case DIALOG_WAIT: {
				ProgressDialog dialog = new ProgressDialog( this );
				dialog.setTitle( R.string.dialog_sms );
				dialog.setMessage( getString( R.string.dialog_wait ) );
				dialog.setIndeterminate( true );
				dialog.setCancelable( true );
				return dialog;
			}
			case DIALOG_RESULT: {
				AlertDialog dialog = new AlertDialog.Builder( this ).create();
				dialog.setTitle( R.string.dialog_sms );
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

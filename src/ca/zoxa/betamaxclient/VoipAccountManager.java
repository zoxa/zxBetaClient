package ca.zoxa.betamaxclient;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;

public class VoipAccountManager extends PreferenceActivity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );
		addPreferencesFromResource( R.xml.preferences_account );

		EditTextPreference pref;

		pref = (EditTextPreference) findPreference( "service" );
		pref.setSummary( pref.getText() );

		pref = (EditTextPreference) findPreference( "username" );
		pref.setSummary( pref.getText() );

		pref = (EditTextPreference) findPreference( "password" );
		pref.setSummary( (pref.getText().length() > 0 ? "***" : "") );

		pref = (EditTextPreference) findPreference( "number" );
		pref.setSummary( pref.getText() );

		pref = (EditTextPreference) findPreference( "access_number" );
		pref.setSummary( pref.getText() );

		pref = (EditTextPreference) findPreference( "country_code" );
		pref.setSummary( pref.getText() );
	}
}

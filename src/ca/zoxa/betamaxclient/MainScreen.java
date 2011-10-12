package ca.zoxa.betamaxclient;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;

public class MainScreen extends TabActivity
{
	// The activity TabHost
	private TabHost tabHost;

	// current Tab selected
	private final int startTab = 0;

	private final int MENU_OPT = 1;
	private final int MENU_ABOUT = MENU_OPT + 1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.main );

		// Resource object to get Drawables
		Resources res = getResources();
		this.tabHost = getTabHost();

		// Initialize a TabSpec for each tab and add it to the TabHost
		tabHost.addTab( tabHost.newTabSpec( "phone" ).setIndicator( res.getString( R.string.app_name_dialer ),
		        res.getDrawable( R.drawable.ic_tab_dialer ) ).setContent(
		        new Intent().setClass( this, PhoneDialer.class ) ) );

		tabHost.addTab( tabHost.newTabSpec( "sms" ).setIndicator( res.getString( R.string.app_name_sms ),
		        res.getDrawable( R.drawable.ic_tab_sms ) ).setContent( new Intent().setClass( this, SmsCompose.class ) ) );

		tabHost.setCurrentTab( this.startTab );
	}

	/**
	 * Add menu to tabs
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu( menu );
		menu.add( 0, MENU_OPT, Menu.NONE, R.string.menu_opt ).setIcon( android.R.drawable.ic_menu_preferences );
		menu.add( 1, MENU_ABOUT, Menu.NONE, R.string.menu_about ).setIcon( android.R.drawable.ic_menu_info_details );
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
			case MENU_OPT:
				startActivity( new Intent( this, VoipAccountManager.class ) );
				break;

			case MENU_ABOUT:
				// About about = new About();
				// about.showAboutDialog( this );
				break;
		}
		return (super.onOptionsItemSelected( item ));
	}
}
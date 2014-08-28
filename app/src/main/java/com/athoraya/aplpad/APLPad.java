package com.athoraya.aplpad;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.athoraya.aplkeys.R;

/**
 * Created by gil on 27/08/2014.
 */
public class APLPad extends Activity {
    private EditText mEditText;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.aplpad_activity);
        PreferenceManager.setDefaultValues(this, R.xml.aplpad_preferences, false);

        mEditText = (EditText) this.findViewById(R.id.edit_message);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    protected void onStart(){
        super.onStart();
        loadPref();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, APLPadSettings.class);
                startActivityForResult(intent, 0);
                return true;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        loadPref();
    }

    private void loadPref(){
        String font_name = mPrefs.getString(APLPadSettings.KEY_PREF_FONT_NAME, "");
        if (font_name != "") {
            mEditText.setTypeface(Typeface.createFromAsset(getAssets(), font_name));
        }
    }

}

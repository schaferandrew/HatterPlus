package edu.msu.cse476.cloudhatter;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

import edu.msu.cse476.cloudhatter.R;


public class HatterActivity extends ActionBarActivity {
    /**
     * Key for the parameters in the bundle
     */
    private static final String PARAMETERS = "parameters";

    /**
     * Request code when selecting a picture
     */
    public static final int SELECT_PICTURE = 1;

    /**
     * Request code when selecting a color
     */
    private static final int SELECT_COLOR = 2;

    /**
     * The hatter view object
     */
    private HatterView hatterView = null;

    /**
     * The color select button
     */
    private Button colorButton = null;

    /**
     * The feather checkbox
     */
    private CheckBox featherCheck = null;

    /**
     * The hat choice spinner
     */
    private Spinner spinner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hatter);

        /*
         * Get some of the views we'll keep around
         */
        hatterView = (HatterView)findViewById(R.id.hatterView);
        colorButton = (Button)findViewById(R.id.buttonColor);
        featherCheck = (CheckBox)findViewById(R.id.checkFeather);
        spinner = (Spinner) findViewById(R.id.spinnerHat);

         /*
         * Set up the spinner
         */

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.hats_spinner, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View view,
                                       int pos, long id) {
                hatterView.setHat(pos);
                colorButton.setEnabled(pos == HatterView.HAT_CUSTOM);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }

        });

        /*
         * Restore any state
         */
        if(savedInstanceState != null) {
            hatterView.getFromBundle(PARAMETERS, savedInstanceState);
        }

        /**
         * Ensure the user interface is up to date
         */
        updateUI();
    }

    /**
     * Ensure the user interface components match the current state
     */
    public void updateUI() {
        spinner.setSelection(hatterView.getHat());
        featherCheck.setChecked(hatterView.getFeather());
        colorButton.setEnabled(hatterView.getHat() == HatterView.HAT_CUSTOM);
    }

    /**
     * Called when it is time to create the options menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_hatter, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        hatterView.putToBundle(PARAMETERS, outState);
    }


    /**
     * Handle the picture select button
     * @param view Button view
     */
    public void onPictureSelect(View view) {
        // Bring up the picture selection dialog box
        PictureDlg dialog = new PictureDlg();
        dialog.show(getSupportFragmentManager(), null);
    }

    /**
     * Handle the color select button
     * @param view Button view
     */
    public void onColorSelect(View view) {
        // Get a picture from the gallery
        Intent intent = new Intent(this, ColorSelectActivity.class);
        startActivityForResult(intent, SELECT_COLOR);
    }

    /**
     * Handle the feather checkbox change
     * @param view Feather checkbox view
     */
    public void onFeatherClicked(View view) {
        CheckBox cb = (CheckBox)findViewById(R.id.checkFeather);
        hatterView.setFeather(cb.isChecked());
        updateUI();
    }

    /**
     * Function called when we get a result from some external
     * activity called with startActivityForResult()
     * @param requestCode the request code we sent to the activity
     * @param resultCode a result of from the activity - ok or cancelled
     * @param data data from the activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == SELECT_PICTURE && resultCode == Activity.RESULT_OK) {
            // Response from the picture selection activity
            Uri imageUri = data.getData();
            setUri(imageUri);
        } else if(requestCode == SELECT_COLOR && resultCode == RESULT_OK) {
            // Response from the color selection activity
            int color = data.getIntExtra(ColorSelectActivity.COLOR, Color.BLACK);
            hatterView.setColor(color);
        }
    }

    public void setUri(Uri uri) {
        hatterView.setImageUri(uri);
    }

    /**
     * Handle options menu selections
     * @param item Menu item selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_about:
                AboutDlg dlg = new AboutDlg();
                dlg.show(getSupportFragmentManager(), "About");
                return true;

            case R.id.menu_reset:
                hatterView.reset();
                return true;

            case R.id.menu_load:
                LoadDlg dlg2 = new LoadDlg();
                dlg2.show(getFragmentManager(), "load");
                return true;
            case R.id.menu_save:
                SaveDlg dlg3 = new SaveDlg();
                dlg3.show(getFragmentManager(),"save");
                return true;
            case R.id.menu_delete:
                DeleteDlg dlg4 = new DeleteDlg();
                dlg4.show(getFragmentManager(),"delete");
        }
        return super.onOptionsItemSelected(item);
    }
}

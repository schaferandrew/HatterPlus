package edu.msu.cse476.cloudhatter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Xml;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Andrew on 10/31/17.
 */

public class LoadingDlg extends DialogFragment {
    public String getCatId() {
        return catId;
    }

    public void setCatId(String catId) {
        this.catId = catId;
    }

    /**
     * Id for the image we are loading
     */
    private String catId;

    /**
     * Set true if we want to cancel
     */
    private volatile boolean cancel = false;

    private final static String ID = "id";


    /**
     * Create the dialog box
     */
    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        if(bundle != null) {
            catId = bundle.getString(ID);
        }
        cancel = false;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Set the title
        builder.setTitle(R.string.loading);

        builder.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        cancel = true;
                    }
                });


        // Create the dialog box
        final AlertDialog dlg = builder.create();

        // Get a reference to the view we are going to load into
        final HatterView view = (HatterView)getActivity().findViewById(R.id.hatterView);

        /*
         * Create a thread to load the hatting from the cloud
         */
        new Thread(new Runnable() {

            @Override
            public void run() {
                // Create a cloud object and get the XML
                Cloud cloud = new Cloud();
                InputStream stream = cloud.openFromCloud(catId);

                // Test for an error
                boolean fail = stream == null;
                if(!fail) {
                    try {

                        if(cancel){
                            return;
                        }

                        XmlPullParser xml = Xml.newPullParser();
                        xml.setInput(stream, "UTF-8");

                        xml.nextTag();      // Advance to first tag
                        xml.require(XmlPullParser.START_TAG, null, "hatter");
                        String status = xml.getAttributeValue(null, "status");
                        if(status.equals("yes")) {

                            while(xml.nextTag() == XmlPullParser.START_TAG) {
                                if(xml.getName().equals("hatting")) {
                                    if(cancel){
                                        return;
                                    }

                                    // do something with the hatting tag...
                                    view.loadXml(xml);
                                    break;
                                }

                                Cloud.skipToEndTag(xml);
                            }
                        } else {
                            fail = true;
                        }

                    } catch(IOException ex) {
                        fail = true;
                    } catch(XmlPullParserException ex) {
                        fail = true;
                    } finally {
                        try {
                            stream.close();
                        } catch(IOException ex) {
                        }
                    }
                }
                final boolean fail1 = fail;
                view.post(new Runnable() {

                    @Override
                    public void run() {
                        dlg.dismiss();
                        if(fail1) {
                            Toast.makeText(view.getContext(),
                                    R.string.loading_fail,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // Success!
                            if(getActivity() instanceof HatterActivity) {
                                ((HatterActivity)getActivity()).updateUI();
                            }
                        }

                    }

                });
            }
        }).start();

        return dlg;
    }

    /**
     * Save the instance state
     */
    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);

        bundle.putString(ID, catId);
    }

    /**
     * Called when the view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancel = true;
    }
}

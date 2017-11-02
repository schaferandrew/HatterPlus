package edu.msu.cse476.cloudhatter;

import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by Andrew on 10/31/17.
 */

public class Cloud {
    private static final String MAGIC = "NechAtHa6RuzeR8x";
    private static final String USER = "schaf170";
    private static final String PASSWORD = "12345678";
    private static final String CATALOG_URL = "https://facweb.cse.msu.edu/ameli/cse476x/hatter-cat.php";
    private static final String SAVE_URL = "https://facweb.cse.msu.edu/ameli/cse476x/hatter-save.php";
    private static final String DELETE_URL = "https://facweb.cse.msu.edu/ameli/cse476x/hatter-delete.php";
    private static final String LOAD_URL = "https://facweb.cse.msu.edu/ameli/cse476x/hatter-load.php";
    private static final String UTF8 = "UTF-8";

    /**
     * Skip the XML parser to the end tag for whatever
     * tag we are currently within.
     * @param xml the parser
     * @throws IOException
     * @throws XmlPullParserException
     */
    public static void skipToEndTag(XmlPullParser xml)
            throws IOException, XmlPullParserException {
        int tag;
        do
        {
            tag = xml.next();
            if(tag == XmlPullParser.START_TAG) {
                // Recurse over any start tag
                skipToEndTag(xml);
            }
        } while(tag != XmlPullParser.END_TAG &&
                tag != XmlPullParser.END_DOCUMENT);
    }

    /**
     * Nested class to store one catalog row
     */
    private static class Item {
        public String name = "";
        public String id = "";
    }

    /**
     * An adapter so that list boxes can display a list of filenames from
     * the cloud server.
     */
    public static class CatalogAdapter extends BaseAdapter {

        /**
         * The items we display in the list box. Initially this is
         * null until we get items from the server.
         */
        private ArrayList<Item> items = new ArrayList<Item>();

        /**
         * Constructor
         */
        public CatalogAdapter(final View view) {
            // Create a thread to load the catalog
            new Thread(new Runnable() {

                @Override
                public void run() {
                    ArrayList<Item> newItems = getCatalog();
                    if (newItems != null) {

                        items = newItems;

                        view.post(new Runnable() {

                            @Override
                            public void run() {
                                // Tell the adapter the data set has been changed
                                notifyDataSetChanged();
                            }

                        });
                    } else {
                        // Error condition!
                        view.post(new Runnable() {

                            @Override
                            public void run() {
                                Toast.makeText(view.getContext(), R.string.catalog_fail, Toast.LENGTH_SHORT).show();
                            }

                        });
                    }
                }
            }).start();
        }

        public ArrayList<Item> getCatalog() {
            ArrayList<Item> newItems = new ArrayList<Item>();

            // Create a GET query
            String query = CATALOG_URL + "?user=" + USER +
                    "&magic=" + MAGIC + "&pw=" + PASSWORD;

            /**
             * Open the connection
             */
            InputStream stream = null;
            try {
                URL url = new URL(query);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                int responseCode = conn.getResponseCode();
                if(responseCode != HttpURLConnection.HTTP_OK) {
                    return null;
                }

                stream = conn.getInputStream();

            } catch (MalformedURLException e) {
                // Should never happen
                return null;
            } catch (IOException ex) {
                return null;
            }

            /**
             * Create an XML parser for the result
             */
            try {
                XmlPullParser xml = Xml.newPullParser();
                xml.setInput(stream, "UTF-8");

                xml.nextTag();      // Advance to first tag
                xml.require(XmlPullParser.START_TAG, null, "hatter");

                String status = xml.getAttributeValue(null, "status");
                if(status.equals("no")) {
                    return null;
                }

                while(xml.nextTag() == XmlPullParser.START_TAG) {
                    if(xml.getName().equals("hatting")) {
                        Item item = new Item();
                        item.name = xml.getAttributeValue(null, "name");
                        item.id = xml.getAttributeValue(null, "id");
                        newItems.add(item);
                    }

                    skipToEndTag(xml);
                }

                // We are done
            } catch(XmlPullParserException ex) {
                return null;
            } catch(IOException ex) {
                return null;
            }finally {
                try {
                    stream.close();
                } catch(IOException ex) {

                }
            }

            return newItems;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Item getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if(view == null) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.catalog_item, parent, false);
            }

            TextView tv = (TextView)view.findViewById(R.id.textItem);
            tv.setText(items.get(position).name);


            return view;        }


        public String getId(int position) {
            return items.get(position).id;
        }
    }


    /**
     * Open a connection to a hatting in the cloud.
     * @param id id for the hatting
     * @return reference to an input stream or null if this fails
     */
    public InputStream openFromCloud(final String id) {
        // Create a get query
        String query = LOAD_URL + "?user=" + USER + "&magic=" + MAGIC + "&pw=" + PASSWORD + "&id=" + id;

        try {
            URL url = new URL(query);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            int responseCode = conn.getResponseCode();
            if(responseCode != HttpURLConnection.HTTP_OK) {
                return null;
            }

            InputStream stream = conn.getInputStream();
            return stream;

        } catch (MalformedURLException e) {
            // Should never happen
            return null;
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * Save a hatting to the cloud.
     * This should be run in a thread.
     * @param name name to save under
     * @param view view we are getting the data from
     * @return true if successful
     */
    public boolean saveToCloud(String name, HatterView view) {
        name = name.trim();
        if(name.length() == 0) {
            return false;
        }
        // Create an XML packet with the information about the current image
        XmlSerializer xml = Xml.newSerializer();
        StringWriter writer = new StringWriter();

        try {
            xml.setOutput(writer);

            xml.startDocument("UTF-8", true);

            xml.startTag(null, "hatter");
            xml.attribute(null, "user", USER);
            xml.attribute(null, "pw", PASSWORD);
            xml.attribute(null, "magic", MAGIC);

            view.saveXml(name, xml);

            xml.endTag(null, "hatter");

            xml.endDocument();

        } catch (IOException e) {
            // This won't occur when writing to a string
            return false;
        }

        final String xmlStr = writer.toString();
        /*
         * Convert the XML into HTTP POST data
         */
        String postDataStr;
        try {
            postDataStr = "xml=" + URLEncoder.encode(xmlStr, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return false;
        }

        /*
         * Send the data to the server
         */
        byte[] postData = postDataStr.getBytes();

        InputStream stream = null;
        try {
            URL url = new URL(SAVE_URL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Length", Integer.toString(postData.length));
            conn.setUseCaches(false);

            OutputStream out = conn.getOutputStream();
            out.write(postData);
            out.close();

            int responseCode = conn.getResponseCode();
            if(responseCode != HttpURLConnection.HTTP_OK) {
                return false;
            }

            stream = conn.getInputStream();

            /**
             * Create an XML parser for the result
             */
            try {
                XmlPullParser xmlR = Xml.newPullParser();
                xmlR.setInput(stream, UTF8);

                xmlR.nextTag();      // Advance to first tag
                xmlR.require(XmlPullParser.START_TAG, null, "hatter");

                String status = xmlR.getAttributeValue(null, "status");
                if(status.equals("no")) {
                    return false;
                }

                // We are done
            } catch(XmlPullParserException ex) {
                return false;
            } catch(IOException ex) {
                return false;
            }

        } catch (MalformedURLException e) {
            return false;
        } catch (IOException ex) {
            return false;
        } finally {
            if(stream != null) {
                try {
                    stream.close();
                } catch(IOException ex) {
                    // Fail silently
                }
            }
        }

        return true;
    }
}

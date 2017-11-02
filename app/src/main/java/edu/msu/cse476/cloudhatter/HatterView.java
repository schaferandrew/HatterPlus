package edu.msu.cse476.cloudhatter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

import edu.msu.cse476.cloudhatter.R;


/**
 * The hatter view. Displays an image with a hat drawn over
 * it that we can manipulate.
 */
public class HatterView extends View {
    /*
     * The ID values for each of the hat types. The values must
     * match the index into the array hats_spinner in arrays.xml.
     */
    public static final int HAT_BLACK = 0;
    public static final int HAT_GRAY = 1;
    public static final int HAT_CUSTOM = 2;

    /**
     * Local class to handle the touch status for one touch.
     * We will have one object of this type for each of the
     * two possible touches.
     */
    private class Touch {
        /**
         * Touch id
         */
        public int id = -1;

        /**
         * Current x location
         */
        public float x = 0;

        /**
         * Current y location
         */
        public float y = 0;

        /**
         * Previous x location
         */
        public float lastX = 0;

        /**
         * Previous y location
         */
        public float lastY = 0;

        /**
         * Change in x value from previous
         */
        public float dX = 0;

        /**
         * Change in y value from previous
         */
        public float dY = 0;

        /**
         * Copy the current values to the previous values
         */
        public void copyToLast() {
            lastX = x;
            lastY = y;
        }

        /**
         * Compute the values of dX and dY
         */
        public void computeDeltas() {
            dX = x - lastX;
            dY = y - lastY;
        }
    }

    /**
     * Current program state
     */
    private static class Parameters implements Serializable {
        /**
         * Serialization ID value
         */
        private static final long serialVersionUID = -6692441979811271612L;

        /**
         * Uri for the image if one exists. The Uri is a string
         * representation.
         */
        public String imageUri = null;

        /**
         * X location of hat relative to the image
         */
        public float hatX = 0;

        /**
         * Y location of hat relative to the image
         */
        public float hatY = 0;

        /**
         * Hat scale, also relative to the image
         */
        public float hatScale = 1;

        /**
         * Hat rotation angle
         */
        public float hatAngle = 0;

        /**
         * Do we draw a feather?
         */
        public boolean feather = false;

        /**
         * The current hat type
         */
        public int hat = HAT_BLACK;

        /**
         * The hat color for HAT_CUSTOM
         */
        public int color = Color.WHITE;
    }

    /**
     * The current parameters
     */
    private Parameters params = new Parameters();

    /**
     * The image bitmap. None initially.
     */
    private Bitmap imageBitmap = null;

    /**
     * Image drawing scale
     */
    private float imageScale = 1;

    /**
     * Image left margin in pixels
     */
    private float marginLeft = 0;

    /**
     * Image top margin in pixels
     */
    private float marginTop = 0;

    /**
     * The bitmap to draw the hat
     */
    private Bitmap hatBitmap = null;

    /**
     * The bitmap to draw the hat band. We draw this
     * only when drawing the custom color hat, so we
     * don't color the hat band
     */
    private Bitmap hatbandBitmap = null;

    /**
     * The bitmap for the feather
     */
    private Bitmap featherBitmap = null;

    /**
     * First touch status
     */
    private Touch touch1 = new Touch();

    /**
     * Second touch status
     */
    private Touch touch2 = new Touch();

    /**
     * Paint to use when drawing the custom color hat
     */
    private Paint customPaint;

    public HatterView(Context context) {
        super(context);
        init(context);
    }

    public HatterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HatterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    /**
     * Initialize the view, loading the currently selected hat
     * @param context
     */
    private void init(Context context) {
        setHat(HAT_BLACK);
        customPaint = new Paint();
        customPaint.setColorFilter(new LightingColorFilter(params.color, 0));
    }


    /**
     * Get the installed image path
     * @return path or null if none
     */
    public String getImageUri() {
        return params.imageUri;
    }

    /**
     * Set an image URI based on a string representation.
     * @param imageUri Uri for the image file
     */
    public void setImageUri(String imageUri) {
        // We'll clear the old URI until we know a new one
        imageBitmap = null;
        params.imageUri = "";

        if(imageUri != null) {
            Uri uri = Uri.parse(imageUri);
            setImageUri(uri);
        }

        invalidate();
    }

    /**
     * Set the image URI. Load an image from any source,
     * including external sources.
     * @param uri URI for the image
     */
    public void setImageUri(final Uri uri) {
        final String scheme = uri.getScheme();
        if(scheme == null) {
            // If no scheme, we have no image
            imageBitmap = null;
            params.imageUri = "";
            return;
        }

        new Thread(new Runnable() {

            /**
             * Run the thread that loads the image
             */
            @Override
            public void run() {

                boolean success = false;
                try {
                    // This code has been modified to load content either
                    // from a content provider (local) or an arbitrary URL
                    // (internet)
                    InputStream input;
                    if(scheme.equals("content")) {
                        input = getContext().getContentResolver().openInputStream(uri);
                    } else {
                        URL url = new URL(uri.toString());
                        input = url.openStream();
                    }

                    imageBitmap = BitmapFactory.decodeStream(input);
                    input.close();
                    params.imageUri = uri.toString();
                    success = true;

                } catch(FileNotFoundException ex) {
                    // All of these are empty, since we
                    // indicate an exception by leaving success
                    // set to false.
                } catch(MalformedURLException ex) {
                } catch(IOException ex) {
                }

                if(!success) {
                    imageBitmap = null;
                    params.imageUri = "";
                }

                /**
                 * Post execute in the UI thread to invalidate and
                 * force a redraw.
                 */
                post(new Runnable() {

                    @Override
                    public void run() {
                        invalidate();
                    }
                });
            }

        }).start();
    }


    /**
     * Handle a draw event
     * @param canvas canvas to draw on.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // If there is no image to draw, we do nothing
        if(imageBitmap == null) {
            return;
        }

        /*
         * Determine the margins and scale to draw the image
         * centered and scaled to maximum size on any display
         */
        // Get the canvas size
        float wid = canvas.getWidth();
        float hit = canvas.getHeight();

        // What would be the scale to draw the where it fits both
        // horizontally and vertically?
        float scaleH = wid / imageBitmap.getWidth();
        float scaleV = hit / imageBitmap.getHeight();

        // Use the lesser of the two
        imageScale = scaleH < scaleV ? scaleH : scaleV;

        // What is the scaled image size?
        float iWid = imageScale * imageBitmap.getWidth();
        float iHit = imageScale * imageBitmap.getHeight();

        // Determine the top and left margins to center
        marginLeft = (wid - iWid) / 2;
        marginTop = (hit - iHit) / 2;

        /*
         * Draw the image bitmap
         */
        canvas.save();
        canvas.translate(marginLeft,  marginTop);
        canvas.scale(imageScale, imageScale);
        canvas.drawBitmap(imageBitmap, 0, 0, null);

        /*
         * Draw the hat
         */
        canvas.translate(params.hatX,  params.hatY);
        canvas.scale(params.hatScale, params.hatScale);
        canvas.rotate(params.hatAngle);

        if(params.hat == HAT_CUSTOM) {
            canvas.drawBitmap(hatBitmap, 0, 0, customPaint);
        } else {
            canvas.drawBitmap(hatBitmap, 0, 0, null);
        }
        if(featherBitmap != null) {
            // Android scaled images that it loads. The placement of the
            // feather is at 322, 22 on the original image when it was
            // 500 pixels wide. It will have to move based on how big
            // the hat image actually is.
            float factor = hatBitmap.getWidth() / 500.0f;
            canvas.drawBitmap(featherBitmap, 322 * factor, 22 * factor, null);
        }
        if(hatbandBitmap != null) {
            canvas.drawBitmap(hatbandBitmap, 0, 0, null);
        }

        canvas.restore();

    }

    /**
     * Get the current hat type
     * @return one of the hat type values HAT_BLACK, etc.
     */
    public int getHat() {
        return params.hat;
    }

    /**
     * Set the hat type
     * @param hat hat type value, HAT_BLACK, etc.
     */
    public void setHat(int hat) {
        params.hat = hat;
        hatBitmap = null;
        hatbandBitmap = null;

        switch(hat) {
            case HAT_BLACK:
                hatBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.hat_black);
                break;

            case HAT_GRAY:
                hatBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.hat_gray);
                break;

            case HAT_CUSTOM:
                hatBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.hat_white);
                hatbandBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.hat_white_band);
                break;
        }

        invalidate();
    }

    /**
     * Indicate if we draw a feather or not
     * @param f true if we draw the feather
     */
    public void setFeather(boolean f) {
        params.feather = f;
        if(f) {
            featherBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.feather);
        } else {
            featherBitmap = null;
        }

        invalidate();
    }

    /**
     * Get the feather draw status
     * @return true if we draw the feather
     */
    public boolean getFeather() {
        return params.feather;
    }

    /**
     * Handle a touch event
     * @param event The touch event
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int id = event.getPointerId(event.getActionIndex());

        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touch1.id = id;
                touch2.id = -1;
                getPositions(event);
                touch1.copyToLast();
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                if(touch1.id >= 0 && touch2.id < 0) {
                    touch2.id = id;
                    getPositions(event);
                    touch2.copyToLast();
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                touch1.id = -1;
                touch2.id = -1;
                invalidate();
                return true;

            case MotionEvent.ACTION_POINTER_UP:
                if(id == touch2.id) {
                    touch2.id = -1;
                } else if(id == touch1.id) {
                    // Make what was touch2 now be touch1 by
                    // swapping the objects.
                    Touch t = touch1;
                    touch1 = touch2;
                    touch2 = t;
                    touch2.id = -1;
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                getPositions(event);
                move();
                return true;
        }

        return super.onTouchEvent(event);
    }

    /**
     * Get the positions for the two touches and put them
     * into the appropriate touch objects.
     * @param event the motion event
     */
    private void getPositions(MotionEvent event) {
        for(int i=0;  i<event.getPointerCount();  i++) {

            // Get the pointer id
            int id = event.getPointerId(i);

            // Convert to image coordinates
            float x = (event.getX(i) - marginLeft) / imageScale;
            float y = (event.getY(i) - marginTop) / imageScale;

            if(id == touch1.id) {
                touch1.copyToLast();
                touch1.x = x;
                touch1.y = y;
            } else if(id == touch2.id) {
                touch2.copyToLast();
                touch2.x = x;
                touch2.y = y;
            }
        }

        invalidate();
    }

    /**
     * Handle movement of the touches
     */
    private void move() {
        // If no touch1, we have nothing to do
        // This should not happen, but it never hurts
        // to check.
        if(touch1.id < 0) {
            return;
        }

        if(touch1.id >= 0) {
            // At least one touch
            // We are moving
            touch1.computeDeltas();

            params.hatX += touch1.dX;
            params.hatY += touch1.dY;
        }

        if(touch2.id >= 0) {
            // Two touches

            /*
             * Rotation
             */
            float angle1 = angle(touch1.lastX, touch1.lastY, touch2.lastX, touch2.lastY);
            float angle2 = angle(touch1.x, touch1.y, touch2.x, touch2.y);
            float da = angle2 - angle1;
            rotate(da, touch1.x, touch1.y);

            /*
             * Scaling
             */
            float length1 = length(touch1.lastX, touch1.lastY, touch2.lastX, touch2.lastY);
            float length2 = length(touch1.x, touch1.y, touch2.x, touch2.y);
            scale(length2 / length1, touch1.x, touch1.y);
        }
    }

    /**
     * Determine the angle for two touches
     * @param x1 Touch 1 x
     * @param y1 Touch 1 y
     * @param x2 Touch 2 x
     * @param y2 Touch 2 y
     * @return computed angle in degrees
     */
    private float angle(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.toDegrees(Math.atan2(dy, dx));
    }

    /**
     * Determine the distance between two touches
     * @param x1 Touch 1 x
     * @param y1 Touch 1 y
     * @param x2 Touch 2 x
     * @param y2 Touch 2 y
     * @return computed distance
     */
    private float length(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float)Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Rotate the image around the point x1, y1
     * @param dAngle Angle to rotate in degrees
     * @param x1 rotation point x
     * @param y1 rotation point y
     */
    public void rotate(float dAngle, float x1, float y1) {
        params.hatAngle += dAngle;

        // Compute the radians angle
        double rAngle = Math.toRadians(dAngle);
        float ca = (float) Math.cos(rAngle);
        float sa = (float) Math.sin(rAngle);
        float xp = (params.hatX - x1) * ca - (params.hatY - y1) * sa + x1;
        float yp = (params.hatX - x1) * sa + (params.hatY - y1) * ca + y1;

        params.hatX = xp;
        params.hatY = yp;
    }

    /**
     * Scale the image around the point x1, y1
     * @param scale Scale factor
     * @param x1 scale point x
     * @param y1 scale point y
     */
    public void scale(float scale, float x1, float y1) {
        params.hatScale *= scale;

        // Compute a vector to hatX, hatY
        float dx = params.hatX - x1;
        float dy = params.hatY - y1;

        // Compute scaled hatX, hatY
        params.hatX = x1 + dx * scale;
        params.hatY = y1 + dy * scale;
    }

    /**
     * Get the current custom hat color
     * @return hat color integer value
     */
    public int getColor() {
        return params.color;
    }

    /**
     * Set the current custom hat color
     * @param color hat color integer value
     */
    public void setColor(int color) {
        params.color = color;

        // Create a new filter to tint the bitmap
        customPaint.setColorFilter(new LightingColorFilter(color, 0));
        invalidate();
    }

    /**
     * Save the view state to a bundle
     * @param key key name to use in the bundle
     * @param bundle bundle to save to
     */
    public void putToBundle(String key, Bundle bundle) {
        bundle.putSerializable(key, params);
    }

    /**
     * Get the view state from a bundle
     * @param key key name to use in the bundle
     * @param bundle bundle to load from
     */
    public void getFromBundle(String key, Bundle bundle) {
        params = (Parameters)bundle.getSerializable(key);

        // Ensure the options are all set
        setColor(params.color);
        setImageUri(params.imageUri);
        setHat(params.hat);
        setFeather(params.feather);
    }

    public void reset() {
        params.hatX = 0;
        params.hatY = 0;
        params.hatScale = 1;
        params.hatAngle = 0;
        invalidate();
    }

    public void loadXml(XmlPullParser xml) throws IOException, XmlPullParserException {
        // Create a new set of parameters
        final Parameters newParams = new Parameters();

        // Load into it
        newParams.imageUri = xml.getAttributeValue(null, "uri");
        newParams.hatX = Float.parseFloat(xml.getAttributeValue(null, "x"));
        newParams.hatY = Float.parseFloat(xml.getAttributeValue(null, "y"));
        newParams.hatAngle = Float.parseFloat(xml.getAttributeValue(null, "angle"));
        newParams.hatScale = Float.parseFloat(xml.getAttributeValue(null, "scale"));
        newParams.color = Integer.parseInt(xml.getAttributeValue(null, "color"));
        newParams.hat = Integer.parseInt(xml.getAttributeValue(null, "type"));
        newParams.feather = xml.getAttributeValue(null, "feather").equals("yes");

        post(new Runnable() {

            @Override
            public void run() {
                params = newParams;

                // Ensure the options are all set
                setColor(params.color);
                setImageUri(params.imageUri);
                setHat(params.hat);
                setFeather(params.feather);

            }

        });

    }

    public void saveXml(String name, XmlSerializer xml) throws IOException {
        xml.startTag(null, "hatting");

        xml.attribute(null, "name", name);
        xml.attribute(null, "uri", params.imageUri != null ? params.imageUri : "");
        xml.attribute(null, "x", Float.toString(params.hatX));
        xml.attribute(null, "y", Float.toString(params.hatY));
        xml.attribute(null, "angle", Float.toString(params.hatAngle));
        xml.attribute(null, "scale", Float.toString(params.hatScale));
        xml.attribute(null,  "color", Integer.toString(params.color));
        xml.attribute(null, "hat", Integer.toString(params.hat));
        xml.attribute(null, "feather", params.feather ? "yes" : "no");

        xml.endTag(null,  "hatting");
    }
}

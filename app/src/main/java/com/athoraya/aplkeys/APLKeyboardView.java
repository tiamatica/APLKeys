package com.athoraya.aplkeys;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.view.inputmethod.InputMethodSubtype;

import java.util.List;

/**
 * APL Keyboard with APL characters and 2 different modes of input:
 *  Standard UK keyboard with overloaded keys
 *  APL Keys grouped as in Dyalog APL IDE
 * Created by Gil on 21/07/2014.
 */
public class APLKeyboardView extends KeyboardView {
    static final int KEYCODE_OPTIONS = -100;

   private Paint myPaint;

    public APLKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAPLKeyboardView();
    }

    public APLKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAPLKeyboardView();
    }

    private void initAPLKeyboardView(){
        myPaint = new Paint();
        myPaint.setAntiAlias(true);
        myPaint.setTextSize(18);
        myPaint.setTextAlign(Paint.Align.RIGHT);
        myPaint.setAlpha(255);
        myPaint.setTypeface(Typeface.DEFAULT);
    }

    @Override
    /**
     * Called when a key is long pressed. By default this will open any popup keyboard associated
     * with this key through the attributes popupLayout and popupCharacters.
     * @param key the key that was long pressed
     * @return true if the long press is handled, false otherwise. Subclasses should call the
     * method on the base class if the subclass doesn't wish to handle the call.
     */
    protected boolean onLongPress(Key key) {
        if (key.codes[0] == 32) {
            getOnKeyboardActionListener().onKey(KEYCODE_OPTIONS, null);
            return true;
        } else {
            return super.onLongPress(key);
        }
    }

    @Override
    /**
     * @param canvas the canvas on which the background will be drawn
     */
    public void onDraw(Canvas canvas){
        super.onDraw(canvas);
        onDrawSuperScript(canvas);
    }

    private void onDrawSuperScript(Canvas canvas) {

        final Paint paint = myPaint;
        final APLKeyboard keyboard = (APLKeyboard)getKeyboard();
        List<Key> keysList = keyboard.getKeys();
        final Key[] keys = keysList.toArray(new Key[keysList.size()]);
        final int kbdPaddingLeft = getPaddingLeft();
        final int kbdPaddingTop = getPaddingTop();
        final Rect padding = new Rect(6, 9, 6, 6);

        paint.setColor(0xFFCCCCCC);     // light grey
        final int keyCount = keys.length;
        for (int i = 0; i < keyCount; i++) {
            final Key key = keys[i];

            CharSequence otherChars = key.popupCharacters;
            String label = otherChars != null && otherChars.length() > 1 ? String.valueOf(otherChars.charAt(1)) : null;

            if (key.codes[0] == 32){    // draw text bottom right
                label = "\u2026";
                canvas.translate(key.x + kbdPaddingLeft, key.y + kbdPaddingTop);
                canvas.drawText(label,
                        key.width - padding.right,
                        key.height - padding.bottom,
                        paint);
                canvas.translate(-key.x - kbdPaddingLeft, -key.y - kbdPaddingTop);
            } else if (label != null) {    // draw text top right
                // Draw the text
                canvas.translate(key.x + kbdPaddingLeft, key.y + kbdPaddingTop);
                canvas.drawText(label,
                        key.width - padding.right,
                        paint.getTextSize() - paint.descent() + padding.top,
                        paint);
                canvas.translate(-key.x - kbdPaddingLeft, -key.y - kbdPaddingTop);
            }
        }

    }

    void setSubtypeOnSpaceKey(final InputMethodSubtype subtype) {
        final APLKeyboard keyboard = (APLKeyboard)getKeyboard();
//        keyboard.setSpaceIcon(getResources().getDrawable(subtype.getIconResId()));
        invalidateAllKeys();
    }

}

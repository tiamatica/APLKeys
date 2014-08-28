package com.athoraya.aplkeys;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.method.MetaKeyKeyListener;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import com.athoraya.utils.TypefaceUtil;

import java.util.concurrent.TimeUnit;

/**
 * Main APL keyboard IME class
 * Created by Gil on 21/07/2014.
 */
public class APLKeysIME extends InputMethodService 
        implements KeyboardView.OnKeyboardActionListener {

    private InputMethodManager mInputMethodManager;

    private APLKeyboardView mInputView;
    private int mLastDisplayWidth;
    private boolean mCapsLock;
    private long mLastShiftTime;

    private APLKeyboard mQwertyKeyboard;
    private APLKeyboard mSymbolsKeyboard;
    private APLKeyboard mSymbolsPhoneKeyboard;
    private APLKeyboard mAPLKeyboardPage1;
    private APLKeyboard mAPLKeyboardPage2;

    private APLKeyboard mCurKeyboard;

    private Boolean mFullQwerty;
    private Boolean mPrefsChanged;

    private static final String KEY_PREF_FULL_QWERTY = "pref_full_qwerty";

    private SharedPreferences mPrefs;
    SharedPreferences.OnSharedPreferenceChangeListener mPrefsListener =
            new SharedPreferences.OnSharedPreferenceChangeListener(){
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    mPrefsChanged = true;
                }
            };

    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override public void onCreate() {
        super.onCreate();
        mInputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);

        PreferenceManager.setDefaultValues(this, R.xml.ime_preferences, false);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPrefs.registerOnSharedPreferenceChangeListener(mPrefsListener);
        loadPrefs();

        TypefaceUtil.overrideFont(getApplicationContext(), "DEFAULT", "fonts/apl385.otf");
        TypefaceUtil.overrideFont(getApplicationContext(), "DEFAULT_BOLD", "fonts/apl385.otf");
    }

    @Override public void onDestroy(){
        super.onDestroy();
        mPrefs.unregisterOnSharedPreferenceChangeListener(mPrefsListener);
    }

    private void loadPrefs() {
        mFullQwerty = mPrefs.getBoolean(KEY_PREF_FULL_QWERTY, true);
        mPrefsChanged = false;
    }

    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override public void onInitializeInterface() {
        Boolean hasChanged =  mPrefsChanged;
        loadPrefs();
        int displayWidth = getMaxWidth();
        hasChanged = hasChanged || displayWidth != mLastDisplayWidth;
        mLastDisplayWidth = displayWidth;
        if (!hasChanged) return;
        /*
        if (mQwertyKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        } else {
            mLastDisplayWidth = displayWidth;
        }
        */
        if (mLastDisplayWidth < 600 && !mFullQwerty) {
            mQwertyKeyboard = new APLKeyboard(this, R.xml.qwerty_reduced);
        } else {
            mQwertyKeyboard = new APLKeyboard(this, R.xml.qwerty);
        }
        mSymbolsKeyboard = new APLKeyboard(this, R.xml.symbols);
        mSymbolsPhoneKeyboard = new APLKeyboard(this, R.xml.symbols_phone);
        mAPLKeyboardPage1 = new APLKeyboard(this, R.xml.apl_chars_page1);
        mAPLKeyboardPage2 = new APLKeyboard(this, R.xml.apl_chars_page2);
    }

    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override public View onCreateInputView() {
        mInputView = (APLKeyboardView) getLayoutInflater().inflate(
                R.layout.input, null);
        mInputView.setOnKeyboardActionListener(this);
        mInputView.setKeyboard(mQwertyKeyboard);
        return mInputView;
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);

        if (mPrefsChanged){
            onInitializeInterface();
        }

        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
                // Numbers and dates default to the symbols keyboard, with
                // no extra features.
                mCurKeyboard = mSymbolsKeyboard;
                break;

            case InputType.TYPE_CLASS_PHONE:
                mCurKeyboard = mSymbolsPhoneKeyboard;
                break;

            case InputType.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                mCurKeyboard = mQwertyKeyboard;
                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        || variation == InputType.TYPE_TEXT_VARIATION_URI
                        || variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    // mPredictionOn = false;
                }

                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
                updateShiftKeyState(attribute);
                break;

            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                mCurKeyboard = mQwertyKeyboard;
                updateShiftKeyState(attribute);
        }

        // Update the label on the enter key, depending on what the application
        // says it will do.
        mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions);
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override public void onFinishInput() {
        super.onFinishInput();

        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false);

        mCurKeyboard = mQwertyKeyboard;
        if (mInputView != null) {
            mInputView.closing();
        }
    }

    @Override public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.
        mInputView.setKeyboard(mCurKeyboard);
        mInputView.closing();
        final InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
        mInputView.setSubtypeOnSpaceKey(subtype);
    }

    @Override
    /**
     * Called when the subtype was changed.
     * @param newSubtype the subtype which is being changed to.
     */
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype newSubtype) {
        mInputView.setSubtypeOnSpaceKey(newSubtype);
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our keyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.
                if (event.getRepeatCount() == 0 && mInputView != null) {
                    if (mInputView.handleBack()) {
                        return true;
                    }
                }
                break;

            case KeyEvent.KEYCODE_DEL:
                // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                break;

            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these.
                return false;

            default:
                // Do nothing.
                break;

        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.

        return super.onKeyUp(keyCode, event);
    }


    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    private void updateShiftKeyState(EditorInfo attr) {
        if (attr != null
                && mInputView != null && mQwertyKeyboard == mInputView.getKeyboard()) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != InputType.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            mInputView.setShifted(mCapsLock || caps != 0);
        }
    }

    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));

    }

    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }


    public void onKey(int primaryCode, int[] keyCodes) {
        if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            handleShift();
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
            handleClose();
        } else if (primaryCode == APLKeyboardView.KEYCODE_OPTIONS) {
            Intent intent = new Intent(this, APLKeysIMESettings.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else if (primaryCode == Keyboard.KEYCODE_ALT) {
            handleAlt();
        } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE) {
            handleMode();
        } else if (primaryCode == '\n') {
            sendKeyChar((char) primaryCode);
        } else {
            handleCharacter(primaryCode);
        }
    }

    private void handleMode() {
        if (mInputView == null) {
            return;
        }

        Keyboard current = mInputView.getKeyboard();
        if (current == mQwertyKeyboard) {
            mCurKeyboard = mSymbolsKeyboard;
        } else if (current == mAPLKeyboardPage1) {
            mCurKeyboard = mSymbolsKeyboard;
        } else if (current == mAPLKeyboardPage2) {
            mCurKeyboard = mSymbolsKeyboard;
        } else if (current == mSymbolsKeyboard) {
            mCurKeyboard = mQwertyKeyboard;
        } else if (current == mSymbolsPhoneKeyboard) {
            mCurKeyboard = mQwertyKeyboard;
        }
        mInputView.setKeyboard(mCurKeyboard);
    }

    private void handleAlt() {
        if (mInputView == null) {
            return;
        }

        Keyboard current = mInputView.getKeyboard();
        if (current == mQwertyKeyboard) {
            mCurKeyboard = mAPLKeyboardPage1;
        } else if (current == mAPLKeyboardPage1) {
            mCurKeyboard = mQwertyKeyboard;
        } else if (current == mAPLKeyboardPage2) {
            mCurKeyboard = mQwertyKeyboard;
        } else if (current == mSymbolsKeyboard) {
            mCurKeyboard = mAPLKeyboardPage1;
        } else if (current == mSymbolsPhoneKeyboard) {
            mCurKeyboard = mAPLKeyboardPage1;
        }
        mInputView.setKeyboard(mCurKeyboard);
    }

    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        ic.commitText(text, 0);
        ic.endBatchEdit();
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    private void handleBackspace() {
        getCurrentInputConnection().deleteSurroundingText(1,0);
        //keyDownUp(KeyEvent.KEYCODE_DEL);
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    private void handleShift() {
        if (mInputView == null) {
            return;
        }

        Keyboard current = mInputView.getKeyboard();

        if (current == mQwertyKeyboard) {
            // Alphabet keyboard
            checkToggleCapsLock();
            mInputView.setShifted(mCapsLock || !mInputView.isShifted());
            return;
        } else if (current == mAPLKeyboardPage1) {
            mCurKeyboard = mAPLKeyboardPage2;
        } else if (current == mAPLKeyboardPage2) {
            mCurKeyboard = mAPLKeyboardPage1;
        } else if (current == mSymbolsKeyboard) {
            mCurKeyboard = mSymbolsPhoneKeyboard;
        } else if (current == mSymbolsPhoneKeyboard) {
            mCurKeyboard = mSymbolsKeyboard;
        }
        mInputView.setKeyboard(mCurKeyboard);

    }

    private void handleCharacter(int primaryCode) {
        if (isInputViewShown() && mInputView.isShifted()) {
            primaryCode = Character.toUpperCase(primaryCode);
        }
        getCurrentInputConnection().commitText(String.valueOf((char) primaryCode), 1);
        if (Character.isLetter(primaryCode)) {
            updateShiftKeyState(getCurrentInputEditorInfo());
        }
    }

    private void handleClose() {
        requestHideSelf(0);
        mInputView.closing();
    }

    private void checkToggleCapsLock() {
        long now = System.currentTimeMillis();
        if (mCapsLock || mLastShiftTime + 800 > now) {
            mCapsLock = !mCapsLock;
            mLastShiftTime = 0;
            mQwertyKeyboard.setShiftIcon(getResources(),mCapsLock);
            mInputView.invalidateAllKeys();
        } else {
            mLastShiftTime = now;
        }
    }


    public void swipeRight() {
    }

    public void swipeLeft() {
    }

    public void swipeDown() {
    }

    public void swipeUp() {
    }

    public void onPress(int primaryCode) {
    }

    public void onRelease(int primaryCode) {
    }

}

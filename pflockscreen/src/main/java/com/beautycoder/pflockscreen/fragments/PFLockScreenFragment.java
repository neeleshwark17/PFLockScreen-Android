package com.beautycoder.pflockscreen.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.beautycoder.pflockscreen.R;
import com.beautycoder.pflockscreen.security.FingerprintPinCodeHelper;
import com.beautycoder.pflockscreen.security.PFSecurityException;
import com.beautycoder.pflockscreen.views.PFCodeView;

/**
 * Created by aleksandr on 2018/02/07.
 */

public class PFLockScreenFragment extends Fragment {

    private static final String FINGERPRINT_DIALOG_FRAGMENT_TAG = "FingerprintDialogFragment";

    private View mFingerprintButton;
    private View mDeleteButton;
    private View mRightButton;
    private View mLeftButton;
    private View mNextButton;
    private PFCodeView mCodeView;

    private boolean mUseFingerPrint = true;
    private boolean mFingerprintHarwareDetected = false;

    private boolean mCreatePasswordMode = true;

    private OnPFLockScreenCodeCreateListener mCodeCreateListener;
    private OnPFLockScreenLoginListener mLoginListener;
    private String mCode = "";
    private String mEncodedPinCode = "";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lock_screen_pf, container, false);
        mFingerprintButton = view.findViewById(R.id.button_finger_print);
        mDeleteButton = view.findViewById(R.id.button_delete);

        mLeftButton = view.findViewById(R.id.button_left);
        mRightButton = view.findViewById(R.id.button_right);
        mNextButton = view.findViewById(R.id.button_next);


        mDeleteButton.setOnClickListener(mOnDeleteButtonClickListener);
        mFingerprintButton.setOnClickListener(mOnFingerprintClickListener);

        mCodeView = view.findViewById(R.id.code_view);
        initKeyViews(view);

        if (mCreatePasswordMode) {
            mLeftButton.setVisibility(View.GONE);
            mRightButton.setVisibility(View.GONE);
            mFingerprintButton.setVisibility(View.GONE);
        }

        mCodeView.setListener(mCodeListener);

        if (mCreatePasswordMode) {
            mNextButton.setOnClickListener(mOnNextButtonClickListener);
        } else {
            mNextButton.setOnClickListener(null);
        }

        if (!mUseFingerPrint) {
            mFingerprintButton.setVisibility(View.GONE);
        }

        mFingerprintHarwareDetected = isFingerprintApiAvailable(getContext());


        return view;
    }

    private void initKeyViews(View parent) {
        parent.findViewById(R.id.button_0).setOnClickListener(mOnKeyClickListener);
        parent.findViewById(R.id.button_1).setOnClickListener(mOnKeyClickListener);
        parent.findViewById(R.id.button_2).setOnClickListener(mOnKeyClickListener);
        parent.findViewById(R.id.button_3).setOnClickListener(mOnKeyClickListener);
        parent.findViewById(R.id.button_4).setOnClickListener(mOnKeyClickListener);
        parent.findViewById(R.id.button_5).setOnClickListener(mOnKeyClickListener);
        parent.findViewById(R.id.button_6).setOnClickListener(mOnKeyClickListener);
        parent.findViewById(R.id.button_7).setOnClickListener(mOnKeyClickListener);
        parent.findViewById(R.id.button_8).setOnClickListener(mOnKeyClickListener);
        parent.findViewById(R.id.button_9).setOnClickListener(mOnKeyClickListener);
    }



    private View.OnClickListener mOnKeyClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v instanceof TextView) {
                String string = ((TextView) v).getText().toString();
                if (string.length() != 1) {
                    return;
                }
                int codeLength = mCodeView.input(string);
                configureRightButton(codeLength);
            }
        }
    };

    private View.OnClickListener mOnDeleteButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int codeLength = mCodeView.delete();
            configureRightButton(codeLength);
        }
    };

    private View.OnClickListener mOnFingerprintClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final PFFingerprintAuthDialogFragment fragment
                    = new PFFingerprintAuthDialogFragment();
            fragment.show(getFragmentManager(), FINGERPRINT_DIALOG_FRAGMENT_TAG);
            fragment.setAuthListener(new PFFingerprintAuthListener() {
                @Override
                public void onAuthenticated() {
                    if (mLoginListener != null) {
                        mLoginListener.onFingerprintSuccessful();
                    }
                    fragment.dismiss();
                }

                @Override
                public void onError() {
                    if (mLoginListener != null) {
                        mLoginListener.onFingerprintLoginFailed();
                    }
                }
            });
        }
    };

    private void configureRightButton(int codeLength) {
        if (mCreatePasswordMode) {
            if (codeLength > 0) {
                mDeleteButton.setVisibility(View.VISIBLE);
            } else {
                mDeleteButton.setVisibility(View.GONE);
            }
            return;
        }

        if (codeLength > 0) {
            mFingerprintButton.setVisibility(View.GONE);
            mDeleteButton.setVisibility(View.VISIBLE);
            mDeleteButton.setEnabled(true);
            return;
        }

        if (mUseFingerPrint && mFingerprintHarwareDetected) {
            mFingerprintButton.setVisibility(View.VISIBLE);
            mDeleteButton.setVisibility(View.GONE);
        } else {
            mFingerprintButton.setVisibility(View.GONE);
            mDeleteButton.setVisibility(View.VISIBLE);
        }
        mDeleteButton.setEnabled(false);

    }

    private boolean isFingerprintApiAvailable(Context context) {
        return FingerprintManagerCompat.from(context).isHardwareDetected();
    }

    private boolean isFingerprintsExists(Context context) {
        return FingerprintManagerCompat.from(context).hasEnrolledFingerprints();
    }

    public void setCreatePasswordMode(boolean createPasswordMode) {
        mCreatePasswordMode = createPasswordMode;
    }


    private PFCodeView.OnPFCodeListener mCodeListener = new PFCodeView.OnPFCodeListener() {
        @Override
        public void onCodeCompleted(String code) {
            if (mCreatePasswordMode) {
                mNextButton.setVisibility(View.VISIBLE);
                mCode = code;
                return;
            }
            mCode = code;
            try {
                boolean isCorrect
                        = FingerprintPinCodeHelper.getInstance().checkPin(getContext(), mEncodedPinCode, mCode);
                if (mLoginListener != null) {
                    if (isCorrect) {
                        mLoginListener.onCodeInputSuccessful();
                    } else {
                        mLoginListener.onPinLoginFailed();
                        errorAction();
                    }
                }
            } catch (PFSecurityException e) {
                e.printStackTrace();

            }


        }

        @Override
        public void onCodeNotCompleted(String code) {
            if (mCreatePasswordMode) {
                mNextButton.setVisibility(View.GONE);
                return;
            }
        }
    };


    private View.OnClickListener mOnNextButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                String encodedCode = FingerprintPinCodeHelper.getInstance().savePin(getContext(),
                        mCode, true);
                if (mCodeCreateListener != null) {
                    mCodeCreateListener.onCodeCreated(encodedCode);
                }
                //showFingerprintAlertDialog(getActivity());
            } catch (PFSecurityException e) {
                e.printStackTrace();
                //TODO: Show error;
            }
        }
    };



    /*private void showFingerprintAlertDialog(Context context) {
        new AlertDialog.Builder(context).setTitle("Fingerprint").setMessage(
                "Would you like to use fingerprint for future login?")
                .setPositiveButton("Use fingerprint", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //if (isFingerprintsExists(getContext())) {
                    //FingerprintPinCodeHelper.getInstance().savePin()
                //}
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        }).create().show();
    }*/

    public void setCodeCreateListener(OnPFLockScreenCodeCreateListener listener) {
        mCodeCreateListener = listener;
    }

    public void setLoginListener(OnPFLockScreenLoginListener listener) {
        mLoginListener = listener;
    }

    public void setEncodedPinCode(String encodedPinCode) {
        mEncodedPinCode = encodedPinCode;
    }

    private void errorAction() {
        Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(400);
        final Animation animShake = AnimationUtils.loadAnimation(getContext(), R.anim.shake);
        mCodeView.startAnimation(animShake);
    }

    public interface OnPFLockScreenCodeCreateListener {

        void onCodeCreated(String encodedCode);

    }


    public interface  OnPFLockScreenLoginListener {

        void onCodeInputSuccessful();

        void onFingerprintSuccessful();

        void onPinLoginFailed();

        void onFingerprintLoginFailed();

    }


}










//private static final String KEY_STORE_NAME = "fp_lock_screen_key_store";
//private Cipher cipher;
//private KeyStore keyStore;
//private KeyguardManager keyguardManager;
//private static KeyGenerator keyGenerator;
//private Cipher defaultCipher;


 /* try {
            generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        keyguardManager = (KeyguardManager) getContext().getSystemService(KEYGUARD_SERVICE);*/

        /*if (!isFingerprintApiAvailable(getContext())) {
            mOnFingerprintButton.setImageDrawable(getResources()
                    .getDrawable(R.drawable.delete_lockscreen_pf));
        }
        try {
            defaultCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get an instance of Cipher", e);
        }
        prepareSensor();*/

    //private boolean isDeviceLockScreenIsProtected(Context context) {
       // return keyguardManager.isKeyguardSecure();
    //}


    /*private void prepareSensor() {
            cipherInit();
            FingerprintManagerCompat.CryptoObject cryptoObject = new FingerprintManagerCompat.CryptoObject(cipher);
            if (cryptoObject != null) {
                Toast.makeText(getContext(), "use fingerprint to login", Toast.LENGTH_LONG).show();
                FingerprintHelper mFingerprintHelper = new FingerprintHelper(this.getContext());
                mFingerprintHelper.startAuth(cryptoObject);
            }
    }


    public class FingerprintHelper extends FingerprintManagerCompat.AuthenticationCallback {
        private Context mContext;
        private CancellationSignal mCancellationSignal;

        FingerprintHelper(Context context) {
            mContext = context;
        }

        void startAuth(FingerprintManagerCompat.CryptoObject cryptoObject) {
            mCancellationSignal = new CancellationSignal();
            FingerprintManagerCompat manager = FingerprintManagerCompat.from(mContext);
            manager.authenticate(cryptoObject, 0, mCancellationSignal, this, null);
        }

        void cancel() {
            if (mCancellationSignal != null) {
                mCancellationSignal.cancel();
            }
        }

        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            Toast.makeText(mContext, errString, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            Toast.makeText(mContext, helpString, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
            Cipher cipher = result.getCryptoObject().getCipher();
            Toast.makeText(mContext, "success", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onAuthenticationFailed() {
            Toast.makeText(mContext, "try again", Toast.LENGTH_SHORT).show();
        }

    }*/

    /*

    public boolean cipherInit() {
        try {
            cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException |
                NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher", e);
        }

        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey("key_name",
                    null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException
                | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }*/


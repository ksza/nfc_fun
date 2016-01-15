package ro.ksza.nfcchat;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private String linkToShare = null;

    private TextView feedbackText;
    private CoordinatorLayout coord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        coord = (CoordinatorLayout) findViewById(R.id.coord);
        feedbackText = (TextView) findViewById(R.id.feedback);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        parseShareData(getIntent());
    }

    private void parseShareData(final Intent intent) {
        if (intent != null && intent.getClipData() != null) {

            try {
                linkToShare = intent.getClipData().getItemAt(0).getText().toString();
                updateFeedback();
            } catch (Throwable t) {
            }
        }
    }

    private void updateFeedback() {
        if (feedbackText != null) {
            if (!nfcAdapter.isEnabled()) {
                feedbackText.setText(R.string.device_nfc_disabled);
                feedbackText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else if (TextUtils.isEmpty(linkToShare)) {
                feedbackText.setText(R.string.nothing_to_share);
                feedbackText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else {
                feedbackText.setText(R.string.approach_to_share);
                feedbackText.setTextColor(getResources().getColor(android.R.color.black));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        enableForegroundDispatchSystem();

        updateFeedback();
    }

    @Override
    protected void onPause() {
        super.onPause();

        disableForegroundDispatchSystem();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            if(!TextUtils.isEmpty(linkToShare)) {

                NdefMessage ndefMessage = createNdefMessage(linkToShare);

                writeNdefMessage(tag, ndefMessage);
            }
        } else {
            parseShareData(intent);
        }
    }

    private void enableForegroundDispatchSystem() {

        Intent intent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        IntentFilter[] intentFilters = new IntentFilter[]{};

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null);
    }

    private void disableForegroundDispatchSystem() {
        nfcAdapter.disableForegroundDispatch(this);
    }

    private void formatTag(Tag tag, NdefMessage ndefMessage) {
        try {

            NdefFormatable ndefFormatable = NdefFormatable.get(tag);
            if (ndefFormatable == null) {
                Toast.makeText(this, "Tag is not ndef formatable!", Toast.LENGTH_SHORT).show();
                return;
            }

            ndefFormatable.connect();
            ndefFormatable.format(ndefMessage);
            ndefFormatable.close();

            Toast.makeText(this, "Tag writen!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("formatTag", e.getMessage());
        }

    }

    private void writeNdefMessage(Tag tag, NdefMessage ndefMessage) {

        try {



            if (tag == null) {
                Snackbar.make(coord, "Tag object cannot be null", Snackbar.LENGTH_SHORT).show();
                return;
            }

            Ndef ndef = Ndef.get(tag);

            if (ndef == null) {
                // format tag with the ndef format and writes the message.
                formatTag(tag, ndefMessage);
            } else {
                ndef.connect();

                if (!ndef.isWritable()) {
                    Snackbar.make(coord, "Tag is not writable", Snackbar.LENGTH_SHORT).show();

                    ndef.close();
                    return;
                }

                ndef.writeNdefMessage(ndefMessage);
                ndef.close();

                final Snackbar snackbar = Snackbar.make(coord, "Successfully shared", Snackbar.LENGTH_SHORT);
                snackbar.setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);

                        MainActivity.this.finish();
                    }
                });
                snackbar.show();
            }

        } catch (Exception e) {
            Log.e("writeNdefMessage", e.getMessage());
        }

    }

    private NdefRecord createYoutubeRecord(String content) {
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, new byte[0], NdefRecord.createUri(content).getPayload());
    }

    private NdefRecord createUserNameRecord(String userName) {
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], userName.getBytes());
    }

    private NdefMessage createNdefMessage(String content) {

        NdefRecord ndefRecord = createYoutubeRecord(content);

        NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{ndefRecord});

        return ndefMessage;
    }
}

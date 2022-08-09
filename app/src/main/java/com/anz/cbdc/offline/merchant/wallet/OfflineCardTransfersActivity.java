package com.anz.cbdc.offline.merchant.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class OfflineCardTransfersActivity extends Activity {

    int channel;
    private NfcForegroundUtil nfcForegroundUtil;
    private IsoDep nfcTag;
    private ProgressDialog dialog;

    private int operatingMode;
    public static final int MODE_BALANCE = 1;
    public static final int MODE_GETID = 2;
    public static final int MODE_TOPUP = 3;
    public static final int MODE_CHARGE = 4;
    private static final String SGD = "SGD ";
    private static final String SHARED_PREFERENCES_FILE_NAME = "merchantWalletSP";
    private static final String MERCHANT_WALLET_BALANCE_SP = "merchantWalletBalance";
    private static final String PAYMENT_CODE_SP = "paymentCode";

    private String textResult;
    private boolean showAlertDialog = true;
    private boolean tapAllowed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nfcForegroundUtil = new NfcForegroundUtil(this);

        setContentView(R.layout.activity_offline_card_transfers);

    }

    public void onPause() {
        super.onPause();
        nfcForegroundUtil.disableForeground();
    }

    public void onResume() {
        super.onResume();
        nfcForegroundUtil.enableForeground();
    }

    public void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        new Thread(new Runnable() {
            public void run() {
                if (tapAllowed) {
                    try {
                        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                        showAlertDialog = true;
                        dialog.setMessage("Reading card");

                        nfcTag = IsoDep.get(tag);
                        nfcTag.connect();
                        nfcTag.setTimeout(60000);

                        // select the WhisperCash application on card
                        sendToTAG("00A404000F57484953504552434153482E415050");

                        // authenticate with PIN 123456 (hex 313233343536)
                        sendToTAG("0002000006313233343536");

                        // history
                        hexToString(sendToTAG("00D0000000"));

                        //sharedResource("");

                        OfflineCardTransfersActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                switch(operatingMode) {
                                    case MODE_BALANCE:
                                        readBalance();
                                        break;
                                    case MODE_GETID:
                                        readId();
                                        break;
                                    case MODE_TOPUP:
                                        Map<String, String> data = getPaymentCode();
                                        String amount = data == null || data.isEmpty() ? "" : data.get("unit");
                                        String code = data == null || data.isEmpty() ? "" : data.get("payCode");
                                        receivePayment(readId(), amount, code);
                                        break;
                                    case MODE_CHARGE:
                                        // send a payment to lite id: 1111111111100029 | 100 units ($1.00)
                                        sendPayment(readId(), getUnits(getChargeAmt()));
                                        break;
                                }

                                dialog.dismiss();

                                tapAllowed = false;

                                if (showAlertDialog) {
                                    showAlertDialog();
                                }

                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void readBalance() {
        try {
            String balance = sendToTAG("0004000000");
            if (balance.endsWith("9000")) {
                String hexBalance = balance.substring(0, balance.length()-4);
                String cardBalance = new String(HexUtils.toBytes(hexBalance));
                Log.i("CARD BALANCE", "TAG<< " + cardBalance);
                showAlertDialog = false;
                // ((TextView)OfflineCardTransfersActivity.this.findViewById(R.id.balanceText)).setText(SGD + displayBalance(cardBalance));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String displayBalance(String balance) {
        try {
            // 00000000000000000000000000099800 | left = 0998 | right = 00
            String left = balance.substring(26, 30);
            String right = balance.substring(balance.length() - 2);
            if (Integer.parseInt(left) < 1000) {
                left = balance.substring(27, 30);
            }
            return left + "." + right;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private String readId() {
        try {
            String id = sendToTAG("0003000000");
            if (id.endsWith("9000")) {
                String hexId = id.substring(0, id.length()-4);
                String cardId = new String(HexUtils.toBytes(hexId));
                Log.i("CARD ID", "TAG<< " + cardId);
                return cardId;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void sendPayment(String recipient, String amount) {
        if (recipient.isEmpty()) {
            textResult = "Unable to read card id, please try again";
            Log.e("SEND PAYMENT", "TAG<< " + textResult);
        } else if (!amount.isEmpty() && Integer.parseInt(amount) > 0) {
            try {
                String paymentResult = sendToTAG("0000000030"+HexUtils.toHex(recipient.getBytes())+HexUtils.toHex(amount.getBytes()));
                if (paymentResult.endsWith("9000")) {
                    String hexPaymentResult = paymentResult.substring(0, paymentResult.length()-4);
                    String textPaymentResult = new String(HexUtils.toBytes(hexPaymentResult));
                    String payer = textPaymentResult.substring(0, 16);
                    String paymentAmount = textPaymentResult.substring(16, 48);
                    String paymentCode = textPaymentResult.substring(48, 58);
                    textResult = "Payment from "+payer+" to "+recipient+" of "+paymentAmount+" units, with code "+paymentCode;
                    Log.i("SEND PAYMENT", "TAG<< " + textResult);
                    // updateOfflineWalletBalance(getChargeAmt());
                    updatePaymentCode(recipient, paymentCode, amount);
                } else {
                    textResult = "Could not send payment";
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            textResult = "Please enter valid 'Receive from card' amount";
            Log.e("SEND PAYMENT", "TAG<< " + textResult);
        }
        setChargeAmt("");
        readBalance();
        showAlertDialog = true;
    }

    private void receivePayment(String sender, String amount, String code) {
        if (sender.isEmpty()) {
            textResult = "Unable to read card id, please try again";
            Log.e("RECV PAYMENT", "TAG<< " + textResult);
        } else if (!amount.isEmpty() && Integer.parseInt(amount) > 0) {
            try {
                String paymentResult = sendToTAG("000100003A"+HexUtils.toHex(sender.getBytes())+HexUtils.toHex(amount.getBytes())+HexUtils.toHex(code.getBytes()));
                if (paymentResult.endsWith("9000")) {
                    textResult = "Payment accepted";
                    // updateOfflineWalletBalance(getTopUpAmt());
                    removePaymentCode(code);
                } else {
                    textResult = "Incorrect code, payment failed";
                }
                Log.i("RECV PAYMENT", "TAG<< " + textResult);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            textResult = "Please enter valid 'Send to card' amount";
            Log.e("RECEIVE PAYMENT", "TAG<< " + textResult);
        }
        setTopUpAmt("");
        readBalance();
        showAlertDialog = true;
    }

    private void showAlertDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle("Operation result")
                .setMessage(textResult)
                .setPositiveButton("OK", null)
                .show();
    }

    private String sendToTAG(String command) throws IOException {
        Log.i("WHISPERCASH", "TAG>> " + command);
        byte[] response = nfcTag.transceive(HexUtils.toBytes(command));
        Log.i("WHISPERCASH", "TAG<< " + HexUtils.toHex(response));
        return HexUtils.toHex(response);
    }

    private void doAskToTap() {
        tapAllowed = true;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog = ProgressDialog.show(OfflineCardTransfersActivity.this, "Waiting", "Please tap a card...", true, true);
            }
        });
    }

    public void doGetBalance(View v) {
        operatingMode = MODE_BALANCE;
        doAskToTap();
    }

    public void doGetCardId(View v) {
        operatingMode = MODE_GETID;
        doAskToTap();
    }

    public void doTopUp(View v) {
        operatingMode = MODE_TOPUP;
        doAskToTap();
    }

    public void doCharge(View v) {
        operatingMode = MODE_CHARGE;
        doAskToTap();
    }

    private void updateOfflineWalletBalance(String amount) {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, MODE_PRIVATE);
            String offlineWalletBalance = sharedPreferences.getString(MERCHANT_WALLET_BALANCE_SP, "0.00");
            BigDecimal bdOld = new BigDecimal(offlineWalletBalance).setScale(2, BigDecimal.ROUND_HALF_UP);
            BigDecimal bdNew = new BigDecimal(amount).setScale(2, BigDecimal.ROUND_HALF_UP);
            if (operatingMode == MODE_TOPUP) {
                BigDecimal newBalance = bdOld.subtract(bdNew);
                sharedPreferencesEditor(MERCHANT_WALLET_BALANCE_SP, newBalance.toString());
            } else if (operatingMode == MODE_CHARGE) {
                BigDecimal newBalance = bdOld.add(bdNew);
                sharedPreferencesEditor(MERCHANT_WALLET_BALANCE_SP, newBalance.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sharedPreferencesEditor(String key, String value) {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    private Map<String, String> getPaymentCode() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, MODE_PRIVATE);
        Gson gson = new Gson();
        Data allPayments = gson.fromJson(sharedPreferences.getString(PAYMENT_CODE_SP, ""), Data.class);
        if(allPayments != null ) {
            if (allPayments != null || allPayments.getMap() != null || !allPayments.getMap().isEmpty()) {
                for (Map.Entry<String, Map<String, String>> entrySet : allPayments.getMap().entrySet()) {
                    return entrySet.getValue();
                }
            }
        }

        return new LinkedHashMap<>();
    }

    private void removePaymentCode(String code) {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        Data allPayments = gson.fromJson(sharedPreferences.getString(PAYMENT_CODE_SP, ""), Data.class);
        if (allPayments != null) {
            if (allPayments != null || allPayments.getMap() != null || !allPayments.getMap().isEmpty()) {
                allPayments.getMap().remove(code);
                editor.putString(PAYMENT_CODE_SP, gson.toJson(allPayments));
                editor.commit();
                Data updatedPayments = gson.fromJson(sharedPreferences.getString(PAYMENT_CODE_SP, ""), Data.class);
                Log.i("updatePaymentCode", "PAYMENT_CODE_SP>> " + (updatedPayments != null ? updatedPayments.getMap().keySet() : ""));
            }
        }

    }

    private void updatePaymentCode(String cardId, String payCode, String unit) {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (false) {
            editor.remove(PAYMENT_CODE_SP);
            editor.commit();
        }

        Gson gson = new Gson();
        Data allPayments = gson.fromJson(sharedPreferences.getString(PAYMENT_CODE_SP, ""), Data.class);

        Map<String, String> payment = new LinkedHashMap<>();
        payment.put("cardId", cardId);
        payment.put("payCode", payCode);
        payment.put("unit", unit);
        payment.put("amount", getChargeAmt());

        if (allPayments == null || allPayments.getMap() == null || allPayments.getMap().isEmpty()) {
            Map<String, Map<String, String>> map = new LinkedHashMap<>();
            map.put(payCode, payment);
            Data data = new Data(map);
            editor.putString(PAYMENT_CODE_SP, gson.toJson(data));
        } else {
            allPayments.getMap().put(payCode, payment);
            editor.putString(PAYMENT_CODE_SP, gson.toJson(allPayments));
        }

        editor.commit();

        Data updatedPayments = gson.fromJson(sharedPreferences.getString(PAYMENT_CODE_SP, ""), Data.class);
        Log.i("updatePaymentCode", "PAYMENT_CODE_SP>> " + updatedPayments.getMap().keySet());

    }

    public String convertWithStream(Map<String, ?> map) {
        String mapAsString = map.keySet().stream()
                .map(key -> key + "=" + map.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
        return mapAsString;
    }

    public Map<String, String> convertWithStream(String mapAsString) {
        Map<String, String> map = Arrays.stream(mapAsString.split(","))
                .map(entry -> entry.split("="))
                .collect(Collectors.toMap(entry -> entry[0], entry -> entry[1]));
        return map;
    }

    private String getUnits(String amount) {
        // 00000000000000000000000000000100
        String units = amount.isEmpty() ? "" : prepareUnits(Integer.parseInt(amount));
        if (units.length() == 3) {
            return "00000000000000000000000000000" + units;
        } else if (units.length() == 4) {
            return "0000000000000000000000000000" + units;
        } else if (units.length() == 5) {
            return "000000000000000000000000000" + units;
        }
        return "";
    }

    private String prepareUnits(int amount) {
        return String.valueOf(amount * 100);
    }

    private String getTopUpAmt() {
        return ""; // ((TextView)OfflineCardTransfersActivity.this.findViewById(R.id.etTopUpAmount)).getText().toString();
    }

    private void setTopUpAmt(String amount) {
        // ((TextView)OfflineCardTransfersActivity.this.findViewById(R.id.etTopUpAmount)).setText(amount);
    }

    private String getChargeAmt() {
        return ((TextView)OfflineCardTransfersActivity.this.findViewById(R.id.etCharge)).getText().toString();
    }

    private void setChargeAmt(String amount) {
        ((TextView)OfflineCardTransfersActivity.this.findViewById(R.id.etCharge)).setText(amount);
    }

    public static String hexToString(String hex) {
        StringBuilder sb = new StringBuilder();
        char[] hexData = hex.toCharArray();
        for (int count = 0; count < hexData.length - 1; count += 2) {
            int firstDigit = Character.digit(hexData[count], 16);
            int lastDigit = Character.digit(hexData[count + 1], 16);
            int decimal = firstDigit * 16 + lastDigit;
            sb.append((char) decimal);
        }
        return sb.toString();
    }

}
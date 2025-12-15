package com.example.phonebook;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.widget.Toast;
import java.util.Locale;

public class CallerIDService extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            
            if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                
                if (phoneNumber != null) {
                    DatabaseHelper dbHelper = new DatabaseHelper(context);
                    android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();
                    android.database.Cursor cursor = db.rawQuery(
                        "SELECT name FROM contacts WHERE number LIKE ?", 
                        new String[]{"%" + phoneNumber + "%"});
                    
                    if (cursor.moveToFirst()) {
                        String name = cursor.getString(0);
                        // عرض اسم المتصل
                        showCallerName(context, name, phoneNumber);
                    }
                    cursor.close();
                    dbHelper.close();
                }
            }
        }
    }
    
    private void showCallerName(Context context, String name, String number) {
        // يمكن استخدام Notification أو Toast أو Dialog
        String message = String.format(Locale.getDefault(), 
            "متصل: %s\nرقم: %s", name, number);
        
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
// ملف MainActivity.java تجريبي
package com.test.app;
import android.app.*;
import android.os.*;
import android.widget.*;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        TextView tv = new TextView(this);
        tv.setText("Hello AIDE!");
        setContentView(tv);
    }
}
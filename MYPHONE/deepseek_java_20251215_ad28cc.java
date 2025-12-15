package com.phonebook.directory;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    
    private EditText etSearch;
    private Button btnSearchName, btnSearchNumber, btnImport, btnExport, btnCall, btnSMS;
    private ListView listView;
    private List<Map<String, String>> contactList = new ArrayList<>();
    private SimpleAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupAdapter();
        loadSampleData();
    }
    
    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        btnSearchName = findViewById(R.id.btnSearchName);
        btnSearchNumber = findViewById(R.id.btnSearchNumber);
        btnImport = findViewById(R.id.btnImport);
        btnExport = findViewById(R.id.btnExport);
        btnCall = findViewById(R.id.btnCall);
        btnSMS = findViewById(R.id.btnSMS);
        listView = findViewById(R.id.listView);
        
        // تعريف الأزرار المخفية في القائمة
        Button btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> showHiddenMenu());
        
        // إضافة مستمعين للأزرار
        btnSearchName.setOnClickListener(v -> searchByName());
        btnSearchNumber.setOnClickListener(v -> searchByNumber());
        btnImport.setOnClickListener(v -> importContacts());
        btnExport.setOnClickListener(v -> exportContacts());
        btnCall.setOnClickListener(v -> makeCall());
        btnSMS.setOnClickListener(v -> sendSMS());
    }
    
    private void setupAdapter() {
        adapter = new SimpleAdapter(
            this,
            contactList,
            android.R.layout.simple_list_item_2,
            new String[] {"name", "number"},
            new int[] {android.R.id.text1, android.R.id.text2}
        );
        listView.setAdapter(adapter);
    }
    
    private void loadSampleData() {
        // بيانات تجريبية
        addContact("أحمد محمد", "0123456789");
        addContact("سارة خالد", "0111222333");
        addContact("محمد علي", "0105555666");
    }
    
    private void addContact(String name, String number) {
        Map<String, String> contact = new HashMap<>();
        contact.put("name", name);
        contact.put("number", number);
        contactList.add(contact);
        adapter.notifyDataSetChanged();
    }
    
    private void searchByName() {
        String query = etSearch.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "أدخل اسم للبحث", Toast.LENGTH_SHORT).show();
            return;
        }
        
        List<Map<String, String>> results = new ArrayList<>();
        for (Map<String, String> contact : contactList) {
            if (contact.get("name").contains(query)) {
                results.add(contact);
            }
        }
        
        updateListView(results);
        Toast.makeText(this, "وجدت " + results.size() + " نتيجة", Toast.LENGTH_SHORT).show();
    }
    
    private void searchByNumber() {
        String query = etSearch.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "أدخل رقم للبحث", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // تحويل الأرقام العربية إلى إنجليزية
        query = arabicToEnglishNumbers(query);
        
        List<Map<String, String>> results = new ArrayList<>();
        for (Map<String, String> contact : contactList) {
            String number = arabicToEnglishNumbers(contact.get("number"));
            if (number.contains(query)) {
                results.add(contact);
            }
        }
        
        updateListView(results);
    }
    
    private String arabicToEnglishNumbers(String text) {
        return text
            .replace("٠", "0").replace("١", "1").replace("٢", "2")
            .replace("٣", "3").replace("٤", "4").replace("٥", "5")
            .replace("٦", "6").replace("٧", "7").replace("٨", "8")
            .replace("٩", "9");
    }
    
    private void updateListView(List<Map<String, String>> data) {
        contactList.clear();
        contactList.addAll(data);
        adapter.notifyDataSetChanged();
    }
    
    private void showHiddenMenu() {
        // عرض القائمة المخفية
        btnImport.setVisibility(btnImport.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        btnExport.setVisibility(btnExport.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }
    
    private void importContacts() {
        try {
            // استيراد من ملف نصي
            File file = new File(Environment.getExternalStorageDirectory(), "contacts.txt");
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                fis.read(data);
                fis.close();
                
                String content = new String(data, "UTF-8");
                String[] lines = content.split("\n");
                
                for (String line : lines) {
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        addContact(parts[0].trim(), parts[1].trim());
                    }
                }
                
                Toast.makeText(this, "تم استيراد " + lines.length + " جهة اتصال", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "أنشئ ملف contacts.txt أولاً", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "خطأ في الاستيراد", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void exportContacts() {
        try {
            File file = new File(Environment.getExternalStorageDirectory(), "contacts_export.txt");
            FileOutputStream fos = new FileOutputStream(file);
            
            StringBuilder content = new StringBuilder();
            for (Map<String, String> contact : contactList) {
                content.append(contact.get("name")).append(",").append(contact.get("number")).append("\n");
            }
            
            fos.write(content.toString().getBytes("UTF-8"));
            fos.close();
            
            Toast.makeText(this, "تم التصدير إلى: " + file.getPath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "خطأ في التصدير", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void makeCall() {
        if (!contactList.isEmpty()) {
            String number = contactList.get(0).get("number");
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + number));
            startActivity(intent);
        }
    }
    
    private void sendSMS() {
        if (!contactList.isEmpty()) {
            String number = contactList.get(0).get("number");
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + number));
            startActivity(intent);
        }
    }
}
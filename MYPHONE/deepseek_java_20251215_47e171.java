package com.example.phonebook;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    
    private static final int PERMISSIONS_REQUEST = 100;
    private static final int FILE_PICKER_REQUEST = 200;
    
    private RecyclerView recyclerView;
    private ContactAdapter adapter;
    private ArrayList<Contact> contactList = new ArrayList<>();
    private ArrayList<Contact> filteredList = new ArrayList<>();
    private EditText etSearch;
    private Spinner spinnerSearchType;
    private TextView tvCount;
    private ImageButton btnMenu, btnSearch, btnCall, btnSms, btnEdit, btnSave;
    private DatabaseHelper dbHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        checkPermissions();
        initDatabase();
        setupRecyclerView();
        setupListeners();
        loadContacts();
        updateCount();
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        etSearch = findViewById(R.id.et_search);
        spinnerSearchType = findViewById(R.id.spinner_search_type);
        tvCount = findViewById(R.id.tv_count);
        btnMenu = findViewById(R.id.btn_menu);
        btnSearch = findViewById(R.id.btn_search);
        btnCall = findViewById(R.id.btn_call);
        btnSms = findViewById(R.id.btn_sms);
        btnEdit = findViewById(R.id.btn_edit);
        btnSave = findViewById(R.id.btn_save);
        
        // تكوين Spinner
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
            this, R.array.search_types, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSearchType.setAdapter(spinnerAdapter);
    }
    
    private void initDatabase() {
        dbHelper = new DatabaseHelper(this);
    }
    
    private void setupRecyclerView() {
        adapter = new ContactAdapter(filteredList, new ContactAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Contact contact) {
                showContactOptions(contact);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
    
    private void setupListeners() {
        btnMenu.setOnClickListener(v -> showMainMenu());
        btnSearch.setOnClickListener(v -> performSearch());
        btnCall.setOnClickListener(v -> makeCall());
        btnSms.setOnClickListener(v -> sendSMS());
        btnEdit.setOnClickListener(v -> toggleEditMode());
        btnSave.setOnClickListener(v -> saveContacts());
        
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                performSearch();
            }
        });
    }
    
    private void checkPermissions() {
        String[] permissions = {
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsToRequest.toArray(new String[0]), PERMISSIONS_REQUEST);
        }
    }
    
    @SuppressLint("Range")
    private void loadContacts() {
        contactList.clear();
        
        // تحميل من قاعدة البيانات الداخلية
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM contacts", null);
        
        while (cursor.moveToNext()) {
            Contact contact = new Contact();
            contact.setId(cursor.getInt(cursor.getColumnIndex("id")));
            contact.setName(cursor.getString(cursor.getColumnIndex("name")));
            contact.setNumber(cursor.getString(cursor.getColumnIndex("number")));
            contactList.add(contact);
        }
        cursor.close();
        
        performSearch();
    }
    
    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        int searchType = spinnerSearchType.getSelectedItemPosition();
        
        filteredList.clear();
        
        if (query.isEmpty()) {
            filteredList.addAll(contactList);
        } else {
            for (Contact contact : contactList) {
                boolean matches = false;
                
                if (searchType == 0) { // بحث بالاسم
                    matches = normalizeArabic(contact.getName()).contains(normalizeArabic(query)) ||
                             contact.getName().contains(query);
                } else { // بحث بالرقم
                    String normalizedContactNum = normalizePhoneNumber(contact.getNumber());
                    String normalizedQuery = normalizePhoneNumber(query);
                    
                    matches = normalizedContactNum.contains(normalizedQuery) ||
                             contact.getNumber().contains(query);
                }
                
                if (matches) {
                    filteredList.add(contact);
                }
            }
        }
        
        adapter.notifyDataSetChanged();
        updateCount();
    }
    
    private String normalizeArabic(String text) {
        if (text == null) return "";
        return text.replace("أ", "ا")
                   .replace("إ", "ا")
                   .replace("آ", "ا")
                   .replace("ى", "ي")
                   .replace("ة", "ه")
                   .replaceAll("\\s+", "")
                   .toLowerCase();
    }
    
    private String normalizePhoneNumber(String number) {
        if (number == null) return "";
        return number.replaceAll("[\\s\\-\\(\\)\\+]", "")
                    .replace("٠", "0").replace("١", "1").replace("٢", "2")
                    .replace("٣", "3").replace("٤", "4").replace("٥", "5")
                    .replace("٦", "6").replace("٧", "7").replace("٨", "8")
                    .replace("٩", "9");
    }
    
    private void showMainMenu() {
        PopupMenu popup = new PopupMenu(this, btnMenu);
        popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            
            if (id == R.id.menu_import) {
                importContacts();
                return true;
            } else if (id == R.id.menu_export) {
                exportContacts();
                return true;
            } else if (id == R.id.menu_sync) {
                syncWithPhoneContacts();
                return true;
            } else if (id == R.id.menu_update) {
                updateContacts();
                return true;
            } else if (id == R.id.menu_settings) {
                // فتح الإعدادات
                return true;
            }
            return false;
        });
        
        popup.show();
    }
    
    private void importContacts() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, FILE_PICKER_REQUEST);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == FILE_PICKER_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            importFromFile(uri);
        }
    }
    
    private void importFromFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            
            // محاولة استخراج الأسماء والأرقام من النص
            String text = content.toString();
            Pattern pattern = Pattern.compile("(\\+?[\\d\\s\\-\\(\\)]{8,})");
            Matcher matcher = pattern.matcher(text);
            
            // استخراج الأسماء من السياق
            String[] lines = text.split("\n");
            Map<String, String> contacts = new HashMap<>();
            
            for (String currentLine : lines) {
                matcher = pattern.matcher(currentLine);
                if (matcher.find()) {
                    String number = matcher.group(1).trim();
                    String name = currentLine.replace(number, "").trim();
                    if (!name.isEmpty()) {
                        contacts.put(number, name);
                    }
                }
            }
            
            // حفظ في قاعدة البيانات
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            for (Map.Entry<String, String> entry : contacts.entrySet()) {
                ContentValues values = new ContentValues();
                values.put("name", entry.getValue());
                values.put("number", entry.getKey());
                db.insert("contacts", null, values);
            }
            
            loadContacts();
            Toast.makeText(this, "تم استيراد " + contacts.size() + " جهة اتصال", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "خطأ في الاستيراد: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    private void exportContacts() {
        try {
            File file = new File(Environment.getExternalStorageDirectory(), 
                "دليل_هاتفي_" + System.currentTimeMillis() + ".txt");
            FileOutputStream fos = new FileOutputStream(file);
            
            StringBuilder content = new StringBuilder();
            for (Contact contact : contactList) {
                content.append(contact.getName()).append("\t").append(contact.getNumber()).append("\n");
            }
            
            fos.write(content.toString().getBytes(StandardCharsets.UTF_8));
            fos.close();
            
            Toast.makeText(this, "تم التصدير إلى: " + file.getPath(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "خطأ في التصدير", Toast.LENGTH_SHORT).show();
        }
    }
    
    @SuppressLint("Range")
    private void syncWithPhoneContacts() {
        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null, null, null, null);
        
        if (cursor != null) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int count = 0;
            
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String number = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.NUMBER));
                
                if (name != null && number != null) {
                    // التحقق إذا كان الرقم موجود مسبقاً
                    Cursor checkCursor = db.rawQuery(
                        "SELECT * FROM contacts WHERE number = ?", new String[]{number});
                    
                    if (!checkCursor.moveToFirst()) {
                        ContentValues values = new ContentValues();
                        values.put("name", name);
                        values.put("number", number);
                        db.insert("contacts", null, values);
                        count++;
                    }
                    checkCursor.close();
                }
            }
            cursor.close();
            
            loadContacts();
            Toast.makeText(this, "تمت المزامنة: " + count + " جهة جديدة", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateContacts() {
        // تحديث الأسماء بناءً على الأرقام المتشابهة
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        // مثال: دمج الأرقام المكررة
        db.execSQL("DELETE FROM contacts WHERE id NOT IN " +
                  "(SELECT MIN(id) FROM contacts GROUP BY number)");
        
        loadContacts();
        Toast.makeText(this, "تم تحديث قاعدة البيانات", Toast.LENGTH_SHORT).show();
    }
    
    private void showContactOptions(Contact contact) {
        PopupMenu popup = new PopupMenu(this, recyclerView);
        popup.getMenuInflater().inflate(R.menu.contact_menu, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            
            if (id == R.id.menu_contact_call) {
                makeCall(contact.getNumber());
                return true;
            } else if (id == R.id.menu_contact_sms) {
                sendSMS(contact.getNumber());
                return true;
            } else if (id == R.id.menu_contact_edit) {
                editContact(contact);
                return true;
            } else if (id == R.id.menu_contact_delete) {
                deleteContact(contact);
                return true;
            } else if (id == R.id.menu_contact_view_similar) {
                showSimilarNumbers(contact.getNumber());
                return true;
            }
            return false;
        });
        
        popup.show();
    }
    
    private void makeCall() {
        if (!filteredList.isEmpty()) {
            makeCall(filteredList.get(0).getNumber());
        }
    }
    
    private void makeCall(String number) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
                == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + number));
            startActivity(intent);
        }
    }
    
    private void sendSMS() {
        if (!filteredList.isEmpty()) {
            sendSMS(filteredList.get(0).getNumber());
        }
    }
    
    private void sendSMS(String number) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + number));
        startActivity(intent);
    }
    
    private void toggleEditMode() {
        // تبديل وضع التعديل
        adapter.setEditMode(!adapter.isEditMode());
        btnSave.setVisibility(adapter.isEditMode() ? View.VISIBLE : View.GONE);
    }
    
    private void saveContacts() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        for (Contact contact : contactList) {
            ContentValues values = new ContentValues();
            values.put("name", contact.getName());
            values.put("number", contact.getNumber());
            db.update("contacts", values, "id = ?", new String[]{String.valueOf(contact.getId())});
        }
        
        toggleEditMode();
        Toast.makeText(this, "تم حفظ التغييرات", Toast.LENGTH_SHORT).show();
    }
    
    private void editContact(Contact contact) {
        // فتح نافذة تعديل
        // (يمكن إضافة Dialog خاص بالتعديل)
    }
    
    private void deleteContact(Contact contact) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("contacts", "id = ?", new String[]{String.valueOf(contact.getId())});
        loadContacts();
        Toast.makeText(this, "تم الحذف", Toast.LENGTH_SHORT).show();
    }
    
    @SuppressLint("Range")
    private void showSimilarNumbers(String number) {
        String normalizedNumber = normalizePhoneNumber(number);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.rawQuery(
            "SELECT * FROM contacts WHERE REPLACE(REPLACE(number, ' ', ''), '-', '') LIKE ?",
            new String[]{"%" + normalizedNumber + "%"});
        
        ArrayList<Contact> similarContacts = new ArrayList<>();
        while (cursor.moveToNext()) {
            Contact contact = new Contact();
            contact.setId(cursor.getInt(cursor.getColumnIndex("id")));
            contact.setName(cursor.getString(cursor.getColumnIndex("name")));
            contact.setNumber(cursor.getString(cursor.getColumnIndex("number")));
            similarContacts.add(contact);
        }
        cursor.close();
        
        // عرض النتائج في Popup
        // (يمكن إضافة Dialog لعرض الأسماء المتشابهة)
    }
    
    private void updateCount() {
        tvCount.setText(String.valueOf(filteredList.size()));
    }
    
    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}
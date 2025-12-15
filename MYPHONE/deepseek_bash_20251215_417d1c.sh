1. أنشئ حساب على GitHub
2. أنشئ مستودع جديد باسم "phonebook-directory"
3. ارفع جميع الملفات
4. أضف ملف .github/workflows/android.yml:

name: Android CI

on: [push]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - name: Build APK
      uses: maierj/fastlane-action@v1.4.0
      with:
        lane: android build
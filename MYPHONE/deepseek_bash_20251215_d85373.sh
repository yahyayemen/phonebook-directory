# 1. تثبيت Termux من Google Play
# 2. فتح Termux وتنفيذ:

pkg update && pkg upgrade
pkg install git
pkg install openjdk-17
pkg install ecj
pkg install dx
pkg install aapt

# 3. تنزيل مشروع Android قالب
git clone https://github.com/termux/termux-create-package

# 4. لكن هذه الطريقة معقدة للتطبيقات الكبيرة
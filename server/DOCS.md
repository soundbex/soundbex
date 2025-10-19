# Python PATH Ayarları (Mac/Linux)

Python kontrolü
````
python3 --version
which python3
````

PATH ekleme, geçici
````
export PATH="/Library/Frameworks/Python.framework/Versions/3.12/bin:$PATH"
````

PATH="" içerisine python kontolünde çıkan dizin girilecek.
Kalıcı olmasını isterseniz .zshrc veya .bashrc dosyalarınıza ekleyebilirsiniz.

### Symlink oluşturma - Gerekebilir
````
sudo ln -s /Library/Frameworks/Python.framework/Versions/3.12/bin/python3 /usr/local/bin/python
````
yine sizin python dizininize göre değişiklik gösterir.


## Kullanılan modüller

````
npm i express cors youtube-sr yt-dlp-exec axios
````

## Backendi başlat
```
node server.js
```
3000 portunda çalışmaya başlayacaktır.
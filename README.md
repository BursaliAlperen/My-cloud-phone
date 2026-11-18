# MyCloudPhone

UGPhone benzeri bulut Android ekranina baglanmak icin Android istemcisi.

## Bu surumun kapsami
- Kotlin tabanli Android APK istemcisi.
- HTTPS ile sunucudaki web uzak ekran istemcisini (or. noVNC) acar.
- Sunucu adresini cihazda saklar.
- Ana cihazdan Android Storage Access Framework ile APK secmeye izin verir.
- Secilen APK'yi HTTPS POST multipart istegiyle `/api/apps/install` adresine gonderir.
- Istege istege bagli `Authorization: Bearer <token>` basligi ekler.
- GitHub Actions her push'ta debug APK derler ve artifact olarak yukler.

## APK icinde APK ice aktarma
1. Bulut sunucusunun HTTPS adresini gir (or. `https://cloud.example.com/vnc.html`).
2. Sunucunun APK aktarim API anahtarini gir.
3. **Ana cihazdan APK ice aktar** dugmesine bas ve APK dosyasini sec.
4. Uygulama dosyayi `POST https://<sunucu-host>/api/apps/install` adresine multipart `apk` alaniyla yollar.

**Sunucu tarafinda endpoint henuz bu depoda uygulanmamistir.** APK secme ve gonderme istemcisi hazirdir; dosyayi guvenli bicimde kabul edip dogrulayan ve Android sanal cihaza `adb install` ile kuran API sonraki sunucu gelistirme adimidir. Endpoint olmadan aktarim HTTP 404/501 benzeri hata verir; bu beklenen durumdur.

Endpoint gereksinimleri:
- HTTPS arkasinda calismali ve Bearer token dogrulamali.
- Dosya boyutu siniri, APK MIME/uzanti kontrolu ve Android paket imzasi/manifest dogrulamasi yapmali.
- Dosyayi rastgele web-root altina koymamali; gecici dizinde tutup dogrulama sonrasi ilgili Android VM'e kurmali.
- ADB'yi internete acmamali; yalnizca sunucu ic agindan kullanmali.
- Kurulum sonucunu JSON olarak donmeli.

## Onemli sinirlar
Bu APK tek basina Android sanal makinesi olusturmaz. Android isletim sistemi, Chrome, VPN, kalici disk ve noVNC/websockify uzak ekran hizmetleri bulut sunucusunda ayrica kurulmalidir. Sunucu tarafi hazir olmadan APK'da sanal Android goruntusu gorunmez. Google Play icin GMS iceren ve sertifikasyon kosullarini karsilayan ayri bir Android imaji gerekir.

## APK indirme
1. GitHub deposunda Actions sekmesini ac.
2. **Build MyCloudPhone APK** is akisini sec.
3. Basarili son calismayi ac.
4. Artifacts bolumunden `MyCloudPhone-debug-apk` dosyasini indir.
5. ZIP icindeki `app-debug.apk` dosyasini Android'e kur.

## Yerel derleme
Gereksinimler: Android Studio, JDK 17, Android SDK 35 ve Gradle 8.9.
`gradle assembleDebug`

## Guvenlik
- Yalnizca HTTPS adresleri kabul edilir.
- VNC, ADB ve websockify portlarini dogrudan internete acma.
- Uzak ekrani kimlik dogrulama ve TLS arkasinda yayinla; mumkunse VPN veya erisim kontrolu kullan.
- APK aktarim anahtarini paylasma; test disinda guclu ve degistirilebilir anahtar kullan.
- Bu surum uzak ekran istemcisi ve APK gonderim istemcisidir; Android VM/kurulum API'si henuz sunucu tarafinda kurulmus degildir.


## APK kurulum API'si (uyumlu bir Linux Android-host üzerinde)

`server/` klasoru, istemcinin APK yukleme istegini kabul eden Node.js API'sini icerir.
API Bearer token ister, tek APK icin 250 MB boyut siniri uygular, ZIP/APK basligini kontrol eder ve APK'yi ayarli Android VM'e `adb install -r` ile kurar.

Ornek baslatma (API ile Android VM ayni ozel Docker aginda/ozel agda erisebilir olmali):

```sh
cd server
export MYCLOUDPHONE_UPLOAD_TOKEN="$(openssl rand -hex 32)"
export ADB_SERIAL="android-vm:5555"
docker build -t mycloudphone-install-api .
docker run --rm -p 8080:8080 \
  -e MYCLOUDPHONE_UPLOAD_TOKEN="$MYCLOUDPHONE_UPLOAD_TOKEN" \
  -e ADB_SERIAL="$ADB_SERIAL" \
  mycloudphone-install-api
```

Bu komut tek basina Android VM yaratmaz. `android-vm:5555` adresinde calisan ve yalnizca ozel agdan erisilen bir Android VM/ADB servisi onceden bulunmalidir. API'yi internete acacaksan TLS reverse proxy ve erisim kontrolu kullan; ADB portunu asla internete acma. Bu API, uyumlu sunucu bulunmadan APK yuklemesini tamamlayamaz.


## VPN (WireGuard)
The Android client includes a shortcut to the official WireGuard Android app. Install WireGuard, then import a WireGuard `.conf` profile (or QR code) supplied by a VPN provider or a WireGuard server you control. MyCloudPhone does not bundle a VPN exit server or provide free VPN credentials. Without a valid profile and reachable VPN server, no VPN connection can be established. WireGuard supports userspace operation without root on Android.

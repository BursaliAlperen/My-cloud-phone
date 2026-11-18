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

# MyCloudPhone

UGPhone-style bulut Android ekranina baglanmak icin Android istemcisi.

## Bu surumun kapsami
- Kotlin tabanli Android APK istemcisi.
- HTTPS ile sunucudaki web uzak ekran istemcisini (or. noVNC) acar.
- Sunucu adresini cihazda saklar.
- GitHub Actions her push'ta debug APK derler ve artifact olarak yukler.

ONEMLI: Bu APK tek basina Android sanal makinesi olusturmaz. Android isletim sistemi, Chrome, VPN, kalici disk ve noVNC/websockify uzak ekran hizmetleri bulut sunucusunda ayrica kurulmalidir. Sunucu tarafi hazir olmadan APK'da sanal Android goruntusu gorunmez. Google Play icin GMS iceren ve sertifikasyon kosullarini karsilayan ayri bir Android imaji gerekir.

## APK indirme
1. GitHub deposunda Actions sekmesini ac.
2. Build MyCloudPhone APK is akisini sec.
3. Basarili son calismayi ac.
4. Artifacts bolumunden MyCloudPhone-debug-apk dosyasini indir.
5. ZIP icindeki app-debug.apk dosyasini Android'e kur.

## Yerel derleme
Gereksinimler: Android Studio, JDK 17, Android SDK 35 ve Gradle 8.9.
Komut: gradle assembleDebug

## Sunucu adresi
Uygulamayi acip HTTPS ile yayinlanan uzak ekran URL'sini gir. Ornek:
https://cloud.example.com/vnc.html

Bu ornek gercek sunucu degildir. Kendi alan adini ve uzak ekran yolunu kullan.

## Guvenlik
- Yalnizca HTTPS adresleri kabul edilir.
- VNC, ADB ve websockify portlarini dogrudan internete acma.
- Uzak ekrani kimlik dogrulama ve TLS arkasinda yayinla; mumkunse VPN veya erisim kontrolu kullan.
- Bu APK su an uzak ekran istemci kabugudur; Android VM kurulumu bu depoda henuz yoktur.

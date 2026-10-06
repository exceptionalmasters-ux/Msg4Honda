# Msg4Honda

Minimalna aplikacja Android dla Hondy Civic IX (2015). Odczytuje lokalne
powiadomienia WhatsApp i na kilka sekund publikuje nadawcę oraz treść wiadomości
jako metadane sesji multimedialnej widoczne przez Bluetooth AVRCP. Pierwsza linia
zawiera nadawcę i napis WhatsApp, a treść zajmuje dwie kolejne linie.

## Założenia MVP

- Samsung Galaxy A53 5G, Android 14 / One UI 6.1
- źródło muzyki: Spotify
- jeden przycisk START/STOP
- wiadomości wyświetlane kolejno przez 2,5 sekundy
- maksymalnie 48 znaków wiadomości podzielonych na dwie linie
- brak uprawnienia `INTERNET`
- brak zapisywania treści wiadomości
- krótkie przejęcie audio focus i bezgłośny strumień wymuszający wybór sesji
  przez radio; Spotify powinno wznowić odtwarzanie po 2,5 sekundy
- po naciśnięciu START aplikacja publikuje komunikat „Test połączenia”, co
  pozwala sprawdzić radio bez czekania na wiadomość WhatsApp

## Budowanie

```bash
./gradlew assembleDebug
```

APK powstaje w `app/build/outputs/apk/debug/app-debug.apk`. Workflow GitHub
Actions buduje również artefakt `Msg4Honda-debug` po każdym pushu do `main`.

## Test w samochodzie

1. Sparuj telefon z Hondą i uruchom muzykę ze Spotify.
2. Uruchom Msg4Honda i naciśnij START.
3. Przy pierwszym uruchomieniu przyznaj dostęp do powiadomień.
4. Wyślij testową wiadomość WhatsApp z innego telefonu.
5. Sprawdź, czy radio pokazuje treść i czy po 2,5 sekundy
   wracają metadane Spotify.

Jeśli komunikat „Test połączenia” pojawia się po naciśnięciu START, ale
wiadomość WhatsApp nie, należy sprawdzić status „Ostatnio” w aplikacji. Pozwala
to rozróżnić problem powiadomień od problemu sesji Bluetooth.

To jest eksperymentalny MVP. Priorytet sesji multimedialnej i sposób
wyświetlania AVRCP zależą od oprogramowania telefonu i radia.

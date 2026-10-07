# Msg4Honda

Minimalna aplikacja Android dla Hondy Civic IX (2015). Odczytuje lokalne
powiadomienia WhatsApp, Messengera, SMS/RCS oraz nawigacji Google Maps i publikuje
je jako metadane sesji multimedialnej widoczne przez Bluetooth AVRCP.
Pierwsza linia zawiera nadawcę i nazwę źródła, a treść zajmuje dwie kolejne linie.

## Założenia MVP

- Samsung Galaxy A53 5G, Android 14 / One UI 6.1
- źródło muzyki: Spotify
- jeden przycisk START/STOP
- osobne przełączniki WhatsApp, Messenger, SMS/RCS i Google Maps
- wiadomości są dzielone na dwuliniowe strony po 48 znaków
- kolejne strony pojawiają się automatycznie co 2 sekundy
- przyciski następny/poprzedni przewijają strony wiadomości z komunikatorów
- przy wskazówce Google Maps przycisk zamyka ją i przekazuje zmianę utworu
  z powrotem do Spotify
- obsługiwane są zarówno komendy AVRCP, jak i surowe zdarzenia przycisków
- ostatnia strona znika po 5 sekundach, wszystkie strony są wtedy usuwane
- po wygaszeniu aplikacja wysyła puste metadane, aby komunikat zniknął także
  wtedy, gdy żaden odtwarzacz muzyki nie przejmuje ekranu radia
- przycisk `DANE MAPS` pokazuje pełny diagnostyczny zrzut ostatniego
  powiadomienia Map (klucze, wartości, teksty i identyfikatory obrazów);
  dane diagnostyczne nie są wysyłane do radia
- diagnostyka dołącza obraz PNG/Base64 pola `right_icon`, aby można było
  rozpoznać kształt strzałki niezależnie od zmiennego skrótu obrazu
- znane obrazy `right_icon` uzupełniają brakujący tekst manewru dla jazdy
  prosto, skrętów w lewo, w prawo, lekko w lewo i lekko w prawo oraz
  zawracania, bez zmiany parsera ulic i odległości
- wskazówki Map pokazują czas dojazdu, manewr ze strzałką ASCII i odległością
  oraz ulicę/kierunek;
  nowsza wskazówka zastępuje poprzednią i nie trafia do kolejki
- aktualizacje Map zawierające tylko malejącą odległość zachowują ostatni
  manewr i ulicę zamiast zastępować je samą liczbą metrów
- brak uprawnienia `INTERNET`
- brak zapisywania treści wiadomości
- bezgłośny strumień wymuszający wybór sesji przez radio, ale bez przejmowania
  audio focus; Spotify powinno odtwarzać muzykę bez przerwy
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
4. Wyślij testową wiadomość z jednego z włączonych źródeł.
5. Sprawdź, czy radio pokazuje treść bez przerwania muzyki i czy po 5 sekundach
   wracają metadane Spotify.

Jeśli komunikat „Test połączenia” pojawia się po naciśnięciu START, ale
wiadomość nie, należy sprawdzić status „Ostatnio” w aplikacji. Pozwala
to rozróżnić problem powiadomień od problemu sesji Bluetooth.

To jest eksperymentalny MVP. Priorytet sesji multimedialnej i sposób
wyświetlania AVRCP zależą od oprogramowania telefonu i radia.

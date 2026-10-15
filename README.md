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
- po wygaszeniu komunikatu sesja Msg4Honda kopiuje bieżący tytuł, album i
  wykonawcę z aktywnej sesji Spotify, a następnie jest dezaktywowana bez
  zwalniania identyfikatora wymaganego przez radio dla kolejnych powiadomień
- obsługiwane są zarówno komendy AVRCP, jak i surowe zdarzenia przycisków
- ostatnia strona znika po 5 sekundach, wszystkie strony są wtedy usuwane
- jeśli Spotify nie udostępnia aktywnej sesji, po wygaszeniu aplikacja wysyła
  puste metadane, aby komunikat nie pozostał na ekranie radia
- przycisk `DANE MAPS` pokazuje pełny diagnostyczny zrzut ostatniego
  powiadomienia Map (klucze, wartości, teksty i identyfikatory obrazów);
  dane diagnostyczne nie są wysyłane do radia
- diagnostyka dołącza obraz PNG/Base64 pola `right_icon`, aby można było
  rozpoznać kształt strzałki niezależnie od zmiennego skrótu obrazu
- znane obrazy `right_icon` uzupełniają brakujący tekst manewru dla jazdy
  prosto, skrętów w lewo, w prawo, lekko w lewo i lekko w prawo oraz
  zawracania, rond, kontynuowania tą samą trasą i trzymania się prawej strony,
  bez zmiany parsera ulic i odległości
- wskazówki Map pokazują czas dojazdu, manewr ze strzałką ASCII i odległością
  oraz ulicę/kierunek;
  nowsza wskazówka zastępuje poprzednią i nie trafia do kolejki
- aktualizacje odległości odświeżają widok tylko w bieżącym 5-sekundowym oknie;
  nie przedłużają czasu przejęcia ekranu, a ten sam manewr po wygaszeniu lub
  ręcznym zamknięciu pozostaje ukryty do czasu pojawienia się nowej wskazówki
- ciężki zrzut diagnostyczny i zapis danych dostępności są ograniczone do
  jednego wykonania na 5 sekund; rozpoznawanie manewrów nadal działa na każdej
  aktualizacji
- bezgłośny AudioTrack jest współdzielony przez kolejne aktualizacje metadanych
  i zwalniany dopiero przy oddaniu sesji Spotify
- pierwszy wiersz Map ma zwarty format bez zbędnych spacji, np.
  `3.1km-10min`
- opcjonalna usługa dostępności obserwuje wyłącznie ekran Google Maps i próbuje
  lokalnie odczytać widoczne ograniczenie prędkości; świeży odczyt jest dodawany
  do pierwszego wiersza, np. `70km/h-13km-30min`
- odczyt ograniczenia wygasa po minucie, nie uruchamia internetu i pozostawia
  dotychczasowy tytuł Map, jeśli Google Maps nie udostępni wartości
- ta sama usługa zbiera lokalne teksty alertów drogowych Google Maps; pojedyncze
  rozpoznane zdarzenie, np. korek, kontrola prędkości lub wypadek, tymczasowo
  zastępuje ulicę w trzecim wierszu radia
- wiele nazw zdarzeń widocznych jednocześnie jest traktowanych jako menu
  `Zgłoś`, a nie prawdziwy alert; surowe zdarzenia są dostępne w `DANE MAPS`
- aktualizacje Map zawierające tylko malejącą odległość zachowują ostatni
  manewr i ulicę zamiast zastępować je samą liczbą metrów
- brak uprawnienia `INTERNET`
- brak zapisywania treści wiadomości
- bezgłośny strumień wymuszający wybór sesji przez radio, ale bez przejmowania
  audio focus; Spotify powinno odtwarzać muzykę bez przerwy
- po naciśnięciu START aplikacja wita użytkownika i pokazuje włączone źródła,
  np. `Msg4Honda` / `Witaj :)` / `Whats, Mess, SMS, Maps`

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
4. Aby testować ograniczenia prędkości, naciśnij `WŁĄCZ OGRANICZENIA` i włącz
   usługę `Ograniczenia prędkości Maps` w ustawieniach dostępności Androida.
5. Wyślij testową wiadomość z jednego z włączonych źródeł.
6. Sprawdź, czy radio pokazuje treść bez przerwania muzyki i czy po 5 sekundach
   wracają metadane Spotify.

Jeśli komunikat powitalny pojawia się po naciśnięciu START, ale
wiadomość nie, należy sprawdzić status „Ostatnio” w aplikacji. Pozwala
to rozróżnić problem powiadomień od problemu sesji Bluetooth.

To jest eksperymentalny MVP. Priorytet sesji multimedialnej i sposób
wyświetlania AVRCP zależą od oprogramowania telefonu i radia.


## Punkt powrotu

Wersja sprzed optymalizacji stabilności jest zachowana na gałęzi
`backup-v0.3.10-before-stability` (commit
`c2d75375af8a3c5192ac64a58163f0c55d18feb9`). Zmiany 1–5 są celowo
zgrupowane w jednym kolejnym commicie, dzięki czemu można je łatwo wycofać.

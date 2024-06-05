# HTTP Load Generator
## Prekwizyty
Do uruchomienia programu należy posiadać zainstalowane JDK Javy w wersji min. 21 oraz maven.
## Kompilacja
`mvn clean package`
Po takiej kompilacji otrzymujemy gotowy plik `.jar` do uruchomienia
### Uruchomienie po kompilacji
Aby uruchomić program, musimy umieścić w jednym katalogu plik z kodem `.jar` oraz plik `requests.json` z konfiguracją zapytań.
Następnie uruchamiamy mastera komendą:  
`java -jar ./target/load-tester.jar --profile=master`  
Nody uruchamiamy w dowolny miejscu komendą zamieniając `{address}` 
poprawnym adresem mastera w formacie tj. `tcp://{ip}:{port}`:  
`java -jar ./target/load-tester.jar --profile=node --master-address={address}`  
Uruchomiony w ten sposób system wykona swoje zadania, po czym wyłączy się.

Przykładowa konfiguracja w pliku JSON to:
```
{
  "requests": [
    {
      "name": "getRandom",
      "uri": "http://10.169.169.221:8080/random",
      "method": "GET",
      "headers": {
        "Accept": "Application/json",
        "X-Custom": "Header",
        "X-Custom": "Text"
      },
      "timeout": 1000,
      "expectedReturnStatusCode": 200,
      "count": 1000
    }
  ],
  "nodes": 0,
  "nextRequestDelay": 1,
  "defaultHeaders": {
    "Default-header": "Default Value",
    "Vary": "Another one"
  }
}
```
Konfiguracja pozwala na specyfikowanie zapytań w tym nagłówków, metod, zawartości oraz adresu. Pozwala także na określenie
czasu, po którym zapytanie ma być uznane za odrzucone oraz oczekiwany kod odpowiedzi. Dodatkowo należy podać oczekiwany czas
pomiędzy wysłaniem kolejnych zapytań. Typ zapytania do wysłania jest wybrany losowo spośród dostępnych pozostałych zapytań.
Wyniki wykonania programu są zapisywane w podkatalogu opisanym datą wykonania w katalogu `statistics`.  
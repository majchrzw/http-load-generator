# HTTP Load Generator

## Uruchomienie
### Master
`mvn spring-boot:run -D"spring-boot.run.profiles=master"`
### Node
`mvn spring-boot:run -D"spring-boot.run.profiles=node"`

## Kompilacja
`mvn clean package -D"skip.tests"`
Po takiej kompilacji otrzymujemy gotowy plik `.jar` do uruchomienia
### Uruchomienie po kompilacji
Aby uruchomić program, musimy umieścić w jednym katalogu plik z kodem `.jar` oraz plik `requests.json` z konfiguracją zapytań.  
Następnie uruchamiamy mastera komendą:
`java -jar -D'spring.profiles.active=master ./{nazwa pliku .jar}`  
Nody uruchamiamy w dowolny miejscu komendą:
`java -jar -D'spring.profiles.active=node ./{nazwa pliku .jar}`
Uruchomiony w ten sposób system wykona swoje zadania, po czym wyłączy sie.
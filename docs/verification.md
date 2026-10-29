# Prüfbericht

Stand: 29. Oktober 2026, GarnBox 0.1.0.

## Internationalisierung am 29. Oktober 2026

69 JUnit-Testfälle bestehen, einschließlich 25 zusätzlicher Prüfungen für
Sprachkataloge und Einstellungen. Alle sechs Kataloge enthalten dieselben
91 Schlüssel. Platzhalter, Singular und Plural, regionale Zahlengruppierung,
Apostrophe und Akzente, Systemsprachvarianten und englischer Fallback sind
geprüft. Eine Spracheinstellung darf die bestehende Datenbank, Texte und
Foto-Bytes nicht verändern. Fehlende, unbekannte, übergroße und beschädigte
Einstellungen sowie fehlgeschlagener Dateiaustausch sind berücksichtigt.

Formatter, Compiler-Lint mit `-Werror`, Enforcer und SpotBugs bestehen.
Die Python-Helfer bestehen Ruff-Prüfung und Formatkontrolle. Die Shell-Skripte
bestehen `bash -n`. Die Versionsnummer bleibt 0.1.0, es ist kein Store-Upload.

Je Sprache wurden drei echte JavaFX-Prozesse auf einem eigenen Xvfb-Bildschirm
gestartet. Die 18 erfolgreichen Abläufe prüfen lokalisierte Oberfläche und
Fehler, Unicode-Suche, lange Notizen, PNG-/JPEG-Import über den nativen Dialog,
abgelehnte ungültige Bilder, Fotoersatz, Entfernen und Verwerfen, SQLite-Sperre
mit erneutem Speichern, sicheres Abbrechen des Löschens, Einstellungen mit
Schreibfehler und einen neuen Prozess nach Sprachwechsel. Bildbytes und Textdaten
werden zusätzlich direkt aus SQLite gelesen und verglichen.

Hauptfenster mit 860 × 700 und Editor mit 660 × 540 Pixeln wurden geprüft.
Fehlerhinweise umbrechen und die Aktionen bleiben erreichbar. Der Editor
scrollt die längeren Inhalte. Die 24 unveränderten Bildschirmaufnahmen der
produktiven App zeigen ausschließlich synthetische Texte und eigene gezeichnete
Garnmotive. Übersicht, Fotodialog, Suche und Sprachwahl wurden visuell geprüft.

Das aktuelle Linux-App-Image startet mit eigener JVM außerhalb des Projekts,
ohne `JAVA_HOME` und mit `PATH=/nonexistent`. Der Test umfasst 1234 vorhandene
Einträge, französische Zahlengruppierung, Unicode-Änderung, nativen JPEG-Import,
Sprachwechsel über die Oberfläche und englischen Neustart. Nach Löschen der
Bildquelle bleibt die gespeicherte Fotovorschau erhalten. Die geladenen
JVM-Bibliotheken und die unveränderte Paketlaufzeit wurden kontrolliert.

Nachweise liegen in `target/gui-international-final` und
`target/packaged-international-final`. Die endgültige Lieferung übernimmt sie
in `Verification` und ergänzt den geprüften Quellcommit sowie SHA-256-Werte.
Sie wird als Ordner ohne ZIP geliefert. Befehle, Voraussetzungen und Grenzen
stehen in [internationalization.md](internationalization.md).

Die folgenden Abschnitte dokumentieren zusätzlich die ursprüngliche deutsche
Abnahme vom 26. Oktober 2026.

## Automatische Prüfungen

Linux x64, Eclipse Temurin JDK 21.0.11, JavaFX 21.0.11,
SQLite JDBC 3.53.4.0 und Maven Wrapper 3.9.16.

```bash
./mvnw spotless:apply
./mvnw verify
bash -n scripts/run.sh scripts/package.sh
git diff --cached --check
```

Bei der ursprünglichen Abnahme bestanden 44 JUnit-Testfälle. Compiler-Lint mit `-Werror`, Maven Enforcer,
Spotless und SpotBugs bestanden ohne Befunde.

| Bereich | Geprüft |
| --- | --- |
| Pflichtfeld und optionale Texte | Fehlend, leer, Unicode-Leerraum, Bereinigung, mehrzeilige Notiz, optionale Farbe |
| Suche | Alle fünf Textfelder, Umlaute, Unicode-Normalisierung, leere Suche, keine Treffer, unabhängige gleichnamige Einträge |
| SQLite | Anlegen, Bearbeiten, Löschen, stabile IDs, Wiederöffnen einer temporären Datei, SQL-Sonderzeichen als Daten |
| Speicherfehler | Exklusive Sperren bei Anlegen, Ersetzen, Entfernen und Löschen, unveränderte gespeicherte Texte und Fotos, Wiederholung nach Freigabe |
| Atomare Änderungen | Ein künstlicher SQLite-Trigger bricht UPDATE ab. Texte und BLOB bleiben gemeinsam unverändert |
| Foto-Lebenszyklus | Hinzufügen, Ersetzen, Entfernen, Löschen, Wiederöffnen nach Löschen der Quelle, Quelldatei nach Fotoentfernung unverändert |
| Bildimport | JPEG und PNG nach Inhalt, falsche Endung, umbenanntes GIF, Textdatei, leere und abgeschnittene Dateien, fehlende Datei, Verzeichnis |
| Grenzen | Mehr als 10 MiB, mehr als 6000 Pixel Breite oder Höhe, mehr als 16 Millionen Pixel, zulässige Dimensionsgrenzen |
| Werteklasse | Defensive Arraykopien, inhaltsbasierte Gleichheit und Hashcode |
| Datenpfade und Fehler | Windows, macOS, Linux, ungültiger Pfad, beschädigte Datenbank und unbekannte neuere Schema-Version |

Es werden ausschließlich generierte Farbflächen und temporäre Datenbanken
verwendet. Die Tests öffnen keine Benutzersammlungen.

## Tatsächlich gestartete Oberfläche

Die Anwendung wurde auf einem separaten Linux-Xvfb-Bildschirm mit Maus und
Tastatur über `xdotool` bedient. Screenshots wurden visuell kontrolliert,
gespeicherte Inhalte zusätzlich mit einer unabhängigen SQLite-Verbindung gelesen.
Testpfad: `target/gui-test-data`, ausschließlich synthetische Garne und Bilder.
Die Testhilfen liegen außerhalb des Produktcodes. Diese Prüfung ist kein
plattformübergreifender automatisierter GUI-Test in `mvn verify`.

| Ablauf | Ergebnis |
| --- | --- |
| Leere Sammlung starten | Einladung zum ersten Garn, neutraler Fotoplatzhalter |
| Leere Bezeichnung speichern | Klarer Feldhinweis, kein Datenbankeintrag |
| Alle Texte und mehrzeilige Notiz eingeben | Korrekt gespeichert, Enter erzeugt im Notizfeld eine neue Zeile |
| PNG über echte Dateiauswahl hinzufügen | Vorschau im Dialog und Detailbereich, BLOB gespeichert |
| Nach „ÖMER“ aus der Notiz suchen | Eintrag gefunden |
| Suche ohne Treffer und anschließendes Leeren | Getrennte Leerzustände, gesamte Sammlung wieder sichtbar |
| Dateiauswahl abbrechen | Unveränderter Dialog schließt anschließend ohne Verwerfrückfrage |
| Text ändern und Foto entfernen, Escape | Rückfrage, Weiterbearbeiten als sichere Vorauswahl |
| Dialogfenster mit Änderungen schließen | Dieselbe Rückfrage, Verwerfen erhält bisherigen Datenbankstand |
| Foto entfernen und „Abbrechen“ wählen | Rückfrage, gespeichertes Foto bleibt vorhanden |
| Ungültiges Foto auswählen | Klare Meldung, bisheriger Entwurf und gespeichertes Foto erhalten |
| PNG durch JPEG ersetzen | Neue Vorschau sichtbar |
| Während Speichern SQLite exklusiv sperren | Dialog bleibt offen, neue Texte und Fotovorschau erhalten, alte Daten unverändert |
| Sperre lösen und erneut speichern | Derselbe Entwurf erfolgreich gespeichert |
| Beide ursprünglichen Bilddateien löschen und App neu starten | Gespeicherte Texte und JPEG-Vorschau weiterhin vorhanden |
| Foto entfernen und speichern | BLOB ist NULL, neutraler Platzhalter sichtbar |
| Löschbestätigung abbrechen | Eintrag bleibt bestehen |
| Löschen bestätigen | Nur ausgewählter Eintrag entfernt, leere Sammlung wieder sichtbar |
| Dialog auf 660 × 540 verkleinern | Formular scrollbar, Abbrechen und Speichern erreichbar |

[Beispielansicht](screenshots/garnbox.png) und
[Speicherfehler mit erhaltenem Entwurf](screenshots/save-failed.png) zeigen
synthetische Testdaten. Weitere lokale Nachweise liegen im ignorierten
Verzeichnis `target/gui-evidence`.

## Paketprüfung mit eingebetteter Laufzeit

```bash
./scripts/package.sh app-image
./target/app-image/GarnBox/bin/GarnBox --version
```

Das Linux-x64-App-Image wurde gebaut und mit echter Oberfläche gestartet.
Der Start gelang auch mit entferntem `JAVA_HOME` und `PATH=/nonexistent`.
Die Prozessbibliotheken zeigten die JVM aus
`target/app-image/GarnBox/lib/runtime/lib/server/libjvm.so`.
Die Versionsausgabe lautet `GarnBox 0.1.0`.

Im Paket wurden eine zuvor gespeicherte Sammlung und ihr Foto nach Neustart
geöffnet. Fotoentfernung, Abbruch und bestätigte Löschung funktionierten.
Ein weiterer Eintrag mit neu importiertem PNG wurde direkt im paketierten
Programm angelegt. Damit sind auch ImageIO, JavaFX-Bildanzeige und das native
SQLite JDBC im Paket praktisch geprüft. Ein Versionsaufruf allein war nicht
der Abnahmenachweis.

## Reproduzierbare manuelle Abnahme

Ein neues leeres Testverzeichnis wählen und den Launcher mit
`--data-dir=/absoluter/testpfad` starten. Ein selbst erzeugtes JPEG und PNG
verwenden. Den obigen Ablauf auf dem jeweiligen Zielsystem wiederholen.
Für den Fehlerfall ausschließlich die Testdatenbank mit einer zweiten
SQLite-Verbindung und `BEGIN EXCLUSIVE` sperren. Nach der erwarteten Meldung
mit `ROLLBACK` freigeben und denselben Dialog erneut speichern.

## Noch offen

Windows und macOS stehen in dieser Umgebung nicht zur Verfügung. EXE, DMG,
PowerShell-Helfer und GitHub-Workflows sind vorbereitet, aber dort nicht gebaut
oder ausgeführt. Intel und Apple Silicon benötigen jeweils eine native Abnahme.

Zu prüfen bleiben Installation, Update, Deinstallation ohne Datenverlust,
Skalierung, native Dateiauswahl, Tastenkürzel und Plattform-Sicherheitsdialoge.
Die Linux-Prüfung ersetzt diese Tests nicht.

Store-Identitäten, Signierung, Mac-App-Sandbox und Einreichungspakete sind offen.
Die Einzelheiten stehen in [packaging.md](packaging.md). Ein normaler Installer
ist keine geprüfte Store-Version. Kein Push, Upload oder Release wurde ausgeführt.

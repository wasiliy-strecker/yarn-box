# Prüfbericht

## App-Icons auf den drei Desktop-Plattformen, 31. Oktober 2026

Vorhandene Motive in zusätzliche Fenstergrößen, Windows-ICO und macOS-ICNS
überführt. Fenster und Startfehler verwenden die App-Icons. Linux erhält einen
benutzerspezifischen Anwendungseintrag mit passender JavaFX-Fensterklasse und
Icons. Windows- und macOS-Pakethelfer verknüpfen die jeweiligen Icon-Dateien
explizit mit Launcher/Bundle und Installer.

Tatsächlich ausgeführt: Icon-Erzeugung mit Pillow, Formatter und Java-Paketbau
mit `-Dmaven.test.skip=true`. Protokoll: `target/desktop-icons-build.log`.
Vorhandene Linux-App-Images mit App-JAR, Icons, Desktop-Installer und Anleitung
aktualisiert. Die Linux-Desktop-Registrierung wurde für das aktuelle Benutzerkonto
ausgeführt. Keine vollständigen nativen Installer neu gebaut.

Die Anwendung wurde in diesem Auftrag nicht gestartet. Der neue Menüeintrag
verwendet den bisherigen Standarddatenpfad. Visuelle Abnahme bleibt offen.

Auf Nutzerwunsch keine Tests, kein `verify` und keine automatisierten GUI-Prüfungen.
Manuelle Abnahme: App über die Ubuntu-Anwendungsübersicht starten, eigenes Icon
im Dock und beim Fensterwechsel ansehen, Dialoge öffnen und nach einem Neustart
die Zuordnung kontrollieren. Windows separat in Explorer, Startmenü und Taskleiste
prüfen, macOS separat im Finder, Dock und App-Umschalter. Native Windows-/macOS-
Pakete und Signierungen wurden hier nicht erzeugt oder geprüft.

## CJK-Schriftdarstellung, 31. Oktober 2026

Der gemeldete Beispieltitel „Bücher 日本語 📚“ ist in der synthetischen
BücherRegal-Datenbank vollständig gespeichert. Die drei Kästchen betreffen die
Darstellung. Alle fünf Apps verwenden jetzt eine app-lokal mitgelieferte,
explizit geladene Schrift Noto Sans CJK JP 2.004 mit Regular und Bold.
Die Änderung gilt für Tabellen, Eingaben und die übrige gestylte Oberfläche.
Gespeicherte Texte und Sprachwahl werden nicht umgeschrieben.

Die eingesehene native Linux-Schriftauswahl von OpenJFX 21.0.11 filtert bei der
Fallback-Ermittlung auf TrueType. Die hier installierten Noto-CJK-Schriften
verwenden CFF-Outlines. Die primäre, direkt geladene OTF-Schrift umgeht diesen
Fallback für die enthaltenen Zeichen. Andere Unicode-Zeichen und die Darstellung
von Emojis hängen weiterhin von der JavaFX-Ersatzschrift des Zielsystems ab.

Quelle: [OpenJFX 21.0.11, fontpath_linux.c](https://github.com/openjdk/jfx21u/blob/21.0.11%2B4/modules/javafx.graphics/src/main/native-font/fontpath_linux.c).
Die Einbindung nutzt die öffentliche API
[Font.loadFont](https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/text/Font.html).

Formatter und Java-Kompilierung mit Paketbau erfolgreich. Pro App wurde
`./mvnw spotless:apply package -Dmaven.test.skip=true` mit dem eigenen
Temurin 21.0.13+9 ausgeführt. Protokoll: `target/ui-font-build.log`.
Die vorhandenen Linux-App-Images wurden mit dem neuen App-JAR und den
zusätzlichen Lizenzhinweisen aktualisiert. Die Java-Laufzeit blieb unverändert.

Auf Nutzerwunsch keine Tests, kein `verify` und keine automatisierte GUI-Abnahme.
Manuell abzunehmen: den obigen Beispieltitel in Tabelle und Bearbeitungsdialog
lesen, zusätzlich Umlaute und Akzente in den sechs UI-Sprachen ansehen und
Dialoggrößen sowie fette Überschriften kontrollieren. In den anderen Apps dafür
nur eigene temporäre Beispieldaten verwenden. Windows und macOS bleiben offen.

## Datenschutz und lokale Paketvorbereitung, 31. Oktober 2026

Aus BücherRegal übernommen: Info-Menü, vollständige Offline-Datenschutzerklärung
in sechs Sprachen, größerer scrollbar-kopierbarer Dialog, Zentrierung nach nativen
Größenkorrekturen und verzögertes Einblenden. Die Texte beschreiben die tatsächlichen
Daten dieser App. Fotos bleiben lokale Datenbankkopien. Status und Arbeitsschritte
werden nur in den Apps mit diesen Funktionen genannt. KostümPlan beschreibt
zusätzlich seinen PDF-Export. Es wurde kein JSON-Import/-Export ergänzt.

Destruktive Schaltflächen haben einen roten Fokusrahmen. Die übrigen Aktionen
behalten die jeweilige App-Farbe. Die Auswahlfläche der Tabelle bleibt erhalten,
der zusätzliche Fokusrahmen der Tabellenzeile ist transparent.

Tatsächlich ausgeführt: Formatter, Java-Compiler und Linux-App-Image-Bau mit
Temurin 21.0.13+9. Tests waren durch `-Dmaven.test.skip=true` ausdrücklich
ausgeschaltet. Der Pakethelfer führte nur seinen Launcher-Versionsaufruf aus.
Der Datenschutztextexport erzeugte sechs Dateien aus den produktiven Ressourcen.
Keine Tests, kein `verify`, keine Fehleranalyse und kein GUI-Prüflauf dieses
Standes. Frühere Prüfergebnisse gelten ausschließlich für die damaligen Stände.

Buildprotokoll: `target/portfolio-privacy-build.log`.
Datenschutztexte: `build/releases/privacy/2026-10-31`.
Die Pakete enthalten die eigene Laufzeit, Lizenzhinweise, Bedienungsanleitung
und die originale Herkunftsdatei des Build-JDK. Die geänderten CI-Workflows und
native Windows-/macOS-Paketierung wurden nicht ausgeführt. Keine neuen Store-
Screenshots erzeugt. Frühere Lieferungen wurden nicht überschrieben.

Manuelle Abnahme durch den Nutzer bleibt offen:

1. Einen Eintrag wählen und mit Tab „Bearbeiten“ sowie „Löschen“ fokussieren.
   Nur der destruktive Fokusrahmen ist rot. Die Zeilenauswahl bleibt sichtbar.
2. Unter „Info“ die Erklärung öffnen. Positionierung und Einblenden beurteilen,
   scrollen, Text kopieren und über Escape sowie „Schließen“ zurückkehren.
3. Den Dialog nach einem Sprachwechsel und Neustart in den sechs Sprachen prüfen.
   Texte, Fotos und die vorhandenen Status-/Schrittdaten müssen erhalten bleiben.
4. Die bisherigen Kernfunktionen der App einschließlich Fotoauswahl und
   gegebenenfalls Status, Checkliste oder PDF manuell abnehmen.

Eine Store-Freigabe ist damit nicht belegt. Identität, MSIX, Signierung,
Installation, Updates, WACK und Windows-Aufnahmen bleiben offen.

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

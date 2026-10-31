# GarnBox

Deine Garne an einem Ort. Erfasse Bezeichnung, Marke oder Hersteller, Farbe,
Aufbewahrungsort und eine mehrzeilige Notiz. Ein optionales Foto hilft beim
Wiedererkennen. Alles bleibt lokal und nach einem Neustart erhalten.

![GarnBox mit synthetischen Testdaten](docs/screenshots/garnbox.png)

GarnBox ist Stufe 2 der Java-Reihe nach BücherRegal. Neu ist genau ein Foto pro
Eintrag. Status, Checklisten, Mengenverwaltung, PDF und Cloud gehören nicht dazu.

## Starten

Build und CI verwenden Eclipse Temurin 21.0.13+9. Maven prüft Hersteller,
Version und Buildnummer. Für direkte Maven-Aufrufe auf diesem Rechner:

```bash
export JAVA_HOME="$HOME/.local/share/garnbox/jdk-21.0.13+9"
export PATH="$JAVA_HOME/bin:$PATH"
```

Entwicklung mit einem vollständigen JDK 21. `JAVA_HOME` zeigt auf das JDK,
`PATH` enthält dessen `bin`-Verzeichnis. Der Maven Wrapper lädt Maven und die
Abhängigkeiten beim ersten Aufruf. Die fertige App arbeitet offline.

Linux und macOS aus `garnbox/`:

```bash
./mvnw javafx:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd javafx:run
```

Im vorhandenen Linux-Workspace genügt `./scripts/run.sh`. Der Helfer findet
bei fehlendem `JAVA_HOME` auch das JDK unter `$HOME/.local/share/garnbox/jdk-21.0.13+9`.
Das gebaute Linux-App-Image startet ohne separat installiertes Java:

```bash
./target/app-image/GarnBox/bin/GarnBox
```

## Bedienung

„Garn hinzufügen“ öffnet denselben Dialog wie „Bearbeiten“. Nur die Bezeichnung
ist erforderlich. Gleichnamige Einträge sind unabhängig. Die Suche berücksichtigt
alle fünf Textfelder, Groß- und Kleinschreibung sowie Umlaute. Eine leere Suche
zeigt die ganze Sammlung. Löschen fragt vorher nach.

Das ausgewählte Foto wird als eigene Kopie in der lokalen SQLite-Datenbank
abgelegt. Verschieben oder Löschen des Originals beeinflusst diese Kopie nicht.
JPEG und PNG werden anhand des Bildinhalts geprüft. Erlaubt sind maximal
10 MiB (10.485.760 Bytes), 6000 Pixel je Seite und 16 Millionen Pixel insgesamt.
Ein zu großes oder ungültiges Foto verändert den bisherigen Entwurf nicht.

Abbrechen der Dateiauswahl verändert nichts. Abbrechen des Eingabedialogs,
Escape und Fensterschließen fragen bei ungespeicherten Änderungen nach.
Nach Speicherfehlern bleiben Texte und gewählte Vorschau zum erneuten Speichern
erhalten. Während Bildprüfung und Speichern wartet der Dialog auf den Abschluss.

| Taste | Aktion |
| --- | --- |
| Strg+N / Cmd+N | Garn hinzufügen |
| Strg+F / Cmd+F | Suche auswählen |
| Enter oder Doppelklick in der Liste | Ausgewählten Eintrag bearbeiten |
| Entf in der Liste | Nach Bestätigung löschen |
| Strg+S / Cmd+S | Dialog speichern |
| Escape | Dialog abbrechen, gegebenenfalls mit Rückfrage |
| Enter im Notizfeld | Neue Zeile |
| Tab / Umschalt+Tab | Steuerelemente wechseln |

## Sprachen und Store-Vorschau

Die Oberfläche ist auf Deutsch, Englisch, Spanisch, Französisch, Italienisch
und Portugiesisch (Brasilien) verfügbar. Über „Sprache“ eine feste Sprache oder
die Systemsprache wählen und GarnBox neu starten. Die Wahl steht in
`language.txt` im Datenordner. Garne und Fotos bleiben dabei unverändert.
Nicht unterstützte Sprachen verwenden Englisch.

Die [Internationalisierungsanleitung](docs/internationalization.md) beschreibt
die sechs Store-Textfassungen, eigene Icons und 24 echte Linux-Vorschau-Bilder.
Die Lieferung erfolgt als Ordner ohne ZIP. Die native Windows-Abnahme folgt
separat. Die Dateiauswahl verwendet teilweise Beschriftungen des Betriebssystems.

## Datenschutz und Fokusrahmen

„Info“ öffnet die vollständige Datenschutzerklärung offline in der ausgewählten
App-Sprache. Der Dialog ist scrollbar, schreibgeschützt und kopierbar.
Escape oder „Schließen“ führen zurück. Er wird erst nach der Positionierung
sichtbar, damit die anfängliche Korrektur der Fensterposition verborgen bleibt.
Die Erklärung beschreibt die tatsächlich gespeicherten Texte, Fotos und die
jeweiligen Projektfunktionen. Originalfotos werden nicht verändert.

Löschschaltflächen erhalten einen roten Fokusrahmen. Die übrigen Schaltflächen
behalten die jeweilige App-Farbe. Die Auswahlfläche der Tabelle bleibt sichtbar,
der zusätzliche Rahmen der fokussierten Tabellenzeile ist transparent.

Die sechs identischen Texte für Partner Center lassen sich aus den Ressourcen
in einen neuen Ausgabeordner exportieren:

```bash
./scripts/export_privacy.sh target/privacy-texts
```

Ein vorhandener Ausgabeordner wird nicht überschrieben. Die vorhandenen
Prüfbedingungen für Vorschau-Lieferungen bleiben bestehen. Der neue Datenschutz-
ordner wird erst mit einer erneut geprüften Lieferung erzeugt.

## Daten und Tests

App-Kennung: `de.appfabrik.garnbox`. Die Datei `garnbox.db` enthält Texte und Fotos.

| System | Benutzerspezifisches Datenverzeichnis |
| --- | --- |
| Windows | `%LOCALAPPDATA%\GarnBox`, sonst `~/AppData/Local/GarnBox` |
| macOS | `~/Library/Application Support/GarnBox` |
| Linux | `$XDG_DATA_HOME/garnbox`, sonst `~/.local/share/garnbox` |

Zum Sichern die App schließen und `garnbox.db` kopieren. Die Datenbank wächst
mit den Fotos. Entfernte Bilder können intern freien Platz hinterlassen, den
SQLite wiederverwendet. Eine sofortige Verkleinerung der Datei ist nicht zugesagt.
Mehrere gleichzeitig bearbeitende App-Instanzen werden nicht unterstützt.

Isolierter Teststart ohne Zugriff auf die normale Sammlung:

```bash
./scripts/run.sh '-Djavafx.args=--data-dir=/tmp/garnbox-test'
./target/app-image/GarnBox/bin/GarnBox --data-dir=/tmp/garnbox-test
```

```bash
./mvnw spotless:apply
./mvnw verify
```

Die Tests erzeugen eigene temporäre SQLite-Dateien und synthetische Bilder.
`verify` umfasst Compiler-Lint, Enforcer, JUnit, Spotless und SpotBugs.

## Technik und Auslieferung

Java 21, JavaFX 21.0.11, SQLite JDBC 3.53.4.0 und Maven Wrapper 3.9.16.
Die [Architektur](docs/architecture.md) erklärt „Eintrag mit Foto speichern“ und
die zusätzlichen Java-Kenntnisse gegenüber BücherRegal.

Die [Paketierung](docs/packaging.md) bereitet Windows-EXE und macOS-DMG mit eigener
Laufzeit vor. Der [Prüfbericht](docs/verification.md) dokumentiert die tatsächlich
geprüfte Linux-Oberfläche und das startbare Paket. Native Windows- und macOS-
Abnahmen sowie Store-Schritte sind offen. Es wurde nichts veröffentlicht.

Hinweise zu Fremdkomponenten: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

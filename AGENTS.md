# GarnBox

Eigenständiges Java-Projekt mit eigenem Git-Repository. Die übergeordneten
Workspace-Regeln gelten ergänzend. BücherRegal bleibt unverändert.

## Umfang und Architektur

Stufe 2 des Hobby-Portfolios. Garne mit Bezeichnung, Marke, Farbe, Ort, Notiz
und genau einem optionalen Foto lokal verwalten. Am 31. Oktober 2026 wurden
zusätzlich ein Farbwähler sowie JSON-Export und JSON-Import der Sammlung beauftragt.
Neue Farbauswahlen als Hexwerte speichern. Bestehende Farbbeschreibungen erst
bei einer bewussten Auswahl oder beim Entfernen ersetzen. Der Export umfasst
alle gespeicherten Einträge und Fotos, unabhängig von Suche und Auswahl.
Importe ergänzen atomar und bewahren vorhandene Einträge. Exakt identische
Kopien anhand aller Texte und Fotobytes unter Berücksichtigung ihrer Anzahl
überspringen. Beschädigte Dateien oder Fotos dürfen keine Teilimporte hinterlassen.
Java 21, JavaFX 21.0.11, SQLite JDBC und ein Maven-Modul. Pakete `domain`,
`application`, `persistence` und `ui`. Keine gemeinsame Runtime mit anderen Apps.
Java-Bezeichner und GitHub-Repository-Metadaten sind englisch. Die Oberfläche
unterstützt de-DE, en-US, es-ES, fr-FR, it-IT und pt-BR. Projektdokumentation ist
deutsch. Eigene UI-Texte gehören in die app-lokalen UTF-8-Sprachkataloge.
Fehler aus Anwendung und Persistenz verwenden stabile Codes, die UI übersetzt sie.
Sprachwahl gilt ab Neustart und darf weder Textdaten noch Foto-BLOBs ändern.

Internationalisierungs-Vorschauen gemäß `docs/internationalization.md` als
Ordner liefern. Keine ZIP-Datei ohne ausdrücklichen Nutzerauftrag. Aufnahmen
stammen aus der echten App mit synthetischen Daten auf einem eigenen Xvfb.
Linux-Vorschauen nicht als geprüfte Windows-Store-Bilder bezeichnen.

## Tatsächlich verwendete Befehle

Aus diesem App-Verzeichnis mit vollständigem JDK 21:

```bash
./mvnw spotless:apply
./mvnw verify
./mvnw javafx:run
./scripts/run.sh
./scripts/package.sh app-image
./target/app-image/GarnBox/bin/GarnBox
bash -n scripts/run.sh scripts/package.sh
git diff --cached --check
```

`run.sh` und `package.sh` verwenden bei fehlendem `JAVA_HOME` auch ein vorhandenes
JDK unter `$HOME/.local/share/garnbox/jdk-21.0.13+9`. Für direkte Wrapper-Aufrufe `JAVA_HOME` und `PATH`
entsprechend setzen. Auf Windows `mvnw.cmd` verwenden.

### App aus einer Agentensitzung öffnen

Unter Ubuntu die vorhandene App unabhängig vom kurzlebigen Tool-Prozess über
den Benutzer-Service-Manager starten. Ein direkter `gtk-launch`-Aufruf aus einem
abgeschlossenen Tool-Befehl ließ das Fenster am 31. Oktober 2026 wieder
verschwinden. Vor einem neuen Start nach einem vorhandenen Fenster mit der
Klasse `de.appfabrik.garnbox.ui.YarnsApplication` suchen und es gegebenenfalls
nur aktivieren. Keine zweite Instanz derselben Sammlung starten.

Aus diesem App-Verzeichnis bei vorhandener X11-Desktop-Sitzung:

```bash
systemd-run --user --collect --property=Type=exec \
  --setenv="DISPLAY=$DISPLAY" --setenv="XAUTHORITY=$XAUTHORITY" \
  "$PWD/target/app-image/GarnBox/bin/GarnBox"
```

Den zurückgegebenen Unit-Namen für `systemctl --user status` und
`journalctl --user -u` verwenden. Erst in einem späteren Tool-Aufruf prüfen,
ob Prozess und Fenster weiterhin bestehen. Ein unmittelbar sichtbares Fenster
allein belegt keinen dauerhaften Start. Dafür ist kein Neubau erforderlich.

Vorbereitete native Wege, hier noch nicht ausgeführt:

```powershell
.\mvnw.cmd verify
.\scripts\package.ps1 -PackageType exe
```

```bash
./scripts/package.sh dmg
```

## Prüf- und Speicherregeln

`verify` umfasst Tests, Compiler-Lint mit Warnungen als Fehlern, Enforcer,
Spotless und SpotBugs. Temporäre SQLite-Dateien und generierte Testbilder
verwenden. GUI-Abläufe mit eigenem `--data-dir` gemäß `docs/verification.md` prüfen.
Ein Versionsaufruf ersetzt keine gestartete GUI mit SQLite- und Fotozugriff.

Bilddateien werden in `PhotoImporter` anhand ihres Inhalts vollständig decodiert.
Grenzen: 10 MiB, 6000 Pixel je Seite, 16 Millionen Pixel. Datenbank und Bildladen
bleiben außerhalb des JavaFX-Threads. Texte und BLOB werden gemeinsam atomar
geändert. Keine Originaldatei verändern oder löschen.

Ein Dialog schließt erst nach erfolgreichem Speichern. Alle Abbruchwege schützen
Text- und Fotoänderungen. Fehler erhalten den Entwurf. Vorschau-Callbacks dürfen
keine inzwischen gewechselte Auswahl überschreiben.

Vor dem lokalen Commit Formatter, `verify` und erforderliche GUI-Prüfungen
abschließen. Changelog unter `CODEX/changelog` im Workspace ergänzen.
Keine Datenbanken, Build-Ausgaben, Testdatenverzeichnisse oder Secrets committen.
Kein Push und keine Veröffentlichung ohne ausdrücklichen Auftrag.

## Datenschutz und lokale Release-Vorbereitung

Build und CI sind auf Eclipse Temurin 21.0.13+9 festgelegt. Eigene Installation
je App verwenden. Datenschutzdialog und Store-Textexport verwenden dieselben
sechs Ressourcen. Foto-, Status-, Checklisten- und PDF-Aussagen müssen dem
Funktionsumfang dieser App entsprechen. Der ausdrücklich beauftragte Transfer
verwendet das app-lokale Format `yarn-box.collection`. Weitere BücherRegal-
Transferfunktionen gehören nicht automatisch zum Umfang dieser App.

Der Dialog verwendet die tatsächlichen Fenstermaße nach nativen Größenkorrekturen
und wird erst nach der Positionierung sichtbar. Destruktive Schaltflächen tragen
die CSS-Klasse `danger` und einen roten Fokusrahmen.

Microsoft Store: MSIX. Kostenloser GitHub-Download: EXE. Identitäten, Signierung,
Windows-Abnahme und Veröffentlichung bleiben separate Release-Schritte.

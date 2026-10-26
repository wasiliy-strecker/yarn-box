# GarnBox

Eigenständiges Java-Projekt mit eigenem Git-Repository. Die übergeordneten
Workspace-Regeln gelten ergänzend. BücherRegal bleibt unverändert.

## Umfang und Architektur

Stufe 2 des Hobby-Portfolios. Garne mit Bezeichnung, Marke, Farbe, Ort, Notiz
und genau einem optionalen Foto lokal verwalten. Keine weiteren Hauptfunktionen.
Java 21, JavaFX 21.0.11, SQLite JDBC und ein Maven-Modul. Pakete `domain`,
`application`, `persistence` und `ui`. Keine gemeinsame Runtime mit anderen Apps.
Java-Bezeichner sind englisch, Oberfläche und Projektdokumentation deutsch.

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
JDK unter `$HOME/.local/jdk-21`. Für direkte Wrapper-Aufrufe `JAVA_HOME` und `PATH`
entsprechend setzen. Auf Windows `mvnw.cmd` verwenden.

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

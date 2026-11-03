# Internationalisierung und Linux-Vorschau

Yarn Box unterstützt de-DE, en-US, es-ES, fr-FR, it-IT und pt-BR. Der Umfang bleibt
Stufe 2 mit fünf Textfeldern und genau einem optionalen Foto pro Eintrag.
Der öffentliche Produktname ist in jeder Sprache Yarn Box. App-Kennung,
Datenpfade und SQLite-Schema bleiben erhalten.

## Sprachwahl und Daten

Ohne gespeicherte Sprachwahl startet die App auf Englisch. Die deutsche
Store-Erstveröffentlichung bestimmt ausschließlich die Store-Texte und Release-
Hinweise. Vorhandene Sprachwahlen werden erhalten.

Die Schaltfläche „Sprache“ bietet die sechs Sprachen und die automatische Wahl
nach Systemsprache. Die Einstellung steht in `language.txt` neben `garnbox.db`
und wird beim nächsten Start angewandt. Nicht unterstützte Sprachen und
unbekannte gespeicherte Werte verwenden Englisch. Ein Lesefehler verwendet die
englische Sprache und zeigt einen Hinweis. Fehler beim Speichern lassen
den Dialog mit seiner Auswahl offen. Der Austausch der Einstellungsdatei ist
atomar. Die Datenbank wird dabei nicht geändert.

Die sechs UTF-8-Kataloge werden auf vollständige, passende Schlüssel geprüft. Eigene Dialoge,
Validierungen, Foto- und Speicherfehler, Mengenangaben und Text-Kontextmenüs
werden übersetzt. Die Sortierung verwendet die gewählte Sprache. Namen, Notizen
und Foto-BLOBs werden unverändert gespeichert. Titel und Dateifilter der nativen
Fotoauswahl sind übersetzt. Deren Betriebssystem-Schaltflächen folgen den
Systemeinstellungen und müssen auf Windows separat geprüft werden.

## Reproduzierbare GUI-Prüfung

Benötigt werden ein vollständiges JDK 21, Python 3, Xvfb und `xdotool`.
`YARNBOX_XVFB` kann auf ein lokal entpacktes Xvfb-Programm zeigen.
Der Helfer verwendet einen eigenen virtuellen Bildschirm mit 1920 × 1080 Pixeln
und keinen vorhandenen Desktop. Temporäre Datenverzeichnisse unter
`target/gui-work/` werden ausdrücklich als synthetische Daten markiert.

```bash
export JAVA_HOME="$HOME/.local/share/garnbox/jdk-21.0.13+9"
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw spotless:apply
./mvnw verify dependency:build-classpath -Dmdep.outputFile=target/gui-classpath.txt
python3 scripts/store_assets/run_gui.py --output target/gui-international
```

Für gezielte Entwicklung sind `--locales fr-FR` und `--mode capture` oder
`--mode verify` verfügbar. Eine Lieferung benötigt alle sechs Sprachen und den
Standardmodus `all`. Der Ausgabeordner muss neu sein.

`--mode startup --locales en-US` prüft separat einen Erststart ohne Sprachdatei
auf einem simulierten deutschen System. Die Oberfläche muss Englisch verwenden.

Je Sprache startet `GuiJourney` die produktive `YarnsApplication` in drei neuen
JVM-Prozessen. Die Bedienung erfolgt mit echten Maus- und Tastaturereignissen.
JavaFX Robot nimmt den Bildschirm unverändert auf. Die native Dateiauswahl wird
mit X11-Eingaben bedient. Es gibt keine nachgebaute Oberfläche oder Bildmontage.

Vier Aufnahmen pro Sprache zeigen die Übersicht, den Foto-Bearbeitungsdialog,
ein lokalisiertes Suchergebnis und die Sprachwahl. Die fünf Beispieldatensätze je
Sprache und drei Garnillustrationen sind frei erfunden. Sie werden nicht beim
normalen Start in eine Benutzersammlung eingefügt.

Die GUI-Prüfung umfasst Pflichtfeldhinweise, Unicode, lange Notizen, sicheren
Abbruch, PNG-Import, abgelehnte ungültige Dateien, JPEG-Ersatz, eine echte
exklusive SQLite-Schreibsperre und erneutes Speichern. Entfernen, Verwerfen und
Speichern eines Fotos werden über die Oberfläche geprüft und zusätzlich in
SQLite kontrolliert. Kleine Haupt- und Dialogfenster, abgebrochenes Löschen,
fehlgeschlagene Spracheinstellung und erfolgreicher Sprachwechsel werden geprüft.
Ein weiterer Prozess bestätigt die neue Sprache, alle Textdaten und die exakten
Foto-Bytes. Die abweichende Ausgangssystemsprache Japanisch prüft den Vorrang
der expliziten Wahl.

## Paketierter Launcher

```bash
./scripts/package.sh app-image
python3 scripts/store_assets/packaged_smoke.py --output target/packaged-international
```

Zusätzlich werden ImageMagick, `xsel` und Python Pillow benötigt. Der echte Launcher startet
außerhalb des Projekts mit eigener JVM, entferntem `JAVA_HOME` und
`PATH=/nonexistent`. Eine vorhandene synthetische Schema-1-Sammlung enthält
1234 Einträge. Der Ablauf prüft französischen Start, Unicode-Bearbeitung, die
native JPEG-Auswahl, atomare Speicherung, Sprachwechsel und englischen Neustart
nach Löschen der importierten Bildquelle. Screenshots dienen der Sichtprüfung
von Zahlengruppierung, übersetzten Dialogen und Fotodarstellung. Datei-Hashes
belegen, welche Laufzeit geprüft wurde und dass sie dabei unverändert blieb.

## Ordner ohne ZIP ausliefern

Alle Screenshots visuell prüfen, die Dokumentation ergänzen und den lokalen
App-Commit erstellen. Danach bei sauberem App-Arbeitsbaum ausführen:

```bash
python3 scripts/store_assets/assemble.py target/gui-international \
  build/releases/international/garnbox-linux-preview-YYYYMMDD-HHMM \
  --packaged-checks target/packaged-international
```

Der Helfer erstellt ausschließlich einen neuen Ordner. Er prüft die Quellhashes,
alle 18 GUI-Prozesse, PNG-Abmessungen, Datei-Hashes der GUI-Nachweise und die
geprüfte Paketlaufzeit. Bestehende Lieferungen werden nicht überschrieben.
`Store/<Sprache>/text` enthält direkt kopierbare Textdateien ohne Listenmarker.
`Store/<Sprache>/screenshots` enthält vier Bilder. `Branding` enthält Icons und
`Verification` die Prüfnachweise. Das Manifest nennt den tatsächlich geprüften
lokalen Commit. `SHA256SUMS.txt` enthält die Hashes aller gelieferten Dateien.
Quellcode, Store-Texte, Werkzeuge und Assets dürfen nach der Aufnahme nicht mehr
geändert werden. Dokumentation kann vor dem Commit ergänzt werden.

## Originale Vektorgrafiken

Das Icon zeigt ein Garnknäuel und Stricknadeln. Drei weitere SVG-Zeichnungen
zeigen Garn und Maschenproben. Es sind eigene Illustrationen ohne Stockfotos
oder fremde Marken. PNG und JPEG werden aus diesen Quellen gerendert.
Die eingebundenen Dateien benötigen zur Laufzeit keine Grafikwerkzeuge.

```bash
/usr/bin/python3 scripts/store_assets/branding.py
```

Das erneute Rendern benötigt PyGObject, Cairo, librsvg und ImageMagick.
Das 256-Pixel-PNG ist das Fenster- und Linux-Paketicon. Für Windows ist ein
mehrstufiges ICO im PowerShell-Pakethelfer eingebunden. Die weiteren PNGs
umfassen 44, 50, 150, 300 und 1024 Pixel. Auswahl und Zuordnung der endgültigen
Windows-Paketressourcen gehören zur nativen Paketierung.

## Windows noch offen

Die 24 Bilder sind echte Linux-Vorschauen. Windows-Paketierung, Installation,
Updateprüfung, Skalierung, native Dateiauswahl und endgültige Windows-Bilder
stehen noch aus. Produktidentität, Signierung und Einreichung im Microsoft Store
gehören zum späteren Releaseauftrag. Dieser Ordner ist keine Store-Abnahme.

## Datenschutzübernahme vom 31. Oktober 2026

Seit dem 3. November 2026 führt „Info“ → „Datenschutz“ zur eigenen englischen
`PRIVACY.md` auf GitHub. Menü und Browserfehlermeldung bleiben in sechs Sprachen
verfügbar. `assemble.py` kopiert die kanonische Datei unverändert zusammen mit
`privacy-url.txt` in den Ordner `Privacy`. Frühere Vorschauordner behalten ihren damaligen
Quellstand. Für die aktuelle UI wurden auf Nutzerwunsch keine neuen Aufnahmen
oder GUI-Prüfungen erzeugt. Neue Bilder und native Abnahme bleiben offen.

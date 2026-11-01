# Prüfbericht

## Mac-Beenden, 1. November 2026

Cmd+Q kann für Hauptfenster und Editor mehrere Schließanfragen auslösen.
Diese werden vor der Rückfrage gebündelt. „Weiterbearbeiten“ erhält den gesamten
Entwurf und verwirft auch den ausstehenden Beenden-Wunsch. „Verwerfen“ schließt
Editor und Hauptfenster. Ein unveränderter Editor benötigt keine Rückfrage.
Das alleinige Schließen des Editors lässt das Hauptfenster geöffnet. Laufendes
Speichern wird nicht unterbrochen. Implementierung und Tests bleiben app-lokal.

Der aktuelle Nutzerauftrag umfasst ausdrücklich die zuvor gefundenen Fehler.
Die frühere Testzurückstellung einzelner Aufgaben gilt nicht für diese Korrektur.
Mit Eclipse Temurin 21.0.13+9 auf einem eigenen Xvfb-/Openbox-Bildschirm ausgeführt:

```bash
./mvnw --batch-mode --no-transfer-progress spotless:apply verify -Dgarnbox.uiTests=true
```

98 Tests bestanden. Keine Fehler und keine übersprungenen Tests.
Compiler-Lint, Enforcer, Spotless und SpotBugs bestanden. Protokoll:
`target/mac-workflows-verify.log`. Sechs neue Beenden-Regressionen rufen den
JavaFX-Glass-Handler auf, den macOS für Cmd+Q verwendet. Sie prüfen Verwerfen,
Weiterbearbeiten, wiederholte Anfragen, unveränderte Entwürfe, das einzelne
Editorfenster und einen gezielt verzögerten Speichervorgang.

Manuelle Mac-Abnahme bleibt erforderlich: Cmd+Q mit offenem Entwurf abbrechen
und bestätigen. Während eines Speichervorgangs erneut versuchen.
Linux-Prüfungen sind keine native Mac- oder Sandbox-Abnahme. Keine Store-Pakete, Veröffentlichung oder Änderung
persönlicher Sammlungen. Bereits laufende Benutzerprozesse wurden nicht beendet.

## Vollständiger Custom-Color-Dialog, 31. Oktober 2026

Der native JavaFX-Farbdialog verwendet jetzt 50 × 34 em bevorzugte Inhaltsgröße,
bei der App-Schrift 700 × 476 Pixel. Der rechte Bedienbereich erhält mindestens
26 em für vollständige übersetzte Beschriftungen. Das Fenster ist vergrößerbar.
Die Mindestgröße berücksichtigt nach dem Layout die tatsächliche Titelleiste
und den Fensterrahmen. Der Dialog wird über dem Editor innerhalb des Bildschirms
positioniert und erst anschließend eingeblendet. Die Umsetzung verwendet
öffentliche JavaFX-APIs und bleibt vollständig app-lokal.

Geprüft mit `./mvnw --batch-mode --no-transfer-progress spotless:apply verify`.
92 gemeldete Tests, davon 0 übersprungen, keine Fehler. Compiler-Lint,
Enforcer, Spotless und SpotBugs bestanden. Protokoll:
`target/color-dialog-verify.log`.

Ein separater temporärer GUI-Prüfhelfer verwendet das echte Farbfeld und den
nativen Dialog ohne Sammlung oder Datenbank. Auf einem eigenen Xvfb mit Openbox
bei 800 × 600 Pixeln wurden en-US, de-DE, es-ES, fr-FR, it-IT und pt-BR geprüft.
Alle 272 Prüfbedingungen je Sprache bestanden. HSB, RGB und Web, vollständig
sichtbare Schaltflächen und Beschriftungen, Verkleinerung auf die Mindestgröße,
Speichern, Verwenden, Abbrechen, Escape, Fensterschließen und erneutes Öffnen.
Protokoll: `target/color-dialog-gui.log`. Synthetische Aufnahmen normaler und
minimaler Fenstergröße: `target/custom-color-review/`. Repräsentative Aufnahmen
wurden zusätzlich visuell geprüft.

Manuelle Wiederholungsprüfung: Ein Farbfeld öffnen, „Custom Color“ wählen und
alle drei Register durchgehen. Die unteren Aktionen müssen vollständig sichtbar
sein. Die Fenstergröße bis zur Mindestgröße ändern und erneut prüfen. Eine
andere Farbe wählen und jeweils Verwenden, Speichern sowie die Abbruchwege
testen. Der Farbfeldwert darf sich nur beim Übernehmen ändern. Den Dialog
zwischen den Schritten erneut öffnen.

Das vorhandene lokale Linux-App-Image mit dem geprüften App-JAR aktualisiert.
JAR-Hashes stimmen überein. Der eingebettete Launcher meldet
`Yarn Box 0.1.0`. Native Windows- und macOS-Abnahmen sind separat.


## Korrekturen nach Release-Prüfung, 31. Oktober 2026

Die gemeldeten Sicherungsfehler, veralteten Tests, der schlecht lesbare Farbcode
und die Sprach- und Namensvorgaben sind korrigiert. Öffentlicher Name ist
**Yarn Box**. Neue Installationen starten auf Englisch, auch bei deutschem
Betriebssystem. Gespeicherte Sprachwahlen bleiben erhalten. Die erste Store-
Veröffentlichung verwendet deutsche Texte und de-DE als primäre Store-Sprache.
Diese Festlegung ändert weder Paket-ID noch Datenpfade.

Neuanlagen und ergänzende Importe begrenzen die Sammlung atomar auf 10.000
Einträge. Identische Kopien können bei voller Sammlung übersprungen werden.
Größere Altbestände bleiben erhalten, lesbar, bearbeitbar und löschbar. Neue
oder bearbeitete Textfelder sind auf 1.048.576 Zeichen begrenzt. Der Export
prüft Einträge, Texte, Fotobytes und die tatsächliche UTF-8-Dateigröße bis
256 MiB vor Veröffentlichung. Ein Fehler hinterlässt keine Teilsicherung und
überschreibt keine vorhandene Datei. Die UI erklärt überschrittene Grenzen.

| Prüfung | Ergebnis |
| --- | --- |
| `./mvnw --batch-mode --no-transfer-progress spotless:apply verify dependency:build-classpath -Dmdep.outputFile=target/gui-classpath.txt` mit Temurin 21.0.13+9 | 92 Tests ohne Fehler oder ausgelassene Fälle. Compiler-Lint, Enforcer, Spotless und SpotBugs bestanden |
| Sammlungstransfer | 10.000 Einträge und maximale Textlänge wiederhergestellt. Gleichzeitige Neuanlagen, Import-Rollback, Kopienanzahl, Altbestände, exakte UTF-8-Dateigrenze und Erhalt vorhandener Exportdateien geprüft |
| Fotos | Optimierung, Transparenz, alle acht EXIF-Ausrichtungen, unveränderte Originale und bytegenauer Wiederimport geprüft |
| Isolierte GUI in sechs Sprachen | 18 Prozesse mit 444 Prüfbedingungen bestanden. Farbwähler, Speicherung, Fehlerbehandlung, Sprachwechsel und Neustart geprüft |
| Englischer Erststart | Drei Prüfbedingungen bei simulierter deutscher Systemsprache und fehlender Spracheinstellung bestanden |
| Lokales Linux-App-Image | Neu gebaut. Echter Launcher mit gebündeltem Java bei `PATH=/nonexistent`, native Fotoauswahl, Unicode-Speicherung und englischer Neustart nach Entfernung der Bildquelle bestanden |
| Sichtprüfung und Skriptsyntax | Dunkler Farbcode auf ausgewählter Zeile sowie englischer Name kontrolliert. Shell-/Python-Syntax und `git diff --check` bestanden |

Nachweise: `target/release-fixes-verify.log`, `target/release-fixes-gui`,
`target/release-fixes-startup`, `target/release-fixes-package.log` und
`target/release-fixes-packaged-2`. Alle Testdaten sind synthetisch und isoliert.
Der Ubuntu-Anwendungseintrag startet das neue App-Image unter
`target/app-image/Yarn Box`. Bereits laufende Nutzerfenster und gespeicherte
Sammlungen wurden nicht verändert. Frühere Lieferordner bleiben erhalten.

Die nachfolgenden Abschnitte dokumentieren ältere Stände. Die jetzt ausgeführten
Prüfungen decken auch die zuvor ungeprüften Fotoänderungen ab. Native Windows-
und macOS-Abnahme, Signierung und Store-Auslieferung bleiben separate Schritte.
Kein Push, Upload oder Store-Release in diesem Auftrag.

## Schaltfläche zum Neuladen entfernt, 31. Oktober 2026

Die Fußleiste enthält nur noch Info, Sprache, Exportieren und Importieren.
Die Schaltfläche „Neu laden“ sowie ihre Bindungen und ungenutzten Übersetzungen
wurden entfernt. Fehlermeldungen in allen sechs Sprachen verweisen bei Bedarf
auf einen App-Neustart. Das Laden beim Start und die automatische Aktualisierung
nach Speichern, Löschen und Import bleiben erhalten.

Formatter, Compiler-Lint, Enforcer, Paketbau und SpotBugs bestanden mit
`./mvnw --no-transfer-progress spotless:apply package spotbugs:check -Dmaven.test.skip=true`.
Protokoll: `target/remove-reload-build.log`. Auf ausdrücklichen Nutzerwunsch
keine Tests, kein `verify` und keine automatisierte GUI-Prüfung.

Manuelle Abnahme durch den Nutzer: Die Fußleiste ohne Neuladen-Schaltfläche
ansehen. Ein Garn hinzufügen, bearbeiten und löschen sowie einen Export
importieren. Die Liste soll die Änderungen jeweils sofort anzeigen.
Diese Abläufe wurden hier nicht ausgeführt.

## Neue Fotos automatisch verkleinern, 31. Oktober 2026

Die Dateiauswahl speichert neue oder ersetzte Fotos mit höchstens 1600 Pixeln
an der längsten Seite. Seitenverhältnis und EXIF-Ausrichtung bleiben erhalten.
Kleine Bilder werden nicht vergrößert. Bilder ohne transparente Pixel werden
als JPEG mit Qualität 0,85 gespeichert, transparente Bilder als PNG.
Quellmetadaten werden nicht übernommen. Die Originaldateien bleiben erhalten.
Datenbanklesen und JSON-Import validieren weiterhin ohne erneute Codierung.
Der Export enthält die gespeicherte Kopie als Base64. Keine Datenmigration.

Auf ausdrücklichen Nutzerwunsch keine Tests, kein `verify`, keine automatisierte
GUI-Prüfung und keine Bildschirmaufnahmen. Formatter, Compiler-Lint, Enforcer,
Paketbau und statische SpotBugs-Analyse bestanden mit:

```bash
./mvnw --no-transfer-progress spotless:apply package spotbugs:check -Dmaven.test.skip=true
```

Protokoll: `target/photo-optimization-build.log`. Die statische Analyse meldete
zunächst bereits vorhandene Pfad- und Injektionshinweise im Fotoordner- und
Sammlungstransfercode. Der Datenordner wird nun direkt vorgehalten, die
Exportpfade werden über geprüfte lokale Variablen verwendet. Für bewusst
geteilte Transfer-Ports gilt dieselbe dokumentierte Konstruktor-Ausnahme wie
für `YarnService`. Der erneute Lauf meldete keine Warnungen oder Fehler.
Das vorhandene Linux-App-Image erhielt das neue JAR und wurde für die eigene
Abnahme über den Benutzer-Service-Manager geöffnet.

Manuelle Abnahme durch den Nutzer, noch nicht ausgeführt:

- Ein Foto über 1600 Pixel innerhalb der Eingabegrenzen hinzufügen, speichern
  und exportieren. Die aus `photoBase64` decodierte Kopie darf an keiner Seite
  größer als 1600 Pixel sein und muss die Proportionen bewahren.
- Ein kleines Bild auswählen. Seine Pixelmaße dürfen nicht wachsen.
- Ein transparentes PNG auswählen. Die gespeicherte Kopie muss PNG mit
  Transparenz bleiben. Ein opakes PNG soll als JPEG gespeichert werden.
- Handyfotos mit EXIF-Drehung und Spiegelung auswählen. Vorschau und exportierte
  Kopie sollen ohne EXIF-Daten die richtige Ausrichtung zeigen.
- Originaldateien vergleichen. Inhalt und Pixelmaße dürfen sich nicht ändern.
- Bei einem vorhandenen Foto nur den Text bearbeiten. Die gespeicherten
  Fotobytes müssen gleich bleiben. Alte und neue Exporte wiederholt importieren.
  Es darf keine erneute Fotokomprimierung oder zusätzliche identische Kopie geben.
- Fehlerhafte oder zu große Dateien auswählen und einen Fotoentwurf abbrechen.
  Der vorhandene gespeicherte Eintrag muss erhalten bleiben.

## Sammlung importieren, 31. Oktober 2026

Eine sichtbare Schaltfläche „Importieren“ neben „Exportieren“ ergänzt.
Vorhandene Yarn-Box-Exporte im Format `yarn-box.collection`, Version 1,
werden mit Texten, Farbwerten und Fotos eingelesen. Bestehende Einträge bleiben
erhalten. Exakt identische Kopien werden unter Berücksichtigung ihrer Anzahl
übersprungen. Die gesamte Datei wird innerhalb einer SQLite-Transaktion
verarbeitet. Ungültige Daten oder Fotos führen zum vollständigen Rollback.

Auf ausdrücklichen Nutzerwunsch keine Tests, kein `verify`, keine automatisierte
GUI-Prüfung und keine Bildschirmaufnahmen. Nur Formatter, Paketbau mit
`-Dmaven.test.skip=true` und Bereitstellung der benötigten Laufzeitbibliotheken.
Protokoll: `target/collection-import-build.log`. Gson 2.13.2 und Error Prone
Annotations 2.41.0 werden mit ihren Lizenzhinweisen ins App-Image übernommen.

Manuelle Abnahme durch den Nutzer: Einen vorhandenen Export importieren und
Texte, Farben und Fotos ansehen. Dieselbe Datei erneut importieren und die
gemeldeten übersprungenen Einträge kontrollieren. Gleichnamige Einträge mit
verschiedenen Inhalten müssen getrennt bleiben. Ein Export mit mehreren
identischen Einträgen muss deren Anzahl erhalten. Eine beschädigte JSON-Datei
oder ein ungültiges Foto darf keine Teileinträge hinterlassen. Abbrechen der
Dateiauswahl lässt die Sammlung unverändert. Import und Neustart sollen die
ergänzten Daten erhalten. Keine dieser Abläufe wurde hier ausgeführt.

## Zuletzt gewählter Bilderordner, 31. Oktober 2026

Die Fotoauswahl verwendet eine appweit geteilte, lokal gespeicherte
Ordnereinstellung. Nach einer bestätigten Dateiauswahl wird der übergeordnete
Ordner im Hintergrund gespeichert. Auch eine später abgelehnte Bilddatei
ändert diese reine Navigationseinstellung. Abbrechen der Dateiauswahl ändert
sie nicht. Fehlende oder unlesbare Ordner führen zum Bilder- beziehungsweise
Benutzerordner. Die Einstellung ist durch `--data-dir` von anderen Sammlungen
isoliert. Ein Schreibfehler verhindert den Fotoimport nicht und wird gemeldet.

Auf ausdrücklichen Nutzerwunsch keine Tests, kein `verify` und keine
automatisierte GUI-Prüfung. Nur Formatter und Paketbau mit
`./mvnw --no-transfer-progress spotless:apply package -Dmaven.test.skip=true`.
Protokoll: `target/photo-directory-build.log`.

Manuelle Abnahme durch den Nutzer: Ein Foto aus einem anderen Ordner wählen
und danach erneut die Fotoauswahl öffnen, auch bei einem anderen Eintrag.
Die App neu starten und den wieder vorgeschlagenen Ordner kontrollieren.
In einen weiteren Ordner navigieren und die Dateiauswahl abbrechen. Beim
nächsten Öffnen muss der zuvor bestätigte Ordner erhalten sein. Den zuletzt
gewählten Ordner testweise verschieben oder einen externen Datenträger trennen
und den Rückfall auf einen verfügbaren Standardordner prüfen.

## Farbwähler und JSON-Export, 31. Oktober 2026

Auf ausdrücklichen Nutzerwunsch keine Tests, kein `verify`, keine automatisierte
GUI-Bedienung und keine Bildschirmaufnahmen. Ausgeführt wurden nur Formatter
und Paketbau mit `./mvnw --no-transfer-progress spotless:apply package
-Dmaven.test.skip=true`. Buildprotokoll: `target/color-picker-build.log`.
Die App wird mit dem aktualisierten JAR für die eigene Abnahme geöffnet.

Manuell zu prüfen: Im Hinzufügen- oder Bearbeitungsdialog eine Farbe aus der
Palette oder eine eigene Farbe auswählen, speichern und Farbfeld sowie Hexwert
in der Liste ansehen. Den Eintrag erneut öffnen. Eine Farbe entfernen und
das Verwerfen reiner Farbänderungen prüfen. Bei einem alten Farbeintrag nur
die Notiz bearbeiten und kontrollieren, dass der bisherige Farbtext erhalten
bleibt. Anschließend einen solchen Altwert bewusst durch eine Farbe ersetzen.

Den Export mit einer gefilterten Liste ausführen und in der JSON-Datei alle
gespeicherten Einträge, ihre `color`-Werte sowie `photoBase64` ansehen. Auch einen
Eintrag ohne Farbe oder Foto berücksichtigen. Vorhandene Exportdateien müssen
erhalten bleiben. Ein abgebrochener Exportdialog darf keine Datei erzeugen.
Die bisherigen GUI-Hilfen aus der Internationalisierung erwarten beim Farbfeld
noch Texteingabe. Sie wurden in diesem Auftrag nicht ausgeführt oder erweitert.

## Zentrierte Garn-Dialoge, 31. Oktober 2026

„Garn hinzufügen“ und „Garn bearbeiten“ verwenden dieselbe Positionierung
wie der Datenschutzdialog. Die vorhandene Logik liegt jetzt app-lokal in
`UiSupport.centerAfterLayout`. Nach nativen Größenkorrekturen wird das Fenster
über seinem Hauptfenster zentriert, auf den sichtbaren Bildschirmbereich
begrenzt und anschließend eingeblendet. Beim Schließen stoppen die Timer.

`./mvnw --no-transfer-progress spotless:apply verify` mit Temurin 21.0.13+9
bestand mit 69 Tests, Compiler-Lint, Enforcer, Spotless und SpotBugs.
Protokoll: `target/dialog-centering-verify.log`.
Der vorhandene GUI-Ablauf auf isoliertem Xvfb bestand für en-US einschließlich
Aufnahmen, 58 Ablaufprüfungen und fünf Prüfungen nach einem Neustart in de-DE.
Er umfasst Eingaben, Fotos, Speichern, Abbrechen und den Verwerfschutz.
Die Aufnahme des Bearbeitungsdialogs wurde visuell auf Zentrierung geprüft.
Nachweise: `target/dialog-centering-gui`.

Das vorhandene Linux-App-Image erhielt das geprüfte App-JAR. Die alte Instanz
wurde über die Oberfläche regulär geschlossen, ohne eine Verwerfrückfrage zu
bestätigen. Die aktualisierte App wurde über den Benutzer-Service-Manager
geöffnet. Auf dem tatsächlichen Ubuntu-Desktop wurden die äußeren Fenstermaße
einschließlich Titelleisten mit X11 gemessen. Bei einem Hauptfenster von
1080 × 837 und einem Hinzufügendialog von 660 × 540 Pixeln beträgt die Abweichung
der Mittelpunkte horizontal 0 und vertikal 0,5 Pixel. Der Dialog bleibt geöffnet.
Messwerte: `target/dialog-centering-desktop/positions.json`. Visuelle Kontrolle
bestanden. Diese Prüfung gilt für Ubuntu, nicht für native Windows-/macOS-Fenster.

## Dauerhafter Start aus der Agentensitzung, 31. Oktober 2026

Yarn Box wurde über den bisherigen `gtk-launch`-Aufruf kurz sichtbar und
verschwand nach Abschluss des Tool-Befehls. Die Gegenprobe verwendete dieselbe
unveränderte Paketdatei, einen temporären Desktop-Eintrag und eine separate
Testsammlung unter `/tmp/yarn-box-start-check-xbhqwuhn`. Während des Befehls
bestand das zusätzliche Fenster. Im folgenden Tool-Aufruf war es verschwunden.
Die mit `strace` aufgezeichneten Prozesse und die Ausgabe lieferten keine
Java-Fehlermeldung. Das konkrete Beendigungssignal wurde nicht aufgezeichnet.

Der Start über `systemd-run --user` blieb dagegen über mehrere abgeschlossene
Tool-Aufrufe hinweg aktiv. Der Benutzer-Service-Manager ist der Elternprozess,
die App hat eine eigene Prozessgruppe. Prozess, sichtbares Hauptfenster und
Service-Status wurden wiederholt kontrolliert. Das Journal enthält nur den
erfolgreichen Start und keine App-Fehlermeldung. Die normale Sammlung ist mit
diesem Startweg geöffnet und bleibt für den Nutzer verfügbar.

Die Korrektur betrifft den Start durch den Agenten. `AGENTS.md` dokumentiert
den dauerhaften Startweg und die nachgelagerte Kontrolle. Anwendungscode,
Paketdateien und Desktop-Registrierung wurden nicht geändert. Keine Daten
migriert oder gelöscht. Keine Kompilierung oder automatisierte Tests für diese
Dokumentationsänderung. Inhalt, Shell-Beispiele und Diff wurden kontrolliert.

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

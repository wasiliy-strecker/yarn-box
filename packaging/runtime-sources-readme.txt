Yarn Box: lokale Laufzeitquellen und Herkunft

Eclipse Temurin 21.0.13+9, OpenJFX 21.0.11+4

OpenJDK21U-jdk-sources_21.0.13_9.tar.gz ist das vollständige Quellarchiv des festgelegten Temurin-Releases. Die zugehörige veröffentlichte SHA-256-Datei und JSON-Metadaten liegen daneben. Die Windows-x64-JSON-Datei enthält die Herkunft des vorgesehenen JDK-Binärarchivs einschließlich Binärprüfsumme und Buildkonfiguration. Das JDK-Binärarchiv selbst ist hier nicht enthalten.

temurin-build-1cf43d7a.tar.gz enthält die in diesen Windows-Buildmetadaten referenzierten Adoptium-Buildskripte. OpenJDK-Quellrevision: 2ab10ca20c191701c3a9f3153bfc6e93d63714c1. Buildskriptrevision: 1cf43d7a3e77886bfea7d550a902e151a87d724d.

openjfx-21.0.11_4-e5073339.tar.gz enthält das vollständige OpenJFX-Repository einschließlich nativer Quellen, Buildskripte und Lizenzen am Commit e5073339cd88cb09ea7c49c3007638bf610e01f3. Der offizielle Tag 21.0.11+4 verweist auf diesen Commit. Die SHA-256 dieser GitHub-Quellarchivlieferung wurde beim Bezug festgehalten. Es handelt sich nicht um eine vom Hersteller veröffentlichte Signatur oder eine unabhängige Reproduzierbarkeitsbestätigung.

runtime-sources.json nennt feste Download-URLs, Größen und SHA-256 aller sechs Dateien. Der Übergabehelfer bricht bei Abweichungen ab. Archive wurden nicht neu gepackt oder inhaltlich verändert. Sie enthalten die Original-Lizenztexte. Zusätzliche mit der App ausgelieferte Hinweise liegen unter licenses und THIRD_PARTY_NOTICES.md.

Diese Dateien sind lokale Release-Begleitmaterialien. Es wurde nichts öffentlich bereitgestellt. Vor öffentlicher Binärverteilung die passenden Quellen und erforderlichen Buildinformationen zugänglich machen und im Store darauf verweisen. Das endgültige Windows-Paket einschließlich zusätzlicher nativer Bibliotheken muss noch gegen Herkunft, Versionen und Lizenzumfang abgeglichen werden. Diese Vorbereitung bestätigt keine abschließende Lizenzabnahme.

Der eigene Yarn Box-Quellcode ist nicht Bestandteil dieser Fremdkomponentenfreigabe.

Mac-App-Store-Vorbereitung: Die Mac-Übergabe ergänzt diese gemeinsame Quelllieferung um die beiden Original-Buildmetadaten für Temurin ARM64 und Intel. Beide nennen dieselbe oben genannte Buildskriptrevision. Die zugehörigen Binärarchive und JavaFX-Mac-Module liegen separat unter MacInputs/dependencies. Ihre festen URLs, Prüfsummen und Architekturzuordnung stehen in Source/packaging/macos/dependencies.json. Auch die tatsächlich gebaute Mac-Ausgabe ist vor Veröffentlichung gegen diese Herkunft und ihren Lizenzumfang abzugleichen.

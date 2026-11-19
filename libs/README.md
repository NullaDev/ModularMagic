# Local development dependencies

L2Magic 3.0.6 is a separate prerequisite, preserved from the successful local build before the source migration.
All library JARs remain external dependencies; ModularMagic does not package their classes or resources.
The matching -sources.jar files are kept beside the binaries for IDE navigation.

Git keeps only the ten library families used by `build.gradle`, including their source
attachments. Other locally downloaded mod JARs (such as GlimmeringTales, L2Backpack,
L2Complements and L2Harvester) are ignored. Add an explicit `.gitignore` exception if
the build gains another vendored dependency. Keep `gradle-wrapper.jar` versioned too.

## Engine artifact checksums (SHA-256)

- l2magic-3.0.6.jar: 210167231261c81c1485fd07c7d0761b42d2e2afccaa1aeb22df6f700ba4ef36
- l2magic-3.0.6-sources.jar: 87f7aa8a50bfe691f0bac3502b1af40491949a347d757453f59c70bece110531

# Upstream reference sources

`l2magic/` preserves the L2Magic sources, generated resources, project documentation,
license and development configuration that this repository started from.

This directory is not a Gradle subproject or a source/resource directory. Its Java
classes, assets, data, access transformers and mixins are not included in ModularMagic.
The original upstream attribution and license remain in the snapshot.

The active project depends on the separate `dev.xkmc:l2magic:3.0.6` JAR in `libs/`.
That JAR and its sources were preserved from the successful local engine build before
moving the sources here. Keep the reference source available when inspecting engine
nodes; make gameplay changes under `src/main/java/cn/nulladev/modularmagic/`.

<!-- SPDX-License-Identifier: AGPL-3.0-or-later -->

## Qué cambió

<!-- Descripción concisa del cambio. -->

## Por qué

<!-- Problema que resuelve o motivación. Enlaza el issue: Closes #___ -->

## Cómo se probó

<!-- Comandos ejecutados, pruebas añadidas, verificación manual. -->

## Impacto

- **UX:** <!-- pantallas afectadas / ninguno -->
- **Base de datos:** <!-- ¿migración nueva? ¿probada en base nueva y actualizada? / ninguno -->
- **Seguridad:** <!-- permisos, secretos, validación de ficheros, auditoría / ninguno -->
- **Documentación:** <!-- docs actualizadas / N/A -->

## Checklist

- [ ] La rama parte de `experiment` actualizado y el PR apunta a `experiment`.
- [ ] Commits siguen Conventional Commits.
- [ ] `./gradlew build` y/o `npm run web:build` pasan localmente.
- [ ] Pruebas añadidas/actualizadas para la lógica nueva.
- [ ] Si cambia la BD: migración añadida (no editada una publicada) y probada.
- [ ] Documentación actualizada (root docs y/o `apps/docs`).
- [ ] Sin secretos ni datos de operador en el diff.
- [ ] Estados de UI cubiertos (loading / empty / error / success) cuando aplica.
- [ ] `CHANGELOG.md` actualizado si el cambio es visible para personas usuarias.

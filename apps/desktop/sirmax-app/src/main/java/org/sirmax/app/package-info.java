// SPDX-License-Identifier: AGPL-3.0-or-later

/**
 * The composition root of the SIRMAX desktop client.
 *
 * <p>This is the one place allowed to depend on every layer. It constructs the concrete adapters
 * from {@code sirmax-infrastructure}, injects them into the application use cases by hand, hands the
 * result to the JavaFX UI, and starts the app. Keeping the wiring explicit (no DI framework) keeps
 * startup fast and auditable (see {@code docs/adr/0005-modular-domain-architecture.md}).
 */
package org.sirmax.app;

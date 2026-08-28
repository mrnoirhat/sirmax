// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Enforces the layer dependency rules from {@code docs/adr/0005-modular-domain-architecture.md} and
 * {@code docs/domain/module-map.md} §5.
 */
class LayerBoundaryTest {

    private static JavaClasses sirmax;

    @BeforeAll
    static void importClasses() {
        sirmax =
                new ClassFileImporter()
                        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                        .importPackages("org.sirmax");
    }

    @Test
    void domainDoesNotDependOnJavaFx() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("org.sirmax.domain..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("javafx..");
        rule.check(sirmax);
    }

    @Test
    void domainDoesNotDependOnJdbc() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("org.sirmax.domain..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("java.sql..", "javax.sql..");
        rule.check(sirmax);
    }

    @Test
    void domainDoesNotDependOnInfrastructure() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("org.sirmax.domain..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("org.sirmax.infrastructure..");
        rule.check(sirmax);
    }

    @Test
    void applicationDoesNotDependOnJavaFxOrJdbc() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("org.sirmax.application..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("javafx..", "java.sql..", "javax.sql..");
        rule.check(sirmax);
    }

    @Test
    void applicationDoesNotDependOnInfrastructure() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("org.sirmax.application..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("org.sirmax.infrastructure..");
        rule.check(sirmax);
    }

    @Test
    void uiDoesNotDependOnJdbcOrInfrastructure() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("org.sirmax.ui..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage(
                                "java.sql..", "javax.sql..", "org.sirmax.infrastructure..");
        rule.check(sirmax);
    }
}

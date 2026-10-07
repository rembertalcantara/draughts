package com.draughts.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Enforces the dependency rule from SPEC.md section 5.1. */
@AnalyzeClasses(packages = "com.draughts", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule engineIsPure = classes().that().resideInAPackage("com.draughts.engine..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("com.draughts.engine..", "java..");

    @ArchTest
    static final ArchRule aiDependsOnlyOnEngine = classes().that().resideInAPackage("com.draughts.ai..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("com.draughts.ai..", "com.draughts.engine..", "java..");

    @ArchTest
    static final ArchRule domainDoesNotKnowAdapters = noClasses()
            .that().resideInAnyPackage("com.draughts.game..", "com.draughts.player..")
            .should().dependOnClassesThat().resideInAnyPackage("com.draughts.web..", "com.draughts.persistence..");

    @ArchTest
    static final ArchRule adaptersAreIndependent = noClasses().that().resideInAPackage("com.draughts.persistence..")
            .should().dependOnClassesThat().resideInAPackage("com.draughts.web..");
}

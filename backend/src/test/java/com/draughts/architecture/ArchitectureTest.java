package com.draughts.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

/** Enforces the hexagonal (ports and adapters) structure described in the README. */
@AnalyzeClasses(packages = "com.draughts", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule hexagonal = onionArchitecture()
            .domainModels("com.draughts.domain..")
            .domainServices("com.draughts.application.port..")
            .applicationServices("com.draughts.application.service..")
            .adapter("web", "com.draughts.adapter.in.web..")
            .adapter("scheduling", "com.draughts.adapter.in.scheduling..")
            .adapter("persistence", "com.draughts.adapter.out.persistence..")
            .adapter("ai", "com.draughts.adapter.out.ai..")
            .adapter("events", "com.draughts.adapter.out.event..")
            // The composition root wires everything together.
            .ignoreDependency(resideInAPackage("com.draughts.config.."), alwaysTrue());

    @ArchTest
    static final ArchRule domainIsFrameworkFree = classes().that().resideInAPackage("com.draughts.domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("com.draughts.domain..", "java..", "lombok..");

    @ArchTest
    static final ArchRule portsArePlainJava = classes().that().resideInAPackage("com.draughts.application.port..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("com.draughts.domain..", "com.draughts.application.port..", "java..", "lombok..");

    /** Use cases may be Spring beans with transactions, but know nothing about HTTP, SQL or messaging. */
    @ArchTest
    static final ArchRule servicesAreTechnologyAgnostic = noClasses()
            .that().resideInAPackage("com.draughts.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.web..", "org.springframework.jdbc..", "org.springframework.messaging..",
                    "jakarta.servlet..");

    @ArchTest
    static final ArchRule adaptersUsePortsNotServices = noClasses().that().resideInAPackage("com.draughts.adapter..")
            .should().dependOnClassesThat().resideInAPackage("com.draughts.application.service..");
}

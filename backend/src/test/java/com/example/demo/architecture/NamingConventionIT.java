package com.example.demo.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Architecture tests for naming conventions.
 */
@AnalyzeClasses(packages = "com.example.demo", importOptions = { ImportOption.DoNotIncludeTests.class })
public class NamingConventionIT {

    private static final String BASE_PACKAGE = "com.example.demo";

    @ArchTest
    public static final ArchRule CONTROLLER_CLASSES_SHOULD_BE_NAMED_CORRECTLY =
        classes().that().resideInAPackage(BASE_PACKAGE + ".**.controller..")
            .and().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .should().haveSimpleNameEndingWith("Controller")
            .as("Controller classes should end with 'Controller'");

    @ArchTest
    public static final ArchRule SERVICE_CLASSES_SHOULD_BE_NAMED_CORRECTLY =
        classes().that().resideInAPackage(BASE_PACKAGE + ".**.service..")
            .and().areAnnotatedWith("org.springframework.stereotype.Service")
            .should().haveSimpleNameEndingWith("Service")
            .as("Service classes should end with 'Service'");

    @ArchTest
    public static final ArchRule REPOSITORY_INTERFACES_SHOULD_BE_NAMED_CORRECTLY =
        classes().that().resideInAPackage(BASE_PACKAGE + ".**.repository..")
            .and().areInterfaces()
            .and().areAnnotatedWith("org.springframework.stereotype.Repository")
            .should().haveSimpleNameEndingWith("Repository")
            .as("Repository interfaces should end with 'Repository'");

    @ArchTest
    public static final ArchRule ENTITY_CLASSES_SHOULD_BE_IN_MODEL_PACKAGE =
        classes().that().areAnnotatedWith("jakarta.persistence.Entity")
            .should().resideInAPackage(BASE_PACKAGE + ".**.model..")
            .as("Entities should be in a 'model' package");
}

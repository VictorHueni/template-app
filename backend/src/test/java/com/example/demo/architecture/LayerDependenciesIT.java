package com.example.demo.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Architecture tests for layer dependencies.
 */
@AnalyzeClasses(packages = "com.example.demo", importOptions = { ImportOption.DoNotIncludeTests.class })
public class LayerDependenciesIT {

    private static final String BASE_PACKAGE = "com.example.demo";

    @ArchTest
    public static final ArchRule LAYER_DEPENDENCIES_ARE_RESPECTED = layeredArchitecture().consideringAllDependencies()
            // Define Layers
            .layer("Api").definedBy(BASE_PACKAGE + ".api.v1..")
            .layer("Controller").definedBy(BASE_PACKAGE + ".**.controller..")
            .layer("Service").definedBy(BASE_PACKAGE + ".**.service..")
            .layer("Repository").definedBy(BASE_PACKAGE + ".**.repository..")
            .layer("Config").definedBy(BASE_PACKAGE + ".**.config..")

            // Define Access Rules
            .whereLayer("Controller").mayOnlyAccessLayers("Service", "Api")
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Service") // Services can call other services
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
            .whereLayer("Api").mayNotBeAccessedByAnyLayer() // The API is the top layer, nothing should access it except controllers
            .as("Layer dependencies should be respected");
}
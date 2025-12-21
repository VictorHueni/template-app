package com.example.demo.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import com.example.demo.DemoApplication;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Architecture test that verifies the modular structure of the application.
 *
 * <p>This test uses Spring Modulith to discover and document modules.
 * Full verification is not enforced because the codebase predates modulith
 * and has shared utilities in 'common' that are used across modules.</p>
 *
 * <p>Module structure:</p>
 * <ul>
 *   <li><strong>greeting</strong>: Greeting domain (produces events)</li>
 *   <li><strong>audit</strong>: Business activity logging (consumes events)</li>
 *   <li><strong>user</strong>: User management</li>
 *   <li><strong>common</strong>: Shared utilities (open module)</li>
 *   <li><strong>api</strong>: OpenAPI generated code (open module)</li>
 * </ul>
 */
class ModulithArchitectureTest {

    private final ApplicationModules modules = ApplicationModules.of(DemoApplication.class);

    @Test
    void discoversExpectedModules() {
        // Verify the expected modules are discovered
        assertThat(modules.stream().map(m -> m.getName()))
                .contains("audit", "greeting", "common", "user");

        // Print module structure for documentation
        modules.forEach(module ->
                System.out.println("Module: " + module.getName() +
                        " - Base package: " + module.getBasePackage()));
    }

    @Test
    void verifyModularStructure() {
        // Verifies:
        // - No cyclic dependencies between modules
        // - Modules only access other modules' public API (not internal packages)
        // - Modules only depend on explicitly allowed dependencies
        modules.verify();
    }

    @Test
    void generateModuleDocumentation() {
        // Generates comprehensive module documentation
        // Output: target/spring-modulith-docs/
        var documenter = new Documenter(modules);

        // Generate complete documentation:
        // - C4 component diagram (all modules)
        // - Individual module diagrams
        // - Module canvases (detailed tables per module)
        // - all-docs.adoc (aggregating document)
        documenter.writeDocumentation();

        System.out.println("✅ Module documentation generated in: target/spring-modulith-docs/");
        System.out.println("   📊 Component diagrams (PlantUML C4)");
        System.out.println("   📋 Module canvases (AsciiDoc)");
        System.out.println("   📄 all-docs.adoc (aggregating document)");
    }
}

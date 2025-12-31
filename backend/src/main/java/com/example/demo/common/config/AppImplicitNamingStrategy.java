package com.example.demo.common.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitEntityNameSource;
import org.hibernate.boot.model.naming.ImplicitForeignKeyNameSource;
import org.hibernate.boot.model.naming.ImplicitIndexNameSource;
import org.hibernate.boot.model.naming.ImplicitUniqueKeyNameSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.hibernate.SpringImplicitNamingStrategy;

/**
 * Custom Implicit Naming Strategy to enforce modern SQL naming standards for constraints.
 * <p>
 * Standards:
 * - Foreign Key: fk_{table_name}_{target_table_name}
 * - Unique Key: uk_{table_name}_{column_name}
 * - Index: ix_{table_name}_{column_name}
 * <p>
 * Note: Primary Key constraint naming is not handled by ImplicitNamingStrategy in Hibernate 6.
 * It is typically handled by the Dialect or physical naming if exposed.
 */
public class AppImplicitNamingStrategy extends SpringImplicitNamingStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppImplicitNamingStrategy.class);

    @Override
    public Identifier determinePrimaryTableName(ImplicitEntityNameSource source) {
        String tableName = source.getEntityNaming().getJpaEntityName();
        
        try {
            String className = source.getEntityNaming().getClassName();
            if (className != null) {

                // Justification: className is from Hibernate's internal entity metadata (compile-time entities only),
                // not user input. This is a safe ORM framework integration pattern with proper exception handling.
                // nosemgrep: java.lang.security.audit.unsafe-reflection.unsafe-reflection
                Class<?> entityClass = Class.forName(className);
                Module moduleAnnotation = entityClass.getAnnotation(Module.class);
                if (moduleAnnotation != null) {
                    // Result: module + entityName (e.g. "auth" + "User" -> "authUser")
                    // The PhysicalNamingStrategy will then convert "authUser" to "auth_user"
                    tableName = moduleAnnotation.value() + tableName;
                }
            }
        }
        catch (ClassNotFoundException | LinkageError e) {
            // Class not found or cannot be loaded, fallback to default naming
            LOGGER.debug(
                    "Failed to load entity class for implicit naming strategy, using default naming convention", e
            );
        }
        
        return toIdentifier(tableName, source.getBuildingContext());
    }

    @Override
    public Identifier determineForeignKeyName(ImplicitForeignKeyNameSource source) {
        // fk_{source_table}_{column_name}_{referenced_table}
        // This pattern prevents collisions when multiple FKs reference the same target table
        String sourceTable = source.getTableName().getText();
        String targetTable = source.getReferencedTableName().getText();
        // Get the column name(s) from the source table that form the FK
        // For most cases, this is a single column; for composite FKs, use the first one
        String columnName = source.getColumnNames().get(0).getText();
        String name = "fk_" + sourceTable + "_" + columnName + "_" + targetTable;
        return toIdentifier(name, source.getBuildingContext());
    }

    @Override
    public Identifier determineUniqueKeyName(ImplicitUniqueKeyNameSource source) {
        // uk_{table}_{column}
        String tableName = source.getTableName().getText();
        // Use the first column name for readability. 
        // Note: Complex composite keys might need a more robust name generation if collisions occur,
        // but for most cases, the first column is the distinguishing factor.
        String columnName = source.getColumnNames().get(0).getText();
        String name = "uk_" + tableName + "_" + columnName;
        return toIdentifier(name, source.getBuildingContext());
    }

    @Override
    public Identifier determineIndexName(ImplicitIndexNameSource source) {
        // ix_{table}_{column}
        String tableName = source.getTableName().getText();
        String columnName = source.getColumnNames().get(0).getText();
        String name = "ix_" + tableName + "_" + columnName;
        return toIdentifier(name, source.getBuildingContext());
    }
}

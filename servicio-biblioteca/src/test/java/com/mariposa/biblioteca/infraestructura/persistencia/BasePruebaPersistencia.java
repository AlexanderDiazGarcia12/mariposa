package com.mariposa.biblioteca.infraestructura.persistencia;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.format_sql=false",
        "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(
        basePackages = "com.mariposa.biblioteca.infraestructura.persistencia",
        includeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = {
                        "com\\.mariposa\\.biblioteca\\.infraestructura\\.persistencia\\.mapeadores\\..*",
                        "com\\.mariposa\\.biblioteca\\.infraestructura\\.persistencia\\.adaptadores\\..*"
                }
        )
)
public abstract class BasePruebaPersistencia {

    @SuppressWarnings("resource")
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("biblioteca_test")
                    .withUsername("biblioteca_test")
                    .withPassword("biblioteca_test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configurarPropiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        registro.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registro.add("spring.flyway.user", POSTGRES::getUsername);
        registro.add("spring.flyway.password", POSTGRES::getPassword);
    }
}

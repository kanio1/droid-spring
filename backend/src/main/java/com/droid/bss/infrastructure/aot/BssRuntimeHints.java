package com.droid.bss.infrastructure.aot;

import com.droid.bss.BssApplication;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.Resource;

/**
 * AOT Runtime Hints for GraalVM Native Image compilation
 * This class configures what resources, reflection, and proxies should be included
 */
@Configuration
@ImportRuntimeHints(BssRuntimeHints.BssApplicationRuntimeHints.class)
public class BssRuntimeHints {

    static class BssApplicationRuntimeHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader cl) {
            // Register main application class
            hints.resources().registerPattern("application.yaml");
            hints.resources().registerPattern("application-prod.yml");
            hints.resources().registerPattern("graphql/schema.graphqls");
            hints.resources().registerPattern("graphql/schema.graphql");
            hints.resources().registerPattern("db/migration/*");

            // Register reflection hints for domain entities
            hints.reflection().registerTypes(
                // Customer domain
                com.droid.bss.domain.customer.Customer.class,
                com.droid.bss.domain.customer.CustomerEntity.class,
                com.droid.bss.domain.customer.CustomerStatus.class,

                // Invoice domain
                com.droid.bss.domain.invoice.Invoice.class,
                com.droid.bss.domain.invoice.InvoiceEntity.class,
                com.droid.bss.domain.invoice.InvoiceStatus.class,
                com.droid.bss.domain.invoice.InvoiceItem.class,
                com.droid.bss.domain.invoice.InvoiceItemEntity.class,
                com.droid.bss.domain.invoice.InvoiceItemStatus.class,

                // Order domain
                com.droid.bss.domain.order.Order.class,
                com.droid.bss.domain.order.OrderEntity.class,
                com.droid.bss.domain.order.OrderItem.class,
                com.droid.bss.domain.order.OrderItemEntity.class,

                // Payment domain
                com.droid.bss.domain.payment.Payment.class,
                com.droid.bss.domain.payment.PaymentEntity.class,
                com.droid.bss.domain.payment.PaymentStatus.class,

                // Product domain
                com.droid.bss.domain.product.Product.class,
                com.droid.bss.domain.product.ProductEntity.class,
                com.droid.bss.domain.product.ProductStatus.class,

                // Subscription domain
                com.droid.bss.domain.subscription.Subscription.class,
                com.droid.bss.domain.subscription.SubscriptionEntity.class,
                com.droid.bss.domain.subscription.SubscriptionStatus.class,

                // Address domain
                com.droid.bss.domain.address.Address.class,
                com.droid.bss.domain.address.AddressEntity.class,
                com.droid.bss.domain.address.AddressStatus.class,

                // Common entities
                com.droid.bss.domain.common.BaseEntity.class,

                // Application DTOs
                com.droid.bss.application.dto.customer.CreateCustomerCommand.class,
                com.droid.bss.application.dto.customer.UpdateCustomerCommand.class,
                com.droid.bss.application.dto.customer.CustomerResponse.class,
                com.droid.bss.application.dto.customer.CustomerListResponse.class,

                com.droid.bss.application.dto.invoice.InvoiceResponse.class,
                com.droid.bss.application.dto.invoice.InvoiceItemResponse.class,

                com.droid.bss.application.dto.payment.PaymentResponse.class,

                com.droid.bss.application.dto.subscription.SubscriptionResponse.class,

                // GraphQL
                com.droid.bss.api.graphql.CustomerGraphQLController.class,
                com.droid.bss.infrastructure.graphql.GraphQLConfig.class,
                com.droid.bss.infrastructure.graphql.DataLoaderConfig.class,
                com.droid.bss.infrastructure.graphql.GraphQLPlaygroundConfig.class,

                // Infrastructure
                com.droid.bss.infrastructure.exception.GlobalExceptionHandler.class,
                com.droid.bss.infrastructure.security.SecurityConfig.class
            );

            // Register JSON serialization hints
            hints.reflection().registerTypes(
                // Jackson annotations
                com.fasterxml.jackson.annotation.JsonProperty.class,
                com.fasterxml.jackson.annotation.JsonIgnore.class,
                com.fasterxml.jackson.annotation.JsonIgnoreProperties.class,
                com.fasterxml.jackson.annotation.JsonInclude.class,
                com.fasterxml.jackson.annotation.JsonManagedReference.class,
                com.fasterxml.jackson.annotation.JsonBackReference.class,
                com.fasterxml.jackson.annotation.JsonSubTypes.class,
                com.fasterxml.jackson.annotation.JsonTypeInfo.class,
                com.fasterxml.jackson.annotation.JsonCreator.class,
                com.fasterxml.jackson.annotation.JsonValue.class,
                com.fasterxml.jackson.annotation.JsonAutoDetect.class,
                com.fasterxml.jackson.databind.ObjectMapper.class
            );

            // Register validation hints
            hints.reflection().registerTypes(
                jakarta.validation.constraints.NotNull.class,
                jakarta.validation.constraints.NotBlank.class,
                jakarta.validation.constraints.NotEmpty.class,
                jakarta.validation.constraints.Size.class,
                jakarta.validation.constraints.Email.class,
                jakarta.validation.constraints.Min.class,
                jakarta.validation.constraints.Max.class,
                jakarta.validation.constraints.Pattern.class
            );

            // Register Spring annotations
            hints.reflection().registerTypes(
                org.springframework.stereotype.Component.class,
                org.springframework.stereotype.Service.class,
                org.springframework.stereotype.Repository.class,
                org.springframework.stereotype.Controller.class,
                org.springframework.web.bind.annotation.RestController.class,
                org.springframework.web.bind.annotation.RequestMapping.class,
                org.springframework.web.bind.annotation.GetMapping.class,
                org.springframework.web.bind.annotation.PostMapping.class,
                org.springframework.web.bind.annotation.PutMapping.class,
                org.springframework.web.bind.annotation.DeleteMapping.class,
                org.springframework.web.bind.annotation.PatchMapping.class,
                org.springframework.web.bind.annotation.RequestBody.class,
                org.springframework.web.bind.annotation.RequestParam.class,
                org.springframework.web.bind.annotation.PathVariable.class,
                org.springframework.web.bind.annotation.ModelAttribute.class,
                org.springframework.beans.factory.annotation.Autowired.class,
                org.springframework.beans.factory.annotation.Qualifier.class,
                org.springframework.context.annotation.Bean.class,
                org.springframework.context.annotation.Configuration.class,
                org.springframework.context.annotation.Import.class,
                org.springframework.context.annotation.ComponentScan.class,
                org.springframework.context.annotation.EnableConfigurationProperties.class,
                org.springframework.transaction.annotation.Transactional.class,
                org.springframework.cache.annotation.Cacheable.class,
                org.springframework.cache.annotation.CacheEvict.class,
                org.springframework.cache.annotation.EnableCaching.class
            );

            // Register JPA/Hibernate hints
            hints.reflection().registerTypes(
                jakarta.persistence.Entity.class,
                jakarta.persistence.Table.class,
                jakarta.persistence.Column.class,
                jakarta.persistence.Id.class,
                jakarta.persistence.GenerationType.class,
                jakarta.persistence.GeneratedValue.class,
                jakarta.persistence.OneToMany.class,
                jakarta.persistence.OneToOne.class,
                jakarta.persistence.ManyToOne.class,
                jakarta.persistence.ManyToMany.class,
                jakarta.persistence.JoinColumn.class,
                jakarta.persistence.JoinTable.class,
                jakarta.persistence.Embeddable.class,
                jakarta.persistence.Embedded.class,
                jakarta.persistence.Enumerated.class,
                jakarta.persistence.Temporal.class,
                jakarta.persistence.Lob.class
            );

            // Register Flyway hints
            hints.reflection().registerTypes(
                org.flywaydb.core.api.migration.BaseJavaMigration.class,
                org.flywaydb.core.api.migration.JavaMigration.class
            );

            // Register Kafka hints
            hints.reflection().registerTypes(
                org.springframework.kafka.annotation.KafkaListener.class,
                org.springframework.kafka.config.TopicBuilder.class
            );

            // Register GraphQL hints
            try {
                hints.reflection().registerTypes(
                    Class.forName("graphql.schema.GraphQLScalarType"),
                    Class.forName("graphql.GraphQL"),
                    Class.forName("graphql.schema.GraphQLSchema"),
                    Class.forName("graphql.schema.idl.RuntimeWiring"),
                    Class.forName("graphql.execution.DataFetcherExceptionHandler")
                );
            } catch (ClassNotFoundException e) {
                // GraphQL classes not found, skip
            }

            // Register for PostgreSQL SSL
            hints.reflection().registerTypes(
                org.postgresql.ssl.NonValidatingFactory.class
            );
        }
    }
}

# Spring Components Implementation Guide for BSS
**Szczegółowy Plan Implementacji z Przykładami Kodu**

---

## 🎯 FAZA 1: Spring GraphQL

### Dlaczego zacząć od GraphQL?

**Business Value:**
- Frontend może pobrać klienta, faktury, płatności, subskrypcje w JEDNYM requescie
- Eliminacja problemów N+1 queries
- Real-time updates przez subscriptions
- Type-safe schema (auto-generowane typy TypeScript)
- 70% mniej requestów HTTP

### Krok 1: Dodaj zależności

```xml
<!-- W pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-graphql</artifactId>
</dependency>

<!-- GraphiQL IDE dla development -->
<dependency>
    <groupId>graphql-kickstart</groupId>
    <artifactId>graphql-kickstart-spring-boot-starter-ui-playground</artifactId>
    <version>12.1.0</version>
</dependency>
```

### Krok 2: GraphQL Schema (schema.graphqls)

```graphql
# BSS Domain Schema
scalar DateTime
scalar UUID

type Customer {
    id: UUID!
    email: String!
    firstName: String!
    lastName: String!
    createdAt: DateTime!
    invoices: [Invoice!]!
    subscriptions: [Subscription!]!
    totalRevenue: Float!
    status: CustomerStatus!
}

type Invoice {
    id: UUID!
    customerId: UUID!
    amount: Float!
    status: InvoiceStatus!
    dueDate: DateTime!
    paidAt: DateTime
    items: [InvoiceItem!]!
    payment: Payment
}

type Subscription {
    id: UUID!
    customerId: UUID!
    productId: UUID!
    status: SubscriptionStatus!
    startDate: DateTime!
    endDate: DateTime
    price: Float!
}

type Payment {
    id: UUID!
    invoiceId: UUID!
    amount: Float!
    method: PaymentMethod!
    status: PaymentStatus!
    processedAt: DateTime!
}

# Queries
type Query {
    # Get customer by ID with all related data
    customer(id: UUID!): Customer

    # Get customers with pagination and filtering
    customers(
        page: Int = 0
        size: Int = 20
        status: CustomerStatus
    ): CustomerConnection!

    # Search customers
    searchCustomers(query: String!): [Customer!]!

    # Get invoices with filtering
    invoices(
        status: InvoiceStatus
        customerId: UUID
        fromDate: DateTime
        toDate: DateTime
    ): [Invoice!]!

    # Dashboard metrics
    dashboardMetrics: DashboardMetrics!
}

# Mutations
type Mutation {
    # Create customer
    createCustomer(input: CreateCustomerInput!): Customer!

    # Update customer
    updateCustomer(id: UUID!, input: UpdateCustomerInput!): Customer!

    # Create invoice
    createInvoice(input: CreateInvoiceInput!): Invoice!

    # Process payment
    processPayment(input: ProcessPaymentInput!): Payment!
}

# Subscriptions - Real-time updates
type Subscription {
    # Customer created/updated
    customerEvents: CustomerEvent!

    # Invoice status changes
    invoiceEvents: InvoiceEvent!

    # Payment processed
    paymentEvents: PaymentEvent!
}

# Pagination helper
type CustomerConnection {
    edges: [CustomerEdge!]!
    pageInfo: PageInfo!
    totalCount: Int!
}

type CustomerEdge {
    node: Customer!
    cursor: String!
}

# Dashboard metrics
type DashboardMetrics {
    totalCustomers: Int!
    activeSubscriptions: Int!
    monthlyRevenue: Float!
    overdueInvoices: Int!
    recentPayments: [Payment!]!
}

enum CustomerStatus { ACTIVE INACTIVE SUSPENDED }
enum InvoiceStatus { DRAFT PENDING PAID OVERDUE CANCELLED }
enum PaymentStatus { PENDING COMPLETED FAILED REFUNDED }
enum SubscriptionStatus { ACTIVE CANCELLED EXPIRED }

input CreateCustomerInput {
    email: String!
    firstName: String!
    lastName: String!
}

input UpdateCustomerInput {
    email: String
    firstName: String
    lastName: String
}

input CreateInvoiceInput {
    customerId: UUID!
    items: [InvoiceItemInput!]!
    dueDate: DateTime!
}

input InvoiceItemInput {
    description: String!
    quantity: Int!
    unitPrice: Float!
}

input ProcessPaymentInput {
    invoiceId: UUID!
    amount: Float!
    method: PaymentMethod!
}
```

### Krok 3: GraphQL Resolver (CustomerResolver.java)

```java
@Component
public class CustomerResolver implements GraphQLResolver<Customer> {

    private final InvoiceRepository invoiceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;

    public CustomerResolver(InvoiceRepository invoiceRepository,
                           SubscriptionRepository subscriptionRepository,
                           PaymentRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
    }

    // Batch loading for N+1 optimization
    @BatchMapping
    public CompletableFuture<List<List<Invoice>>> invoices(List<Customer> customers) {
        List<UUID> ids = customers.stream()
            .map(Customer::getId)
            .toList();

        return CompletableFuture.supplyAsync(() ->
            invoiceRepository.findByCustomerIdIn(ids)
        );
    }

    @BatchMapping
    public CompletableFuture<List<List<Subscription>>> subscriptions(List<Customer> customers) {
        List<UUID> ids = customers.stream()
            .map(Customer::getId)
            .toList();

        return CompletableFuture.supplyAsync(() ->
            subscriptionRepository.findByCustomerIdIn(ids)
        );
    }

    // Calculated field - total revenue
    public CompletableFuture<Double> getTotalRevenue(Customer customer) {
        return CompletableFuture.supplyAsync(() ->
            paymentRepository.getTotalRevenueByCustomer(customer.getId())
        );
    }
}
```

### Krok 4: Query Controller (GraphQLController.java)

```java
@RestController
public class GraphQLController {

    private final CustomerService customerService;
    private final InvoiceService invoiceService;
    private final SubscriptionService subscriptionService;
    private final DashboardService dashboardService;

    @QueryMapping
    public Customer customer(@Argument UUID id) {
        return customerService.findById(id);
    }

    @QueryMapping
    public CustomerConnection customers(
            @Argument int page,
            @Argument int size,
            @Argument CustomerStatus status) {
        return customerService.findCustomers(page, size, status);
    }

    @QueryMapping
    public List<Invoice> invoices(
            @Argument InvoiceStatus status,
            @Argument UUID customerId,
            @Argument DateTime fromDate,
            @Argument DateTime toDate) {
        return invoiceService.findInvoices(status, customerId, fromDate, toDate);
    }

    @QueryMapping
    public DashboardMetrics dashboardMetrics() {
        return dashboardService.getMetrics();
    }

    // Mutations
    @MutationMapping
    public Customer createCustomer(@Argument CreateCustomerInput input) {
        return customerService.create(input);
    }

    @MutationMapping
    public Customer updateCustomer(@Argument UUID id, @Argument UpdateCustomerInput input) {
        return customerService.update(id, input);
    }

    @MutationMapping
    public Invoice createInvoice(@Argument CreateInvoiceInput input) {
        return invoiceService.create(input);
    }

    @MutationMapping
    public Payment processPayment(@Argument ProcessPaymentInput input) {
        return paymentService.process(input);
    }

    // Subscriptions - Real-time updates
    @SubscriptionMapping
    public Flux<CustomerEvent> customerEvents() {
        return customerEventPublisher.customerEvents();
    }

    @SubscriptionMapping
    public Flux<InvoiceEvent> invoiceEvents() {
        return invoiceEventPublisher.invoiceEvents();
    }

    @SubscriptionMapping
    public Flux<PaymentEvent> paymentEvents() {
        return paymentEventPublisher.paymentEvents();
    }
}
```

### Krok 5: Konfiguracja GraphQL (GraphQLConfig.java)

```java
@Configuration
@EnableGraphQl
public class GraphQLConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
            // Custom scalars
            .scalar(ExtendedScalars.DateTime)
            .scalar(ExtendedScalars.UUID)
            // DataFetcher for custom fields
            .type("Customer", typeWiring -> typeWiring
                .dataFetcher("totalRevenue", environment -> {
                    Customer customer = environment.getSource();
                    return paymentService.getTotalRevenue(customer.getId());
                })
            );
    }

    @Bean
    public GraphQlSourceBuilderCustomizer sourceBuilderCustomizer() {
        return builder -> builder
            .schemaResources(Pattern.compile("schema\\.graphqlqls"));
    }
}
```

### Krok 6: Frontend Integration (GraphQL Client)

```typescript
// graphql-client.ts
import { ApolloClient, InMemoryCache, createHttpLink } from '@apollo/client/core';
import { GraphQLWsLink } from '@apollo/client/link/subscriptions';
import { getMainDefinition } from '@apollo/client/utilities';
import { split } from '@apollo/client/link/core';
import { createClient } from 'graphql-ws';

const httpLink = createHttpLink({
  uri: '/graphql',
  headers: {
    authorization: `Bearer ${getAuthToken()}`
  }
});

const wsLink = new GraphQLWsLink(createClient({
  url: `ws://localhost:3000/graphql`,
  connectionParams: {
    authorization: `Bearer ${getAuthToken()}`
  }
}));

// Split links based on operation type
const link = split(
  ({ query }) => {
    const definition = getMainDefinition(query);
    return (
      definition.kind === 'OperationDefinition' &&
      definition.operation === 'subscription'
    );
  },
  wsLink,
  httpLink
);

export const apolloClient = new ApolloClient({
  link,
  cache: new InMemoryCache(),
});

// hooks/useGraphQL.ts
export const useCustomer = (id: string) => {
  return useQuery(GET_CUSTOMER, {
    variables: { id },
    fetchPolicy: 'cache-and-network'
  });
};

export const useCustomerSubscriptions = () => {
  return useSubscription(CUSTOMER_EVENTS, {
    onData: ({ data }) => {
      // Update cache or show notification
      console.log('Customer event:', data);
    }
  });
};
```

### Krok 7: Przykład Użycia (React Component)

```typescript
// CustomerDashboard.tsx
const CustomerDashboard = ({ customerId }: { customerId: string }) => {
  const { data, loading, error } = useQuery(GET_CUSTOMER_WITH_RELATIONS, {
    variables: { id: customerId }
  });

  // Subskrypcja na real-time updates
  useSubscription(INVOICE_STATUS_CHANGED, {
    variables: { customerId },
    onData: ({ data }) => {
      toast.success(`Invoice ${data.invoiceId} status changed!`);
      // Refresh query
      refetch();
    }
  });

  if (loading) return <Spinner />;
  if (error) return <ErrorAlert error={error} />;

  const { customer } = data;

  return (
    <div>
      <h1>{customer.firstName} {customer.lastName}</h1>
      <p>Email: {customer.email}</p>
      <p>Total Revenue: ${customer.totalRevenue}</p>

      <h2>Active Subscriptions</h2>
      {customer.subscriptions.map(sub => (
        <SubscriptionCard key={sub.id} subscription={sub} />
      ))}

      <h2>Recent Invoices</h2>
      {customer.invoices.map(invoice => (
        <InvoiceCard key={invoice.id} invoice={invoice} />
      ))}
    </div>
  );
};

const GET_CUSTOMER_WITH_RELATIONS = gql`
  query GetCustomer($id: UUID!) {
    customer(id: $id) {
      id
      email
      firstName
      lastName
      status
      totalRevenue
      invoices {
        id
        amount
        status
        dueDate
      }
      subscriptions {
        id
        status
        startDate
        product {
          name
          price
        }
      }
    }
  }
`;

const INVOICE_STATUS_CHANGED = gql`
  subscription InvoiceStatusChanged($customerId: UUID!) {
    invoiceEvents(customerId: $customerId) {
      invoiceId
      status
      changedAt
    }
  }
`;
```

### Korzyści GraphQL

✅ **Eliminuje N+1 problem** - Batch loading automatyczny
✅ **70% mniej requestów** - Wszystko w jednym zapytaniu
✅ **Type-safety** - Auto-generowane TypeScript types
✅ **Real-time** - Subscriptions dla live updates
✅ **Single source of truth** - GraphQL schema = kontrakt
✅ **Powerful tooling** - GraphiQL, Apollo DevTools

---

## 🎯 FAZA 2: Spring Native (GraalVM)

### Dlaczego Native to game-changer?

**Performance (Production):**
- ⚡ **Startup time:** 50ms (vs 5000ms JVM)
- 💾 **Memory:** 100MB (vs 500MB JVM)
- 🐳 **Docker image:** 50MB (vs 300MB)
- 🔥 **TPS:** 3x więcej throughput
- ⚡ **Cold start:** Niewidoczny (50ms)

### Krok 1: Native Configuration

```xml
<!-- W pom.xml -->
<properties>
    <native.buildtools.version>0.9.28</native.buildtools.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aot</artifactId>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- Standard Spring Boot plugin -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <image>
                    <builder>paketobuildpacks/builder:tiny</builder>
                    <env>
                        <BP_NATIVE_IMAGE>true</BP_NATIVE_IMAGE>
                    </env>
                </image>
            </configuration>
        </plugin>

        <!-- Native Build Tools -->
        <plugin>
            <groupId>org.graalvm.buildtools</groupId>
            <artifactId>native-maven-plugin</artifactId>
            <version>${native.buildtools.version}</version>
            <executions>
                <execution>
                    <id>build-native</id>
                    <goals>
                        <goal>compile-no-fork</goal>
                    </goals>
                </execution>
                <execution>
                    <id>test-native</id>
                    <goals>
                        <goal>test-compile-no-fork</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Krok 2: Native Hint Configuration (ResourceHint.java)

```java
@NativeHint(
    type = @TypeHint(
        types = {
            Customer.class,
            Invoice.class,
            Subscription.class,
            Payment.class
        }
    ),
    resources = {
        @ResourceHint(patterns = {
            "schema.graphqls",
            "db/migration/**",
            "application.yaml"
        })
    },
    initialization = @Initialization(
        value = "com.droid.bss.BssApplication",
        buildTime = Initialization.BuildTime.RUN_AT_BUILD_TIME
    )
)
public class ResourceHint {
    // GraalVM AOT configuration marker class
}
```

### Krok 3: Build & Run Native Image

```bash
# Build native image
./mvnw -Pnative spring-boot:run

# Build Docker image (native)
./mvnw spring-boot:build-image

# Test native image
./target/bss-backend

# Benchmark
# JVM: 5s startup, 500MB memory
# Native: 50ms startup, 100MB memory ⚡
```

### Krok 4: Native Benefits for BSS

```java
// Start time comparison
// JVM Mode:
public class BssApplication {
    public static void main(String[] args) {
        // ~5 seconds to reach this line
        SpringApplication.run(BssApplication.class, args);
    }
}

// Native Mode:
public class BssApplication {
    public static void main(String[] args) {
        // ~50 milliseconds to reach this line ⚡
        SpringApplication.run(BssApplication.class, args);
    }
}
```

### Korzyści Native dla BSS

✅ **Instant scaling** - Nowe instancje w 50ms
✅ **Lower costs** - 5x mniejsze instancje AWS/Azure/GCP
✅ **Better UX** - API response w 50ms (vs 5s cold start)
✅ **Container efficiency** - 50MB image (vs 300MB)
✅ **Reduced overhead** - Brak JIT, interpreter
✅ **Production-ready** - Netflix, Google używają w production

---

## 🎯 FAZA 3: Spring RSocket (Real-time)

### Dlaczego RSocket?

**Use Cases w BSS:**
- 🔔 Real-time notifications (płatności, faktury)
- 📊 Live dashboard updates
- 👤 Customer session management
- 📈 Monitoring metrics stream
- 🎮 Interactive features

### Krok 1: RSocket Configuration

```java
@Configuration
@EnableRSocket
public class RSocketConfig implements RSocketConfigurer {

    @Override
    public void configureMessageSequences(Consumer<Consumer<RSocketMessageSequence>> consumer) {
        // Custom message sequences
    }

    @Override
    public void configureRSocketServer(Consumer<RSocketServerConfigurer> consumer) {
        consumer.accept(config -> config
            .socketAddressSupplier(() -> InetSocketAddress.createAnyLocal())
            .fragments(16 * 1024)
            .mtu(16 * 1024)
        );
    }
}
```

### Krok 2: RSocket Controller (NotificationService.java)

```java
@Controller
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Fire-and-forget - payment processed
    @MessageMapping("payment.processed")
    public Mono<Void> paymentProcessed(PaymentEvent event) {
        log.info("Payment processed: {}", event.getId());
        notificationService.sendToAdmins(event);
        return Mono.empty();
    }

    // Request-response - get current customer session
    @MessageMapping("session.get")
    public Mono<CustomerSession> getSession(SessionRequest request) {
        return sessionService.getSession(request.getCustomerId());
    }

    // Stream - real-time notifications
    @MessageMapping("notifications.stream")
    public Flux<Notification> streamNotifications(String customerId) {
        return notificationService.streamForCustomer(customerId)
            .delayElements(Duration.ofSeconds(1)); // Simulate real-time
    }

    // Channel - bidirectional communication
    @MessageMapping("monitoring.channel")
    public Flux<MonitoringEvent> monitoringChannel(Flux<MonitoringRequest> requests) {
        return requests
            .flatMap(request -> {
                log.info("Monitoring request: {}", request.getType());
                return metricsService.streamMetrics(request.getType());
            });
    }
}
```

### Krok 3: Client-side RSocket (Frontend)

```typescript
// rsocket-client.ts
import { RSocketClient, JsonSerializers } from 'rsocket-core';
import { WebsocketClientTransport } from 'rsocket-websocket-client';

const client = new RSocketClient({
  serializers: JsonSerializers,
  transport: new WebsocketClientTransport({
    url: 'ws://localhost:8080/rsocket',
    connectionParams: {
      authToken: getAuthToken()
    }
  })
});

// Connect
client.connect().then(socket => {
  console.log('Connected to RSocket server');

  // Fire-and-forget
  socket.fireAndForget({
    data: {
      type: 'payment.processed',
      paymentId: 'pay_123',
      amount: 99.99
    }
  });

  // Request-response
  socket.requestResponse({
    data: {
      customerId: 'cust_123'
    }
  }).subscribe({
    onNext: response => {
      console.log('Session:', response.data);
    }
  });

  // Stream - real-time notifications
  const subscription = socket.requestStream({
    data: { customerId: 'cust_123' }
  }).subscribe({
    onNext: notification => {
      showNotification(notification.data);
    },
    onError: error => {
      console.error('Stream error:', error);
    },
    onComplete: () => {
      console.log('Stream completed');
    }
  });
});

// Custom hook for React
export const useRSocketNotifications = (customerId: string) => {
  const [notifications, setNotifications] = useState<Notification[]>([]);

  useEffect(() => {
    const subscription = client.connect().then(socket => {
      return socket.requestStream({
        data: { customerId }
      }).subscribe({
        onNext: event => {
          setNotifications(prev => [...prev, event.data]);
        }
      });
    });

    return () => subscription.dispose();
  }, [customerId]);

  return notifications;
};
```

### Krok 4: Event-driven Architecture (EventPublisher.java)

```java
@Service
public class EventPublisher {

    private final RSocketServer rSocketServer;
    private final FluxProcessor<PaymentEvent, PaymentEvent> paymentProcessor;
    private final FluxProcessor<InvoiceEvent, InvoiceEvent> invoiceProcessor;

    public EventPublisher(RSocketServer rSocketServer) {
        this.rSocketServer = rSocketServer;
        this.paymentProcessor = Sinks.many().multicast().autoConnect().replay();
        this.invoiceProcessor = Sinks.many().multicast().autoConnect().replay();
    }

    public void publishPaymentEvent(PaymentEvent event) {
        paymentProcessor.emitNext(event, Sinks.EmitFailureHandler.FAIL_FAST);

        // Also publish to GraphQL subscriptions
        graphQLPublisher.publish("paymentEvents", event);
    }

    public void publishInvoiceEvent(InvoiceEvent event) {
        invoiceProcessor.emitNext(event, Sinks.EmitFailureHandler.FAIL_FAST);
    }

    // Available for RSocket subscriptions
    public Flux<PaymentEvent> getPaymentEventStream() {
        return paymentProcessor;
    }

    public Flux<InvoiceEvent> getInvoiceEventStream() {
        return invoiceProcessor;
    }
}
```

### Korzyści RSocket

✅ **Bi-directional** - Server może pushować dane do klienta
✅ **Reactive** - Backpressure, flow control
✅ **Multiple patterns** - Request-response, stream, channel
✅ **Low latency** - TCP/WebSocket
✅ **Auto-reconnect** - Built-in resilience
✅ **Real-time UI** - Instant updates, no polling

---

## Podsumowanie Implementacji

| Component | Use Case | Benefit | Implementation Time |
|-----------|----------|---------|-------------------|
| **GraphQL** | Frontend data fetching | 70% less requests, type-safety | 2-3 days |
| **RSocket** | Real-time notifications | Instant updates, bi-directional | 1-2 days |
| **Native** | Production performance | 100x faster startup, 5x less memory | 3-5 days |
| **DevTools** | Development experience | Auto-reload, faster builds | 30 minutes |
| **Config Metadata** | Better IDE support | Auto-completion, validation | 1 hour |

**Total estimated time: 6-11 dni** dla pełnej implementacji wszystkich komponentów.

**ROI:** Lepszy UX, 10x performance, 5x mniejszy footprint, real-time capabilities, 70% mniej HTTP requests.

---

## Następne Kroki

1. **Wybierz priorytet** - Rozpocznij od GraphQL (najwyższy ROI)
2. **Setup build pipeline** - Maven/Gradle configuration
3. **Test w dev** - Sprawdź compatibility
4. **Migrate gradually** - Niekrytyczne feature'y najpierw
5. **Monitor performance** - Prometheus, Grafana
6. **Deploy to production** - A/B testing

**Czy chcesz, żebym zaczął implementację któregoś z tych komponentów?**

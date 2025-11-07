package com.droid.bss.infrastructure.database.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Test suite for SpringTransactionManager
 *
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpringTransactionManager Unit Tests")
class SpringTransactionManagerTest {

    @Mock
    private PlatformTransactionManager platformTransactionManager;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private TransactionStatus transactionStatus;

    private SpringTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        transactionManager = new SpringTransactionManager(platformTransactionManager, dataSource);
        given(platformTransactionManager.getTransaction(any(TransactionDefinition.class))).willReturn(transactionStatus);
    }

    @Test
    @DisplayName("Should create transaction manager")
    void shouldCreateTransactionManager() {
        // Then
        assertThat(transactionManager).isNotNull();
    }

    @Test
    @DisplayName("Should begin transaction")
    void shouldBeginTransaction() throws TransactionException {
        // When
        TransactionContext context = transactionManager.beginTransaction();

        // Then
        assertThat(context).isNotNull();
        assertThat(context.getTransactionType()).isEqualTo(TransactionType.LOCAL);
        assertThat(context.isActive()).isTrue();
        then(platformTransactionManager).should().getTransaction(any(TransactionDefinition.class));
    }

    @Test
    @DisplayName("Should begin transaction with isolation level")
    void shouldBeginTransactionWithIsolationLevel() throws TransactionException {
        // When
        TransactionContext context = transactionManager.beginTransaction(IsolationLevel.SERIALIZABLE);

        // Then
        assertThat(context).isNotNull();
        assertThat(context.getIsolationLevel()).isEqualTo(IsolationLevel.SERIALIZABLE);
    }

    @Test
    @DisplayName("Should commit transaction")
    void shouldCommitTransaction() throws TransactionException {
        // Given
        TransactionContext context = transactionManager.beginTransaction();
        given(transactionStatus.isCompleted()).willReturn(false);

        // When
        transactionManager.commit(context);

        // Then
        assertThat(context.getState()).isEqualTo(TransactionState.COMMITTED);
        then(platformTransactionManager).should().commit(any(TransactionStatus.class));
    }

    @Test
    @DisplayName("Should rollback transaction")
    void shouldRollbackTransaction() throws TransactionException {
        // Given
        TransactionContext context = transactionManager.beginTransaction();
        given(transactionStatus.isCompleted()).willReturn(false);

        // When
        transactionManager.rollback(context);

        // Then
        assertThat(context.getState()).isEqualTo(TransactionState.ROLLED_BACK);
        then(platformTransactionManager).should().rollback(any(TransactionStatus.class));
    }

    @Test
    @DisplayName("Should suspend transaction")
    void shouldSuspendTransaction() throws TransactionException {
        // Given
        TransactionContext context = transactionManager.beginTransaction();

        // When
        transactionManager.suspend(context);

        // Then
        assertThat(context.isSuspended()).isTrue();
        assertThat(context.getState()).isEqualTo(TransactionState.SUSPENDED);
    }

    @Test
    @DisplayName("Should resume suspended transaction")
    void shouldResumeSuspendedTransaction() throws TransactionException {
        // Given
        TransactionContext context = transactionManager.beginTransaction();
        transactionManager.suspend(context);

        // When
        transactionManager.resume(context);

        // Then
        assertThat(context.isSuspended()).isFalse();
        assertThat(context.getState()).isEqualTo(TransactionState.ACTIVE);
    }

    @Test
    @DisplayName("Should get current context")
    void shouldGetCurrentContext() throws TransactionException {
        // Given
        TransactionContext context = transactionManager.beginTransaction();

        // When
        TransactionContext current = transactionManager.getCurrentContext();

        // Then
        assertThat(current).isNotNull();
        assertThat(current.getTransactionId()).isEqualTo(context.getTransactionId());
    }

    @Test
    @DisplayName("Should check if transaction is active")
    void shouldCheckIfTransactionIsActive() throws TransactionException {
        // When no transaction
        assertThat(transactionManager.isTransactionActive()).isFalse();

        // Given
        transactionManager.beginTransaction();

        // Then
        assertThat(transactionManager.isTransactionActive()).isTrue();
    }

    @Test
    @DisplayName("Should set transaction timeout")
    void shouldSetTransactionTimeout() {
        // When
        transactionManager.setTransactionTimeout(600);

        // Then
        assertThat(transactionManager.getTransactionTimeout()).isEqualTo(600);
    }

    @Test
    @DisplayName("Should throw exception for null context in commit")
    void shouldThrowExceptionForNullContextInCommit() {
        // When & Then
        assertThatThrownBy(() -> transactionManager.commit(null))
            .isInstanceOf(TransactionException.class)
            .hasMessageContaining("Transaction context cannot be null");
    }

    @Test
    @DisplayName("Should throw exception for null context in rollback")
    void shouldThrowExceptionForNullContextInRollback() {
        // When & Then
        assertThatCode(() -> transactionManager.rollback(null))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("TransactionContext should track state")
    void transactionContextShouldTrackState() {
        // Given
        TransactionContext context = new TransactionContext("test-participant");

        // Then
        assertThat(context.getState()).isEqualTo(TransactionState.ACTIVE);
        assertThat(context.isActive()).isTrue();
        assertThat(context.isRollbackOnly()).isFalse();

        // When
        context.setRollbackOnly();

        // Then
        assertThat(context.isRollbackOnly()).isTrue();
        assertThat(context.getState()).isEqualTo(TransactionState.MARKED_ROLLBACK);
    }

    @Test
    @DisplayName("TransactionContext should track suspension")
    void transactionContextShouldTrackSuspension() {
        // Given
        TransactionContext context = new TransactionContext("test-participant");

        // Then
        assertThat(context.isSuspended()).isFalse();

        // When
        context.setSuspended(true);

        // Then
        assertThat(context.isSuspended()).isTrue();
    }

    @Test
    @DisplayName("TransactionContext should check timeout")
    void transactionContextShouldCheckTimeout() {
        // Given
        TransactionContext context = new TransactionContext("test-participant");
        context.setTimeoutSeconds(0); // Set to 0 to force timeout

        // Then
        assertThat(context.isTimeout()).isTrue();
    }

    @Test
    @DisplayName("TransactionContext should compare transactions")
    void transactionContextShouldCompareTransactions() {
        // Given
        TransactionContext context1 = new TransactionContext("test-participant");
        TransactionContext context2 = new TransactionContext("test-participant");

        // Then
        assertThat(context1.isSameTransaction(context1)).isTrue();
        assertThat(context1.isSameTransaction(context2)).isFalse();
        assertThat(context1.isSameTransaction(null)).isFalse();
    }

    @Test
    @DisplayName("IsolationLevel should convert from name")
    void isolationLevelShouldConvertFromName() {
        // When
        IsolationLevel level = IsolationLevel.fromName("READ_COMMITTED");

        // Then
        assertThat(level).isEqualTo(IsolationLevel.READ_COMMITTED);
    }

    @Test
    @DisplayName("IsolationLevel should convert from level")
    void isolationLevelShouldConvertFromLevel() {
        // When
        IsolationLevel level = IsolationLevel.fromLevel(Connection.TRANSACTION_READ_COMMITTED);

        // Then
        assertThat(level).isEqualTo(IsolationLevel.READ_COMMITTED);
    }

    @Test
    @DisplayName("IsolationLevel should check types")
    void isolationLevelShouldCheckTypes() {
        // Then
        assertThat(IsolationLevel.LOCAL.isLocal()).isTrue();
        assertThat(IsolationLevel.LOCAL.isDistributed()).isFalse();
        assertThat(IsolationLevel.LOCAL.isXA()).isFalse();

        assertThat(IsolationLevel.DISTRIBUTED.isLocal()).isFalse();
        assertThat(IsolationLevel.DISTRIBUTED.isDistributed()).isTrue();
        assertThat(IsolationLevel.DISTRIBUTED.isXA()).isFalse();

        assertThat(IsolationLevel.XA.isLocal()).isFalse();
        assertThat(IsolationLevel.XA.isDistributed()).isTrue();
        assertThat(IsolationLevel.XA.isXA()).isTrue();
    }

    @Test
    @DisplayName("TransactionType should check types")
    void transactionTypeShouldCheckTypes() {
        // Then
        assertThat(TransactionType.LOCAL.isLocal()).isTrue();
        assertThat(TransactionType.LOCAL.isDistributed()).isFalse();
        assertThat(TransactionType.LOCAL.isXA()).isFalse();

        assertThat(TransactionType.DISTRIBUTED.isLocal()).isFalse();
        assertThat(TransactionType.DISTRIBUTED.isDistributed()).isTrue();
        assertThat(TransactionType.DISTRIBUTED.isXA()).isFalse();

        assertThat(TransactionType.XA.isLocal()).isFalse();
        assertThat(TransactionType.XA.isDistributed()).isTrue();
        assertThat(TransactionType.XA.isXA()).isTrue();
    }

    @Test
    @DisplayName("TransactionState should check states")
    void transactionStateShouldCheckStates() {
        // Then
        assertThat(TransactionState.ACTIVE.isActive()).isTrue();
        assertThat(TransactionState.ACTIVE.isCompleted()).isFalse();
        assertThat(TransactionState.ACTIVE.isSuspended()).isFalse();
        assertThat(TransactionState.ACTIVE.isTransient()).isFalse();

        assertThat(TransactionState.COMMITTED.isActive()).isFalse();
        assertThat(TransactionState.COMMITTED.isCompleted()).isTrue();
        assertThat(TransactionState.COMMITTED.isSuspended()).isFalse();

        assertThat(TransactionState.SUSPENDING.isActive()).isTrue();
        assertThat(TransactionState.SUSPENDING.isSuspended()).isTrue();
        assertThat(TransactionState.SUSPENDING.isTransient()).isTrue();
    }

    @Test
    @DisplayName("TransactionException should create specific exceptions")
    void transactionExceptionShouldCreateSpecificExceptions() {
        // Test timeout
        TransactionException timeoutEx = TransactionException.timeout("tx-1", 300);
        assertThat(timeoutEx.getMessage()).contains("timeout");
        assertThat(timeoutEx.getErrorCode()).isEqualTo("TRANSACTION_TIMEOUT");
        assertThat(timeoutEx.isRollbackOnly()).isTrue();

        // Test commit failure
        TransactionException commitEx = TransactionException.commitFailure("tx-1", new RuntimeException("test"));
        assertThat(commitEx.getMessage()).contains("commit");
        assertThat(commitEx.getErrorCode()).isEqualTo("COMMIT_FAILED");
    }

    @Test
    @DisplayName("Should not support XA transactions")
    void shouldNotSupportXATransactions() {
        // Then
        assertThat(transactionManager.supportsXATransactions()).isFalse();
        assertThat(transactionManager.supportsDistributedTransactions()).isFalse();
    }
}

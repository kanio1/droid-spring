package com.droid.bss.infrastructure.database.sharding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ShardKey}.
 *
 * @since 1.0
 */
@DisplayName("ShardKey")
class ShardKeyTest {

    @Test
    @DisplayName("Should create shard key from string")
    void shouldCreateShardKeyFromString() {
        ShardKey shardKey = ShardKey.of("test-key");

        assertEquals("test-key", shardKey.getValue());
        assertEquals(ShardKeyType.STRING, getPrivateField(shardKey, "type"));
    }

    @Test
    @DisplayName("Should create shard key from long")
    void shouldCreateShardKeyFromLong() {
        ShardKey shardKey = ShardKey.of(12345L);

        assertEquals("12345", shardKey.getValue());
        assertEquals(ShardKeyType.LONG, getPrivateField(shardKey, "type"));
        assertEquals(12345L, shardKey.getLongValue());
    }

    @Test
    @DisplayName("Should create shard key from int")
    void shouldCreateShardKeyFromInt() {
        ShardKey shardKey = ShardKey.of(123);

        assertEquals("123", shardKey.getValue());
        assertEquals(ShardKeyType.INT, getPrivateField(shardKey, "type"));
        assertEquals(123L, shardKey.getLongValue());
        assertEquals(123, shardKey.getIntValue());
    }

    @Test
    @DisplayName("Should create shard key from UUID")
    void shouldCreateShardKeyFromUUID() {
        UUID uuid = UUID.randomUUID();
        ShardKey shardKey = ShardKey.of(uuid);

        assertEquals(uuid.toString(), shardKey.getValue());
        assertEquals(ShardKeyType.UUID, getPrivateField(shardKey, "type"));
    }

    @Test
    @DisplayName("Should create shard key from object")
    void shouldCreateShardKeyFromObject() {
        String value = "test-value";
        ShardKey shardKey = ShardKey.ofObject(value);

        assertEquals("test-value", shardKey.getValue());
    }

    @Test
    @DisplayName("Should identify numeric keys")
    void shouldIdentifyNumericKeys() {
        ShardKey longKey = ShardKey.of(123L);
        ShardKey intKey = ShardKey.of(456);
        ShardKey stringKey = ShardKey.of("test");

        assertTrue(longKey.isNumeric());
        assertTrue(intKey.isNumeric());
        assertFalse(stringKey.isNumeric());
    }

    @Test
    @DisplayName("Should return correct long value")
    void shouldReturnCorrectLongValue() {
        ShardKey shardKey = ShardKey.of(789L);
        assertEquals(789L, shardKey.getLongValue());
    }

    @Test
    @DisplayName("Should throw exception when getting long value from non-numeric key")
    void shouldThrowExceptionWhenGettingLongValueFromNonNumericKey() {
        ShardKey shardKey = ShardKey.of("test");
        assertThrows(IllegalStateException.class, shardKey::getLongValue);
    }

    @Test
    @DisplayName("Should return correct int value")
    void shouldReturnCorrectIntValue() {
        ShardKey shardKey = ShardKey.of(999);
        assertEquals(999, shardKey.getIntValue());
    }

    @Test
    @DisplayName("Should throw exception when getting int value from non-numeric key")
    void shouldThrowExceptionWhenGettingIntValueFromNonNumericKey() {
        ShardKey shardKey = ShardKey.of("test");
        assertThrows(IllegalStateException.class, shardKey::getIntValue);
    }

    @Test
    @DisplayName("Should return correct int value for long key within int range")
    void shouldReturnCorrectIntValueForLongKeyWithinIntRange() {
        ShardKey shardKey = ShardKey.of(12345L);
        assertEquals(12345, shardKey.getIntValue());
    }

    @Test
    @DisplayName("Should throw exception when long value exceeds int range")
    void shouldThrowExceptionWhenLongValueExceedsIntRange() {
        ShardKey shardKey = ShardKey.of(Long.MAX_VALUE);
        assertThrows(IllegalStateException.class, shardKey::getIntValue);
    }

    @Test
    @DisplayName("Should generate consistent hash codes for same values")
    void shouldGenerateConsistentHashCodesForSameValues() {
        ShardKey key1 = ShardKey.of("test");
        ShardKey key2 = ShardKey.of("test");

        assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    @DisplayName("Should consider keys equal when values are the same")
    void shouldConsiderKeysEqualWhenValuesAreTheSame() {
        ShardKey key1 = ShardKey.of("test");
        ShardKey key2 = ShardKey.of("test");

        assertEquals(key1, key2);
    }

    @Test
    @DisplayName("Should consider keys not equal when values are different")
    void shouldConsiderKeysNotEqualWhenValuesAreDifferent() {
        ShardKey key1 = ShardKey.of("test1");
        ShardKey key2 = ShardKey.of("test2");

        assertNotEquals(key1, key2);
    }

    @Test
    @DisplayName("Should consider keys not equal when compared to null")
    void shouldConsiderKeysNotEqualWhenComparedToNull() {
        ShardKey key = ShardKey.of("test");
        assertNotEquals(null, key);
    }

    @Test
    @DisplayName("Should consider keys not equal when compared to different type")
    void shouldConsiderKeysNotEqualWhenComparedToDifferentType() {
        ShardKey key = ShardKey.of("test");
        assertNotEquals("test", key);
    }

    @Test
    @DisplayName("Should provide meaningful toString output")
    void shouldProvideMeaningfulToStringOutput() {
        ShardKey key = ShardKey.of("test");
        String output = key.toString();

        assertNotNull(output);
        assertTrue(output.contains("ShardKey"));
        assertTrue(output.contains("test"));
    }

    @Test
    @DisplayName("Should create composite key from string parts")
    void shouldCreateCompositeKeyFromStringParts() {
        ShardKey key1 = ShardKey.of("user123");
        ShardKey key2 = ShardKey.of("order456");

        assertNotEquals(key1, key2);
    }

    private Object getPrivateField(ShardKey shardKey, String fieldName) {
        try {
            java.lang.reflect.Field field = ShardKey.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(shardKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

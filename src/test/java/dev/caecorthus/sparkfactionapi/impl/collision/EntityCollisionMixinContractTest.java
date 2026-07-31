package dev.caecorthus.sparkfactionapi.impl.collision;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityCollisionMixinContractTest {
    @Test
    void requiredMixinTargetsPairAwareEntityCollision() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/mixin/EntityCollisionMixin.java"
        ));
        String config = Files.readString(Path.of("src/main/resources/sparkfactionapi.mixins.json"));

        assertTrue(source.contains("@Mixin(value = Entity.class, priority = 1100)"));
        assertTrue(source.contains("@WrapMethod(method = \"collidesWith(Lnet/minecraft/entity/Entity;)Z\")"));
        assertTrue(source.contains("Operation<Boolean> original"));
        assertTrue(source.contains("EntityCollisionExemptions.shouldCancelCollision"));
        assertTrue(source.contains("return false;"));
        assertTrue(source.contains("return original.call(other);"));
        assertFalse(source.contains("@Inject("));
        assertFalse(source.contains("method = \"isCollidable"));
        assertFalse(source.contains("method = \"isPushable"));
        assertTrue(config.contains("\"EntityCollisionMixin\""));
        assertTrue(config.contains("\"defaultRequire\": 1"));
    }
}

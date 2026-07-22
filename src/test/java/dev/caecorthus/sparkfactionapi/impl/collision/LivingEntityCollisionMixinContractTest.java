package dev.caecorthus.sparkfactionapi.impl.collision;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LivingEntityCollisionMixinContractTest {
    @Test
    void requiredMixinTargetsPairAwarePushMethod() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/mixin/LivingEntityCollisionMixin.java"
        ));
        String config = Files.readString(Path.of("src/main/resources/sparkfactionapi.mixins.json"));

        assertTrue(source.contains("method = \"pushAway(Lnet/minecraft/entity/Entity;)V\""));
        assertTrue(source.contains("at = @At(\"HEAD\")"));
        assertTrue(source.contains("cancellable = true"));
        assertTrue(source.contains("EntityCollisionExemptions.shouldCancelPush"));
        assertTrue(config.contains("\"LivingEntityCollisionMixin\""));
        assertTrue(config.contains("\"defaultRequire\": 1"));
    }
}

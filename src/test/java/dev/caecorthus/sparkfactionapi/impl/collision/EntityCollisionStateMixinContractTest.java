package dev.caecorthus.sparkfactionapi.impl.collision;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityCollisionStateMixinContractTest {
    @Test
    void tracksServerAuthoritativeCollisionExemptionForAllObservers() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/mixin/EntityCollisionStateMixin.java"
        ));
        String exemptions = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/collision/EntityCollisionExemptions.java"
        ));
        String beforeTracking = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/mixin/ServerEntityHandlerCollisionMixin.java"
        ));
        String config = Files.readString(Path.of("src/main/resources/sparkfactionapi.mixins.json"));

        assertTrue(source.contains("DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.BOOLEAN)"));
        assertTrue(source.contains("method = \"<init>\""));
        assertTrue(source.contains("DataTracker$Builder;build()Lnet/minecraft/entity/data/DataTracker;"));
        assertTrue(source.contains("builder.add(SPARKFACTIONAPI_COLLISION_EXEMPT, false)"));
        assertTrue(source.contains("return original.call(builder)"));
        assertTrue(source.contains("method = \"tick()V\""));
        assertTrue(source.contains("EntityCollisionExemptions.refresh((Entity) (Object) this)"));
        assertTrue(exemptions.contains("return entity instanceof EntityCollisionTrackedState trackedState"));
        assertTrue(exemptions.contains("trackedState.sparkfactionapi$isCollisionExempt()"));
        assertTrue(exemptions.contains("entity.getWorld() instanceof ServerWorld"));
        assertTrue(exemptions.contains("trackedState.sparkfactionapi$isCollisionExempt() != exempt"));
        assertTrue(beforeTracking.contains("method = \"startTracking(Lnet/minecraft/entity/Entity;)V\""));
        assertTrue(beforeTracking.contains("at = @At(\"HEAD\")"));
        assertTrue(beforeTracking.contains("EntityCollisionExemptions.refresh(entity)"));
        assertTrue(config.contains("\"EntityCollisionStateMixin\""));
        assertTrue(config.contains("\"ServerEntityHandlerCollisionMixin\""));
    }
}

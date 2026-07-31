package dev.caecorthus.sparkfactionapi.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.caecorthus.sparkfactionapi.impl.collision.EntityCollisionExemptions;
import dev.caecorthus.sparkfactionapi.impl.collision.EntityCollisionTrackedState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityCollisionStateMixin implements EntityCollisionTrackedState {
    @Unique
    private static final TrackedData<Boolean> SPARKFACTIONAPI_COLLISION_EXEMPT =
            DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.BOOLEAN);

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/data/DataTracker$Builder;build()Lnet/minecraft/entity/data/DataTracker;"
            )
    )
    private DataTracker sparkfactionapi$trackCollisionExemption(
            DataTracker.Builder builder,
            Operation<DataTracker> original
    ) {
        builder.add(SPARKFACTIONAPI_COLLISION_EXEMPT, false);
        return original.call(builder);
    }

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void sparkfactionapi$refreshCollisionExemption(CallbackInfo ci) {
        EntityCollisionExemptions.refresh((Entity) (Object) this);
    }

    @Override
    public boolean sparkfactionapi$isCollisionExempt() {
        return ((Entity) (Object) this).getDataTracker().get(SPARKFACTIONAPI_COLLISION_EXEMPT);
    }

    @Override
    public void sparkfactionapi$setCollisionExempt(boolean exempt) {
        ((Entity) (Object) this).getDataTracker().set(SPARKFACTIONAPI_COLLISION_EXEMPT, exempt);
    }
}

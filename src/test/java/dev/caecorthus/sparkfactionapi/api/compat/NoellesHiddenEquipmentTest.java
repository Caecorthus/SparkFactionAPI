package dev.caecorthus.sparkfactionapi.api.compat;

import net.minecraft.item.Item;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class NoellesHiddenEquipmentTest {
    private static final String PUBLIC_API =
            "dev.caecorthus.sparkfactionapi.api.compat.NoellesHiddenEquipment";
    private static final String INTERNAL_REGISTRY =
            "dev.caecorthus.sparkfactionapi.impl.compat.noellesroles.NoellesHiddenEquipmentRegistry";

    @Test
    void publicCompatibilityApiOnlyRequiresItemRegistration() throws Exception {
        Class<?> publicApi = requireClass(PUBLIC_API);
        Method register = publicApi.getMethod("register", Item.class);

        assertEquals(void.class, register.getReturnType());
        assertEquals(1, publicApi.getDeclaredMethods().length);
    }

    @Test
    void registryUsesItemIdentityAndIgnoresEmptyStacks() throws Exception {
        requireClass(INTERNAL_REGISTRY);
        Path registrySource = Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/compat/noellesroles/NoellesHiddenEquipmentRegistry.java"
        );
        String registry = Files.readString(registrySource);

        assertTrue(registry.contains("new IdentityHashMap<>()"));
        assertTrue(registry.contains("Objects.requireNonNull(item"));
        assertTrue(registry.contains("!stack.isEmpty()"));
        assertTrue(registry.contains("HIDDEN_ITEMS.contains(stack.getItem())"));
    }

    @Test
    void optionalMixinExtendsNoellesResultWithoutAddingAHardDependency() throws Exception {
        Path mixinSource = Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/mixin/compat/noellesroles/NoellesHiddenEquipmentHelperMixin.java"
        );
        assertTrue(Files.isRegularFile(mixinSource), "missing optional Noelles hidden-equipment mixin");

        String mixin = Files.readString(mixinSource);
        assertTrue(mixin.contains("@Pseudo"));
        assertTrue(mixin.contains("org.agmas.noellesroles.util.HiddenEquipmentHelper"));
        assertTrue(mixin.contains("@At(\"RETURN\")"));
        assertTrue(mixin.contains("require = 0"));
        assertTrue(mixin.contains("getReturnValueZ()"));
        assertTrue(mixin.contains("NoellesHiddenEquipmentRegistry.shouldHide(stack)"));

        String mixinConfig = Files.readString(Path.of("src/main/resources/sparkfactionapi.mixins.json"));
        assertTrue(mixinConfig.contains("compat.noellesroles.NoellesHiddenEquipmentHelperMixin"));

        String metadata = Files.readString(Path.of("src/main/resources/fabric.mod.json"));
        assertFalse(metadata.contains("\"noellesroles\""));
    }

    private static Class<?> requireClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            fail("missing compatibility class " + className);
            throw new AssertionError(exception);
        }
    }
}

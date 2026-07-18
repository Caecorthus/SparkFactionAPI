package dev.caecorthus.sparkfactionapi.impl.target;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerAffectAdapterContractTest {
    private static final Path MIXIN_ROOT = Path.of(
            "src/main/java/dev/caecorthus/sparkfactionapi/mixin"
    );

    @Test
    void watheWeaponReceiversCheckPolicyAtTheHead() throws IOException {
        String knife = source("KnifeStabPayloadReceiverMixin.java");
        String gun = source("GunShootPayloadReceiverMixin.java");

        assertHeadVeto(knife);
        assertTrue(knife.contains(
                "method = \"receive(Ldev/doctor4t/wathe/util/KnifeStabPayload;"
                        + "Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V\""
        ));
        assertTrue(knife.contains("GameConstants.DeathReasons.KNIFE"));
        assertHeadVeto(gun);
        assertTrue(gun.contains(
                "method = \"receive(Ldev/doctor4t/wathe/util/GunShootPayload;"
                        + "Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V\""
        ));
        assertTrue(gun.contains("GameConstants.DeathReasons.GUN"));
    }

    @Test
    void serverAttackGuardCoversVanillaAndWatheBatBeforeEffects() throws IOException {
        String attack = source("ServerPlayerEntityAffectMixin.java");

        assertHeadVeto(attack);
        assertTrue(attack.contains("method = \"attack\""));
        assertTrue(attack.contains("GameConstants.DeathReasons.BAT"));
        assertTrue(attack.contains("Identifier.of(\"minecraft\", \"attack\")"));
    }

    @Test
    void serverDamageGuardCoversPlayerAttributedProjectilesAndAreaDamage() throws IOException {
        String damage = source("ServerPlayerEntityAffectMixin.java");

        assertTrue(damage.contains("method = \"damage\""));
        assertTrue(damage.contains("source.getAttacker() instanceof PlayerEntity actor"));
        assertTrue(damage.contains("Identifier.of(\"minecraft\", \"damage\")"));
        assertTrue(damage.contains("cir.setReturnValue(false)"));
    }

    @Test
    void bothVanillaUseEntityPathsReturnFailOnDenial() throws IOException {
        for (String file : List.of("PlayerEntityAffectMixin.java", "EntityAffectMixin.java")) {
            String interaction = source(file);

            assertHeadVeto(interaction);
            assertTrue(interaction.contains("Identifier.of(\"minecraft\", \"use_entity\")"));
            assertTrue(interaction.contains("ActionResult.FAIL"));
        }
    }

    @Test
    void everyAdapterIsRegisteredInTheCommonMixinConfig() throws IOException {
        String config = Files.readString(Path.of("src/main/resources/sparkfactionapi.mixins.json"));

        for (String mixin : List.of(
                "KnifeStabPayloadReceiverMixin",
                "GunShootPayloadReceiverMixin",
                "ServerPlayerEntityAffectMixin",
                "PlayerEntityAffectMixin",
                "EntityAffectMixin"
        )) {
            assertTrue(config.contains("\"" + mixin + "\""), "missing mixin " + mixin);
        }
    }

    private static String source(String name) throws IOException {
        Path path = MIXIN_ROOT.resolve(name);
        assertTrue(Files.isRegularFile(path), "missing adapter " + name);
        return Files.readString(path);
    }

    private static void assertHeadVeto(String source) {
        assertTrue(source.contains("@At(\"HEAD\")"));
        assertTrue(source.contains("cancellable = true"));
        assertTrue(source.contains("SparkFactionApi.canAffectPlayer"));
    }
}

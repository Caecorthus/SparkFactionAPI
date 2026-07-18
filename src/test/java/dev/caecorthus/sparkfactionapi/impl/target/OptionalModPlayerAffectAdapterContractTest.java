package dev.caecorthus.sparkfactionapi.impl.target;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptionalModPlayerAffectAdapterContractTest {
    private static final Path SOURCE_ROOT = Path.of("src/main/java/dev/caecorthus/sparkfactionapi");
    private static final Path MIXIN_ROOT = SOURCE_ROOT.resolve("mixin");

    @Test
    void sharedGuardStaysOutsideTheConfiguredMixinPackage() throws IOException {
        Path guard = SOURCE_ROOT.resolve("impl/target/PlayerAffectMixinGuard.java");

        assertTrue(Files.isRegularFile(guard));
        assertTrue(Files.readString(guard)
                .contains("package dev.caecorthus.sparkfactionapi.impl.target;"));
        assertFalse(Files.exists(MIXIN_ROOT.resolve("compat/PlayerAffectMixinGuard.java")));
        try (var files = Files.walk(MIXIN_ROOT)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertFalse(Files.readString(file)
                        .contains("dev.caecorthus.sparkfactionapi.mixin.compat.PlayerAffectMixinGuard"), file.toString());
            }
        }
    }

    @Test
    void optionalAdaptersStayInsideFactionApiAndUsePseudoTargets() throws IOException {
        for (String file : adapterFiles()) {
            String source = source(file);

            assertTrue(source.contains("@Pseudo"), file);
            assertTrue(source.contains("targets ="), file);
            assertTrue(source.contains("PlayerAffectMixinGuard"), file);
            assertTrue(source.contains("require = 0, remap = false"), file);
            assertFalse(source.contains("import org.agmas.noellesroles"), file);
            assertFalse(source.contains("import dev.caecorthus.sparkwitch"), file);
            assertFalse(source.contains("import annina.sparkstrength"), file);
        }
    }

    @Test
    void noellesAdaptersCoverPacketsAndGasWhileGenericProjectileGuardCoversAxes() throws IOException {
        assertContains("compat/noellesroles/NoellesRolesPacketAffectMixin.java",
                "lambda$registerPackets$0", "lambda$registerPackets$4");
        assertContains("compat/noellesroles/NoellesRolesDemonHunterAffectMixin.java", "receive");
        assertContains("compat/noellesroles/NoellesRolesPoisonGasAffectMixin.java", "isInGas");

        String projectileGuard = source("WorldProjectileAffectMixin.java");
        assertTrue(projectileGuard.contains("World.class"));
        assertTrue(projectileGuard.contains("ProjectileEntity projectile"));
        assertTrue(projectileGuard.contains("method = \"getOtherEntities\""));
        assertTrue(projectileGuard.contains("PlayerAffectMixinGuard.allows"));
    }

    @Test
    void witchAndStrengthAdaptersGuardBeforeTheirPublicMutationEntrypoints() throws IOException {
        assertContains("compat/sparkwitch/WitchSkillUseAffectMixin.java", "method = \"use\"");
        assertContains("compat/sparkwitch/SparkWitchCombatAffectMixin.java", "tryHandleAttack");
        assertContains("compat/sparkstrength/NoisemakerAffectMixin.java", "tryUseBackpackGlow");
        assertContains("compat/sparkstrength/ProfessorSerumAffectMixin.java",
                "useHeldSerumOnTarget", "tryRemoteFeed");
        assertContains("compat/sparkstrength/CriminologistAffectMixin.java", "handleSelection");
        assertContains("compat/sparkstrength/VeteranKnifeAffectMixin.java", "handleKnifeStab");
    }

    @Test
    void allOptionalAdaptersAndTheGenericKillGuardAreRegistered() throws IOException {
        String config = Files.readString(Path.of("src/main/resources/sparkfactionapi.mixins.json"));

        assertTrue(config.contains("\"GameFunctionsPlayerAffectMixin\""));
        assertTrue(config.contains("\"WorldProjectileAffectMixin\""));
        assertFalse(config.contains("NoellesRolesThrowingAxeAffectMixin"));
        for (String file : adapterFiles()) {
            String mixin = file.substring(0, file.length() - ".java".length()).replace('/', '.');
            assertTrue(config.contains("\"" + mixin + "\""), "missing mixin " + mixin);
        }
    }

    private static List<String> adapterFiles() {
        return List.of(
                "compat/noellesroles/NoellesRolesPacketAffectMixin.java",
                "compat/noellesroles/NoellesRolesDemonHunterAffectMixin.java",
                "compat/noellesroles/NoellesRolesPoisonGasAffectMixin.java",
                "compat/sparkwitch/WitchSkillUseAffectMixin.java",
                "compat/sparkwitch/SparkWitchCombatAffectMixin.java",
                "compat/sparkstrength/NoisemakerAffectMixin.java",
                "compat/sparkstrength/ProfessorSerumAffectMixin.java",
                "compat/sparkstrength/CriminologistAffectMixin.java",
                "compat/sparkstrength/VeteranKnifeAffectMixin.java"
        );
    }

    private static void assertContains(String file, String... fragments) throws IOException {
        String source = source(file);
        for (String fragment : fragments) {
            assertTrue(source.contains(fragment), () -> file + " is missing " + fragment);
        }
    }

    private static String source(String relativePath) throws IOException {
        Path path = MIXIN_ROOT.resolve(relativePath);
        assertTrue(Files.isRegularFile(path), "missing adapter " + relativePath);
        return Files.readString(path);
    }
}

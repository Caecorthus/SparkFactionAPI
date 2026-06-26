package dev.caecorthus.sparkfactionapi.component;

import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.scoreboard.ScoreboardComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.scoreboard.ScoreboardComponentInitializer;

public final class SparkFactionComponents implements ScoreboardComponentInitializer {
    @Override
    public void registerScoreboardComponentFactories(@NotNull ScoreboardComponentFactoryRegistry registry) {
        registry.registerScoreboardComponent(SparkFactionRoundEndComponent.KEY, SparkFactionRoundEndComponent::new);
    }
}

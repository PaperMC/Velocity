package com.velocitypowered.api.util.title;

import com.google.common.base.Preconditions;
import net.kyori.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import java.util.Optional;

public class TextTitle implements Title {
    private final Component title;
    private final Component subtitle;
    private final int stay;
    private final int fadeIn;
    private final int fadeOut;
    private final boolean resetBeforeSend;

    private TextTitle(Builder builder) {
        this.title = builder.title;
        this.subtitle = builder.subtitle;
        this.stay = builder.stay;
        this.fadeIn = builder.fadeIn;
        this.fadeOut = builder.fadeOut;
        this.resetBeforeSend = builder.resetBeforeSend;
    }

    public Optional<Component> getTitle() {
        return Optional.ofNullable(title);
    }

    public Optional<Component> getSubtitle() {
        return Optional.ofNullable(subtitle);
    }

    public int getStay() {
        return stay;
    }

    public int getFadeIn() {
        return fadeIn;
    }

    public int getFadeOut() {
        return fadeOut;
    }

    public boolean isResetBeforeSend() {
        return resetBeforeSend;
    }

    public boolean areTimesSet() {
        return stay != 0 || fadeIn != 0 || fadeOut != 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextTitle textTitle = (TextTitle) o;
        return stay == textTitle.stay &&
                fadeIn == textTitle.fadeIn &&
                fadeOut == textTitle.fadeOut &&
                resetBeforeSend == textTitle.resetBeforeSend &&
                Objects.equals(title, textTitle.title) &&
                Objects.equals(subtitle, textTitle.subtitle);
    }

    @Override
    public String toString() {
        return "TextTitle{" +
                "title=" + title +
                ", subtitle=" + subtitle +
                ", stay=" + stay +
                ", fadeIn=" + fadeIn +
                ", fadeOut=" + fadeOut +
                ", resetBeforeSend=" + resetBeforeSend +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, subtitle, stay, fadeIn, fadeOut, resetBeforeSend);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private @Nullable Component title;
        private @Nullable Component subtitle;
        private int stay;
        private int fadeIn;
        private int fadeOut;
        private boolean resetBeforeSend;

        private Builder() {}

        public Builder title(Component title) {
            this.title = Preconditions.checkNotNull(title, "title");
            return this;
        }

        public Builder clearTitle() {
            this.title = null;
            return this;
        }

        public Builder subtitle(Component subtitle) {
            this.subtitle = Preconditions.checkNotNull(subtitle, "subtitle");
            return this;
        }

        public Builder clearSubtitle() {
            this.subtitle = null;
            return this;
        }

        public Builder stay(int ticks) {
            Preconditions.checkArgument(ticks >= 0, "ticks value %s is negative", ticks);
            this.stay = ticks;
            return this;
        }

        public Builder fadeIn(int ticks) {
            Preconditions.checkArgument(ticks >= 0, "ticks value %s is negative", ticks);
            this.fadeIn = ticks;
            return this;
        }

        public Builder fadeOut(int ticks) {
            Preconditions.checkArgument(ticks >= 0, "ticks value %s is negative", ticks);
            this.fadeOut = ticks;
            return this;
        }

        public Builder resetBeforeSend(boolean b) {
            this.resetBeforeSend = b;
            return this;
        }

        public Component getTitle() {
            return title;
        }

        public Component getSubtitle() {
            return subtitle;
        }

        public int getStay() {
            return stay;
        }

        public int getFadeIn() {
            return fadeIn;
        }

        public int getFadeOut() {
            return fadeOut;
        }

        public boolean isResetBeforeSend() {
            return resetBeforeSend;
        }

        public TextTitle build() {
            return new TextTitle(this);
        }

        @Override
        public String toString() {
            return "Builder{" +
                    "title=" + title +
                    ", subtitle=" + subtitle +
                    ", stay=" + stay +
                    ", fadeIn=" + fadeIn +
                    ", fadeOut=" + fadeOut +
                    ", resetBeforeSend=" + resetBeforeSend +
                    '}';
        }
    }
}

package net.thenextlvl.i18n;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.TagPattern;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.translation.GlobalTranslator;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

final class ComponentBundleImpl implements ComponentBundle {
    private static final Logger LOGGER = LoggerFactory.getLogger("i18n");
    private final Locale fallback;
    private final Map<String, String> placeholders;
    private final MiniMessageTranslationStore translator;

    private ComponentBundleImpl(Locale fallback, Map<String, String> placeholders, MiniMessageTranslationStore translator) {
        this.fallback = fallback;
        this.placeholders = Map.copyOf(placeholders);
        this.translator = translator;
    }

    @Override
    public MiniMessageTranslationStore translator() {
        return translator;
    }

    @Override
    public ComponentBundleImpl registerTranslations() throws IllegalStateException {
        if (GlobalTranslator.translator().addSource(translator)) return this;
        throw new IllegalStateException("Translation store '" + translator.name() + "' already registered");
    }

    @Override
    public void unregisterTranslations() throws IllegalStateException {
        if (GlobalTranslator.translator().removeSource(translator)) return;
        throw new IllegalStateException("Translation store '" + translator.name() + "' not registered");
    }

    @Override
    public @Nullable Component translate(final String translationKey, final Audience audience, final ComponentLike... arguments) {
        return translate(Component.translatable(translationKey, arguments), audience);
    }

    @Override
    public @Nullable Component translate(final String translationKey, final Audience audience) {
        return translate(translationKey, audience, new Component[0]);
    }

    @Override
    public @Nullable Component translate(final String translationKey, final Locale locale, final ComponentLike... arguments) {
        return translate(Component.translatable(translationKey, arguments), locale);
    }

    @Override
    public @Nullable Component translate(final String translationKey, final Locale locale) {
        return translate(translationKey, locale, new Component[0]);
    }

    @Override
    public @Nullable Component translate(final TranslatableComponent component, final Audience audience) {
        return translate(component, audience.get(Identity.LOCALE).orElse(fallback));
    }

    @Override
    public @Nullable Component translate(TranslatableComponent component, final Locale locale) {
        if (!placeholders.isEmpty()) component = component.arguments(placeholders(locale, component.arguments()));
        return translator.translate(component, locale);
    }

    @SuppressWarnings("PatternValidation")
    private List<ComponentLike> placeholders(final Locale locale, final List<TranslationArgument> arguments) {
        final var result = new ArrayList<ComponentLike>(arguments.size() + placeholders.size());
        for (final var entry : placeholders.entrySet()) {
            final var translated = translator.translate(Component.translatable(entry.getValue()), locale);
            if (translated != null) result.add(Argument.component(entry.getKey(), translated));
            else result.add(Component.text(entry.getValue(), NamedTextColor.RED));
        }
        result.addAll(arguments);
        return result;
    }

    @Override
    public void sendMessage(final Audience audience, final String translationKey) {
        sendMessage(audience, translationKey, new Component[0]);
    }

    @Override
    public void sendMessage(final Audience audience, final String translationKey, final ComponentLike... arguments) {
        final var translated = translate(translationKey, audience, arguments);
        if (translated != null && !Component.empty().equals(translated)) audience.sendMessage(translated);
    }

    @Override
    public void sendMessage(final Audience audience, final String translationKey, final TagResolver... resolver) {
        sendMessage(audience, translationKey, Argument.tagResolver(resolver));
    }

    @Override
    public void sendActionBar(final Audience audience, final String translationKey, final ComponentLike... arguments) {
        final var translated = translate(translationKey, audience, arguments);
        if (translated != null && !Component.empty().equals(translated)) audience.sendActionBar(translated);
    }

    @Override
    public void showTitle(final Audience audience, @Nullable final String title, @Nullable final String subtitle, final Title.@Nullable Times times, final ComponentLike... arguments) {
        final var titleComponent = title != null ? translate(title, audience, arguments) : null;
        final var subtitleComponent = subtitle != null ? translate(subtitle, audience, arguments) : null;
        if (subtitleComponent != null || titleComponent != null) audience.showTitle(Title.title(
                titleComponent != null ? titleComponent : Component.empty(),
                subtitleComponent != null ? subtitleComponent : Component.empty(),
                times
        ));
    }

    @Override
    public void showTitle(final Audience audience, @Nullable final String title, @Nullable final String subtitle, final ComponentLike... arguments) {
        showTitle(audience, title, subtitle, Title.DEFAULT_TIMES, arguments);
    }

    @Override
    public Component component(final String translationKey, final Audience audience) {
        return component(translationKey, audience, new Component[0]);
    }

    @Override
    public Component component(final String translationKey, final Audience audience, final ComponentLike... arguments) {
        final var locale = audience.get(Identity.LOCALE).orElse(fallback);
        return component(translationKey, locale, arguments);
    }

    @Override
    public Component component(final String translationKey, final Audience audience, final TagResolver... resolvers) {
        return component(translationKey, audience, Argument.tagResolver(resolvers));
    }

    @Override
    public Component component(final String translationKey, final Locale locale) {
        return component(translationKey, locale, new Component[0]);
    }

    @Override
    public Component component(final String translationKey, final Locale locale, final ComponentLike... arguments) {
        final var translated = translate(translationKey, locale, arguments);
        return translated != null ? translated : Component.text(translationKey, NamedTextColor.RED);
    }

    @Override
    public Component component(final String translationKey, final Locale locale, final TagResolver... resolvers) {
        return component(translationKey, locale, Argument.tagResolver(resolvers));
    }

    public static final class Builder implements ComponentBundle.Builder {
        private final Map<String, Locale> files = new HashMap<>();
        private final Map<String, String> placeholders = new HashMap<>();

        private @Nullable ResourceMigrator migrator = null;
        private Charset charset = StandardCharsets.UTF_8;
        private Locale fallback = Locale.US;
        private MiniMessage miniMessage = MiniMessage.miniMessage();
        private Scope scope = Scope.FILTER_AND_FILL;

        private Key name;
        private Path path;

        Builder(final Key name, final Path path) {
            this.name = name;
            this.path = path;
        }

        @Override
        public ComponentBundle.Builder charset(final Charset charset) {
            this.charset = charset;
            return this;
        }

        @Override
        public ComponentBundle.Builder fallback(final Locale fallback) {
            this.fallback = fallback;
            return this;
        }

        @Override
        public ComponentBundle.Builder migrator(@Nullable final ResourceMigrator migrator) {
            this.migrator = migrator;
            return this;
        }

        @Override
        public ComponentBundle.Builder miniMessage(final MiniMessage miniMessage) {
            this.miniMessage = miniMessage;
            return this;
        }

        @Override
        public ComponentBundle.Builder name(final Key name) {
            this.name = name;
            return this;
        }

        @Override
        public ComponentBundle.Builder path(final Path path) {
            this.path = path;
            return this;
        }

        @Override
        public Builder resource(final String name, final Locale locale) throws IllegalStateException {
            final var suffix = ".properties";
            final var key = name.endsWith(suffix) ? name : name + suffix;
            if (files.put(key, locale) == null) return this;
            throw new IllegalStateException("Resource '" + key + "' already registered for locale " + locale);
        }

        @Override
        public ComponentBundle.Builder scope(final Scope scope) {
            this.scope = scope;
            return this;
        }

        @Override
        public ComponentBundle.Builder placeholder(@TagPattern final String name, final String translationKey) {
            placeholders.put(name, translationKey);
            return this;
        }

        @Override
        public ComponentBundle build() throws ResourceMigrationException {
            final var registry = MiniMessageTranslationStore.create(name, miniMessage);
            registry.defaultLocale(fallback);
            registerResources(registry);
            return new ComponentBundleImpl(fallback, placeholders, registry, miniMessage);
        }

        private void registerResources(final MiniMessageTranslationStore registry) {
            files.forEach((name, locale) -> {
                try {
                    registry.registerAll(locale, extractResource(name, locale));
                } catch (final IOException e) {
                    LOGGER.error("Failed to register resource '{}' ({})", name, locale, e);
                }
            });
        }

        private @Unmodifiable Map<String, String> extractResource(final String baseName, final Locale locale) throws IOException {
            final var file = new PropertiesFile(path.resolve(baseName), charset, readResource(baseName));

            migrate(baseName, locale, file);

            file.validate(scope);

            if (file.getRoot().isEmpty()) Files.deleteIfExists(file.getFile());
            else file.save();

            final var properties = new HashMap<String, String>(file.getRoot().size());
            file.getRoot().forEach((key, value) -> properties.put(key.toString(), value.toString()));
            return properties;
        }

        private Properties readResource(final String name) throws IOException {
            try (final var resource = getClass().getClassLoader().getResourceAsStream(name)) {
                if (resource != null) return readResource(resource);
                throw new IOException("Resource '" + name + "' not found in classpath");
            }
        }

        private Properties readResource(final InputStream resource) throws IOException {
            try (final var reader = new InputStreamReader(resource, charset);
                 final var buffer = new BufferedReader(reader)) {
                final var properties = new Properties();
                properties.load(buffer);
                return properties;
            }
        }

        private void migrate(final String baseName, final Locale locale, final PropertiesFile file) throws IOException {
            final var oldResource = migrator != null ? migrator.getOldResourceName(locale) : null;

            final var oldPath = migrator != null ? migrator.getOldPath() : null;
            if (path.equals(oldPath)) throw new ResourceMigrationException("New and old path cannot match");

            final var migrate = (oldPath != null || !baseName.equals(oldResource))
                    && (oldResource != null || oldPath != null);

            if (migrate) migrate(baseName, file, oldPath, oldResource);

            if (migrate || Files.isRegularFile(file.getFile())) try {
                migrateResource(baseName, locale, file);
            } catch (final Exception e) {
                throw new ResourceMigrationException("An error occurred while migrating resource '" + file.getFile() + "'", e);
            }
        }

        private void migrate(final String baseName, final PropertiesFile file, @Nullable final Path oldPath, @Nullable final String oldResource) throws IOException {
            final var actualPath = oldPath != null ? oldPath : path;
            final var actualResource = oldResource != null ? oldResource : baseName;
            final var oldFile = new PropertiesFile(actualPath.resolve(actualResource), charset, new Properties());
            file.merge(oldFile.getRoot());
            if (Files.deleteIfExists(oldFile.getFile()))
                LOGGER.debug("Migrated resource '{}' to '{}'", oldFile.getFile(), file.getFile());
            else LOGGER.warn("Failed to delete old resource '{}'", oldFile.getFile());
        }

        private void migrateResource(final String resource, final Locale locale, final PropertiesFile file) {
            if (migrator == null || !migrator.shouldMigrate(resource, file.getRoot())) return;

            final var migrated = new Properties(file.getRoot().size());

            file.getRoot().forEach((key, message) -> {
                final var migration = migrator.migrate(locale, key.toString(), message.toString());
                if (migration == null) {
                    migrated.put(key, message);
                    return;
                }

                if (migration.drop()) return;

                final var migratedKey = migration.key() != null ? migration.key() : key;
                final var migratedMessage = migration.message() != null ? migration.message() : message;

                migrated.put(migratedKey, migratedMessage);
            });

            file.setRoot(migrated);
        }
    }
}

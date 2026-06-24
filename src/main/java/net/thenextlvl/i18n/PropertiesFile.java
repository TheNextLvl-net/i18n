package net.thenextlvl.i18n;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Properties;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

class PropertiesFile {
    private final Charset charset;
    private final Path path;

    private final Properties defaultRoot;
    private Properties root;

    private boolean loaded;

    public PropertiesFile(final Path path, final Charset charset, final Properties root) {
        this.charset = charset;
        this.defaultRoot = root;
        this.path = path;
        this.root = root;
    }

    protected Properties load() {
        if (!Files.isRegularFile(getFile())) return (Properties) getRoot().clone();
        try (final var reader = new InputStreamReader(Files.newInputStream(getFile(), READ), charset);
             final var buffer = new BufferedReader(reader)) {
            final var properties = new Properties();
            properties.load(buffer);
            return properties;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public PropertiesFile save(final FileAttribute<?>... attributes) {
        try {
            final var root = getRoot();
            Files.createDirectories(getFile().toAbsolutePath().getParent(), attributes);
            try (final var writer = new BufferedWriter(new OutputStreamWriter(
                    Files.newOutputStream(getFile(), WRITE, CREATE, TRUNCATE_EXISTING),
                    charset
            ))) {
                root.store(writer, null);
                return this;
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public PropertiesFile validate(final ComponentBundle.Scope scope) {
        final var root = getRoot();
        if (root == defaultRoot) return this;
        if (scope.isFiltering()) root.entrySet().removeIf(entry ->
                !defaultRoot.containsKey(entry.getKey()));
        if (scope.isFilling()) merge(defaultRoot);
        return this;
    }

    public PropertiesFile setRoot(final Properties root) {
        this.loaded = true;
        this.root = root;
        return this;
    }

    public Properties getRoot() {
        if (loaded) return root;
        loaded = true;
        return root = load();
    }

    public Path getFile() {
        return path;
    }

    public PropertiesFile merge(final Properties properties) {
        final var root = getRoot();
        properties.forEach((key, value) -> {
            if (root.containsKey(key)) return;
            root.put(key, value);
        });
        return this;
    }
}

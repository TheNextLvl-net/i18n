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

    public PropertiesFile(Path path, Charset charset, Properties root) {
        this.charset = charset;
        this.defaultRoot = root;
        this.path = path;
        this.root = root;
    }

    protected Properties load() {
        if (!Files.isRegularFile(getFile())) return (Properties) getRoot().clone();
        try (var reader = new InputStreamReader(Files.newInputStream(getFile(), READ), charset);
             var buffer = new BufferedReader(reader)) {
            var properties = new Properties();
            properties.load(buffer);
            return properties;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public PropertiesFile save(FileAttribute<?>... attributes) {
        try {
            var root = getRoot();
            Files.createDirectories(getFile().toAbsolutePath().getParent(), attributes);
            try (var writer = new BufferedWriter(new OutputStreamWriter(
                    Files.newOutputStream(getFile(), WRITE, CREATE, TRUNCATE_EXISTING),
                    charset
            ))) {
                root.store(writer, null);
                return this;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public PropertiesFile validate(ComponentBundle.Scope scope) {
        var root = getRoot();
        if (root == defaultRoot) return this;
        if (scope.isFiltering()) root.entrySet().removeIf(entry ->
                !defaultRoot.containsKey(entry.getKey()));
        if (scope.isFilling()) merge(defaultRoot);
        return this;
    }

    public PropertiesFile setRoot(Properties root) {
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

    public PropertiesFile merge(Properties properties) {
        var root = getRoot();
        properties.forEach((key, value) -> {
            if (root.containsKey(key)) return;
            root.put(key, value);
        });
        return this;
    }
}

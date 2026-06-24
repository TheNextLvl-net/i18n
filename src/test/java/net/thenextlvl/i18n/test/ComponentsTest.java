package net.thenextlvl.i18n.test;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;

class ComponentsTest extends BaseTest {
    @Override
    public Key key() {
        return Key.key("test", "components");
    }

    @Test
    void lineBreaksProduceSeparateComponents() {
        final var lines = bundle.components("lines", Locale.US);

        Assertions.assertArrayEquals(new Component[]{
                Component.text("first").decorate(TextDecoration.ITALIC),
                Component.text("second").decoration(TextDecoration.ITALIC, false),
                Component.text("third").decoration(TextDecoration.ITALIC, false),
                Component.text("fourth"),
                Component.empty(),
                Component.text("hello", NamedTextColor.RED)
        }, lines);
    }
}

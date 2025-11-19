import org.jspecify.annotations.NullMarked;

@NullMarked
module i18n.test {
    requires net.kyori.adventure.key;
    requires net.kyori.adventure.text.logger.slf4j;
    requires net.kyori.adventure.text.minimessage;
    requires net.kyori.adventure;
    requires net.thenextlvl.i18n;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;
    
    exports net.thenextlvl.i18n.test;
}
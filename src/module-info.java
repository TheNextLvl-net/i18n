import org.jspecify.annotations.NullMarked;

@NullMarked
module net.thenextlvl.i18n {
    requires net.kyori.adventure.key;
    requires net.kyori.adventure.text.minimessage;
    requires net.kyori.adventure;
    requires net.kyori.examination.api;
    requires org.slf4j;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;

    exports net.thenextlvl.i18n;
}
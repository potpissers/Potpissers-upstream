package com.memeasaur.potpissersdefault.Util.Potpissers.Methods;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class Component {
    public static net.kyori.adventure.text.Component getConsoleComponent(String string) {
        return net.kyori.adventure.text.Component.text(string, NamedTextColor.GRAY, TextDecoration.ITALIC);
    }
    public static net.kyori.adventure.text.Component getFocusComponent(String string) {
        return net.kyori.adventure.text.Component.text(string, NamedTextColor.LIGHT_PURPLE);
    }
    public static net.kyori.adventure.text.Component getNormalComponent(String string) {
        return net.kyori.adventure.text.Component.text(string, NamedTextColor.YELLOW);
    }
    public static net.kyori.adventure.text.Component getDangerComponent(String string) {
        return net.kyori.adventure.text.Component.text(string, NamedTextColor.RED);
    }
    public static net.kyori.adventure.text.Component getPartyConsoleComponent(String string) {
        return net.kyori.adventure.text.Component.text(string, NamedTextColor.GREEN, TextDecoration.ITALIC);
    }
    public static net.kyori.adventure.text.Component getAllyConsoleComponent(String string) {
        return net.kyori.adventure.text.Component.text(string, NamedTextColor.AQUA, TextDecoration.ITALIC);
    }
    public static net.kyori.adventure.text.Component getDangerConsoleComponent(String string) {
        return net.kyori.adventure.text.Component.text(string, NamedTextColor.RED, TextDecoration.ITALIC);
    }
}

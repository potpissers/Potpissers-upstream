package com.memeasaur.potpissersdefault.Classes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum ChatPrefix {
    OWNER(0, Component.text("Owner", NamedTextColor.DARK_RED)),
    DEV(1, Component.text("Dev", NamedTextColor.DARK_PURPLE)),

    STAFF_BASIC(2, Component.text("Staff", NamedTextColor.GREEN)),
    STAFF_GOLD(3, Component.text("Staff", NamedTextColor.YELLOW)),
    STAFF_DIAMOND(4, Component.text("Staff", NamedTextColor.AQUA)),
    STAFF_RUBY(5, Component.text("Staff", NamedTextColor.RED)),
    STAFF_BIG_DOG(6, Component.text("Staff", NamedTextColor.LIGHT_PURPLE)),

    HELPER_BASIC(7, Component.text("Helper", NamedTextColor.GREEN)),
    HELPER_GOLD(8, Component.text("Helper", NamedTextColor.YELLOW)),
    HELPER_DIAMOND(9, Component.text("Helper", NamedTextColor.AQUA)),
    HELPER_RUBY(10, Component.text("Helper", NamedTextColor.RED)),
    HELPER_BIG_DOG(11, Component.text("Helper", NamedTextColor.LIGHT_PURPLE)),

    MEDIA_BASIC(12, Component.text("Media", NamedTextColor.GREEN)),
    MEDIA_GOLD(13, Component.text("Media", NamedTextColor.YELLOW)),
    MEDIA_DIAMOND(14, Component.text("Media", NamedTextColor.AQUA)),
    MEDIA_RUBY(15, Component.text("Media", NamedTextColor.RED)),

    FAMOUS_GOLD(16, Component.text("Famous", NamedTextColor.YELLOW)),
    FAMOUS_DIAMOND(17, Component.text("Famous", NamedTextColor.AQUA)),
    FAMOUS_RUBY(18, Component.text("Famous", NamedTextColor.RED)),
    FAMOUS_BIG_DOG(19, Component.text("Famous", NamedTextColor.LIGHT_PURPLE)),

    BUILDER_BASIC(20, Component.text("Builder", NamedTextColor.GREEN)),
    BUILDER_GOLD(21, Component.text("Builder", NamedTextColor.YELLOW)),
    BUILDER_DIAMOND(22, Component.text("Builder", NamedTextColor.AQUA)),
    BUILDER_RUBY(23, Component.text("Builder", NamedTextColor.RED)),
    BUILDER_BIG_DOG(24, Component.text("Builder", NamedTextColor.LIGHT_PURPLE)),

    LEADER_BASIC(25, Component.text("Leader", NamedTextColor.GREEN)),
    LEADER_GOLD(26, Component.text("Leader", NamedTextColor.YELLOW)),
    LEADER_DIAMOND(27, Component.text("Leader", NamedTextColor.AQUA)),
    LEADER_RUBY(28, Component.text("Leader", NamedTextColor.RED)),
    LEADER_BIG_DOG(29, Component.text("Leader", NamedTextColor.LIGHT_PURPLE)),

    GITHUB_BASIC(30, Component.text("Github", NamedTextColor.GREEN)),
    GITHUB_GOLD(31, Component.text("Github", NamedTextColor.YELLOW)),
    GITHUB_DIAMOND(32, Component.text("Github", NamedTextColor.AQUA)),
    GITHUB_RUBY(33, Component.text("Github", NamedTextColor.RED)),
    GITHUB_BIG_DOG(34, Component.text("Github", NamedTextColor.LIGHT_PURPLE)),

    BASIC(35, Component.text("Basic", NamedTextColor.GREEN)),
    BASIC_BADGE(36, Component.text("", NamedTextColor.GREEN)),
    GOLD(37, Component.text("Gold", NamedTextColor.YELLOW)),
    GOLD_BADGE(38, Component.text("", NamedTextColor.YELLOW)),
    DIAMOND(39, Component.text("Diamond", NamedTextColor.AQUA)),
    DIAMOND_BADGE(40, Component.text("", NamedTextColor.AQUA)),
    RUBY(41, Component.text("Ruby", NamedTextColor.RED)),
    RUBY_BADGE(42, Component.text("", NamedTextColor.RED)),
    BIG_DOG(43, Component.text("Big dog", NamedTextColor.LIGHT_PURPLE)),
    BIG_DOG_BADGE(44, Component.text("", NamedTextColor.LIGHT_PURPLE));

    public final int postgresId;
    public final Component component;
    ChatPrefix(int id, Component component) {
        this.postgresId = id;
        this.component = component;
    }

    public static final Map<Integer, ChatPrefix> CHAT_PREFIX_ID_MAP = Arrays.stream(ChatPrefix.values()).collect(Collectors.toMap(chatPrefix -> chatPrefix.postgresId, chatPrefix -> chatPrefix));
}

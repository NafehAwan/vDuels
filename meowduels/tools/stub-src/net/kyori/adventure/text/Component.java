package net.kyori.adventure.text;

public interface Component {
    net.kyori.adventure.text.Component append(net.kyori.adventure.text.Component a0);
    net.kyori.adventure.text.Component colorIfAbsent(net.kyori.adventure.text.format.TextColor a0);
    net.kyori.adventure.text.Component decoration(net.kyori.adventure.text.format.TextDecoration a0, boolean a1);
}

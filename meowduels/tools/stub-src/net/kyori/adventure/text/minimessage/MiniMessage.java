package net.kyori.adventure.text.minimessage;

// Both of these come from ComponentSerializer<I extends Component, O extends I, R>,
// and are declared here in their ERASED form because that is the only form that
// exists at runtime:
//
//   I  deserialize(R) -> Component deserialize(Object)   R is unbounded
//   R  serialize(O)   -> Object    serialize(Component)  O erases to Component
//
// Writing serialize as returning String compiles fine against this stub and
// then throws NoSuchMethodError on the first chat message, because no method
// with that descriptor exists. deserialize was already correct, and it is the
// proof of the rule: if the erasure were anything else it would have been
// failing the same way for as long as this plugin has run.
public interface MiniMessage {
    net.kyori.adventure.text.Component deserialize(java.lang.Object a0);
    java.lang.Object serialize(net.kyori.adventure.text.Component a0);
    public static net.kyori.adventure.text.minimessage.MiniMessage miniMessage() { return null; }
}

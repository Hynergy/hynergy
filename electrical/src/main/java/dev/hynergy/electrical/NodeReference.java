package dev.hynergy.electrical;

/** A node in one electrical declaration. */
public sealed interface NodeReference permits DeviceTerminal, DeviceDeclaration.InternalNode { }

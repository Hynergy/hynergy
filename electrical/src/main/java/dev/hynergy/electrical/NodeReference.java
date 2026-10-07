package dev.hynergy.electrical;

/**
 * Identifies a node in one electrical declaration.
 */
public sealed interface NodeReference permits DeviceTerminal, DeviceDeclaration.InternalNode { }

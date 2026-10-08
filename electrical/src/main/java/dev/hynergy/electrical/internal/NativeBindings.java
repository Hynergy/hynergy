package dev.hynergy.electrical.internal;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public final class NativeBindings {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup SYMBOLS = NativeLibrary.load();


    private static final MethodHandle ABI_VERSION =
        downcall("hynergy_abi_version", FunctionDescriptor.of(ValueLayout.JAVA_INT));

    static int abiVersion() {
        try {
            return (int) ABI_VERSION.invokeExact();
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to call hynergy_abi_version", e);
        }
    }



    private static final MethodHandle ABI_REVISION =
        downcall("hynergy_abi_revision", FunctionDescriptor.of(ValueLayout.JAVA_INT));

    static int abiRevision() {
        try {
            return (int) ABI_REVISION.invokeExact();
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to call hynergy_abi_revision", e);
        }
    }



    /**
     * Creates an engine through the native ABI.
     *
     * <p>Native signature:</p>
     * <pre>{@code
     * EngineHandle* (u32 max_worker_threads)
     * }</pre>
     */
    private static final MethodHandle ENGINE_CREATE = downcall(
        "hynergy_engine_create", FunctionDescriptor.of(
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT
        )
    );

    public static MemorySegment createEngine(int maxWorkerThreads) {
        try {
            return (MemorySegment) ENGINE_CREATE.invokeExact(maxWorkerThreads);
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to create native electrical engine", throwable);
        }
    }



    private static final MethodHandle ENGINE_DESTROY =
        downcall("hynergy_engine_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    public static void destroyEngine(MemorySegment engine) {
        try {
            ENGINE_DESTROY.invokeExact(engine);
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to destroy native electrical engine", throwable);
        }
    }



    /**
     * Registers an encoded definition through the native ABI.
     *
     * <p>Native signature:</p>
     * <pre>{@code
     * u32 (EngineHandle* engine, const u8* input, u32 input_len,
     * DefinitionRegistrationResult* result)
     * }</pre>
     */
    private static final MethodHandle ENGINE_REGISTER_DEFINITION = downcall(
        "hynergy_engine_register_definition", FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS
        )
    );

    public static int registerDefinition(
        MemorySegment engine,
        MemorySegment input,
        int inputLength,
        MemorySegment result
    ) {
        try {
            return (int) ENGINE_REGISTER_DEFINITION.invokeExact(engine, input, inputLength, result);
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to register native electrical device definition", throwable);
        }
    }



    private static final MethodHandle ENGINE_VALIDATE_PARAMETER = downcall(
            "hynergy_engine_validate_parameter",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS,
                    ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_DOUBLE)
    );

    public static int validateParameter(MemorySegment engine, int definition, int parameter, double value) {
        try {
            return (int) ENGINE_VALIDATE_PARAMETER.invokeExact(engine, definition, parameter, value);
        } catch (Throwable failure) {
            throw new IllegalStateException("Failed to call hynergy_engine_validate_parameter", failure);
        }
    }

    private static final MethodHandle ENGINE_VALIDATE_PARAMETERS = downcall(
            "hynergy_engine_validate_parameters",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
    );

    public static int validateParameters(MemorySegment engine, int definition, MemorySegment values, int count) {
        try {
            return (int) ENGINE_VALIDATE_PARAMETERS.invokeExact(engine, definition, values, count);
        } catch (Throwable failure) {
            throw new IllegalStateException("Failed to call hynergy_engine_validate_parameters", failure);
        }
    }

    /**
     * Creates a world through the native ABI.
     *
     * <p>Native signature:</p>
     * <pre>{@code
     * u32 (EngineHandle* engine, u32 tick_frequency_hz, WorldHandle** world)
     * }</pre>
     */
    private static final MethodHandle ENGINE_CREATE_WORLD = downcall(
        "hynergy_engine_create_world", FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS
        )
    );

    public static int createWorld(MemorySegment engine, int tickFrequencyHz, MemorySegment result) {
        try {
            return (int) ENGINE_CREATE_WORLD.invokeExact(engine, tickFrequencyHz, result);
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to create native electrical world", throwable);
        }
    }



    private static final MethodHandle WORLD_DESTROY =
        downcall("hynergy_world_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    public static void destroyWorld(MemorySegment world) {
        try {
            WORLD_DESTROY.invokeExact(world);
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to destroy native electrical world", throwable);
        }
    }



    /**
     * Advances a world through the native ABI.
     *
     * <p>Native signature:</p>
     * <pre>{@code
     * u32 (WorldHandle* world, SubscriptionRecord* records, u32 record_capacity,
     * TickResult* result)
     * }</pre>
     */
    private static final MethodHandle WORLD_TICK = downcall(
        "hynergy_world_tick", FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS
        )
    );

    public static int tickWorld(MemorySegment world, MemorySegment records, int record_capacity, MemorySegment result) {
        try {
            return (int) WORLD_TICK.invokeExact(world, records, record_capacity, result);
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to tick native electrical world", throwable);
        }
    }



    /**
     * Applies encoded world commands through the native ABI.
     *
     * <p>Native signature:</p>
     * <pre>{@code
     * u32 (WorldHandle* world, const u8* input, u32 input_len, CommandResult* result)
     * }</pre>
     */
    private static final MethodHandle WORLD_APPLY_COMMANDS = downcall(
        "hynergy_world_apply_commands", FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS
        )
    );

    public static int applyCommands(MemorySegment world, MemorySegment input, int inputLength, MemorySegment result) {
        try {
            return (int) WORLD_APPLY_COMMANDS.invokeExact(world, input, inputLength, result);
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to apply native electrical world commands", throwable);
        }
    }



    /**
     * Creates an observation subscription through the native ABI.
     *
     * <p>Native signature:</p>
     * <pre>{@code
     * u32 (WorldHandle* world, u32 device_id, u32 observer_id, u32* subscription_id)
     * }</pre>
     */
    private static final MethodHandle WORLD_SUBSCRIBE_OBSERVER = downcall(
        "hynergy_world_subscribe_observer", FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS
        )
    );

    public static int subscribeObserver(
        MemorySegment world,
        int deviceId,
        int observerId,
        MemorySegment subscriptionId
    ) {
        try {
            return (int) WORLD_SUBSCRIBE_OBSERVER.invokeExact(world, deviceId, observerId, subscriptionId);
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to create native electrical subscription", throwable);
        }
    }



    /**
     * Stops an observation subscription through the native ABI.
     *
     * <p>Native signature:</p>
     * <pre>{@code
     * u32 (WorldHandle* world, u32 subscription_id)
     * }</pre>
     */
    private static final MethodHandle WORLD_UNSUBSCRIBE = downcall(
        "hynergy_world_unsubscribe", FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT
        )
    );

    public static int unsubscribe(MemorySegment world, int subscriptionId) {
        try {
            return (int) WORLD_UNSUBSCRIBE.invokeExact(world, subscriptionId);
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to destroy native electrical subscription", throwable);
        }
    }



    private static MethodHandle downcall(String name, FunctionDescriptor descriptor) {
        MemorySegment symbol =
            SYMBOLS.find(name).orElseThrow(() -> new IllegalStateException("Missing native symbol: " + name));

        return LINKER.downcallHandle(symbol, descriptor);
    }
}

package dev.hynergy.core.electricity.device;

import dev.hynergy.electrical.DeviceType;
import dev.hynergy.electrical.ParameterConstraints;
import dev.hynergy.electrical.PrimitiveDeviceTypes;

final class DeviceTestTypes {
    private DeviceTestTypes() {
    }

    static DeviceType resistor(int parameterId, int positiveId, int negativeId, int currentId) {
        return DeviceType.define(b -> {
            var p = b.terminal(positiveId, "positive");
            var n = b.terminal(negativeId, "negative");
            var r = b.parameter(parameterId, "resistance", ParameterConstraints.positiveFinite());
            var child = b.element(PrimitiveDeviceTypes.RESISTANCE, e -> {
                e.connect(PrimitiveDeviceTypes.RESISTANCE.terminal(0), p);
                e.connect(PrimitiveDeviceTypes.RESISTANCE.terminal(1), n);
                e.bind(PrimitiveDeviceTypes.RESISTANCE.parameter(0), r);
            });
            b.childObserver(currentId, "current", child, PrimitiveDeviceTypes.RESISTANCE.observer(1));
        });
    }
}

use super::IdAllocator;
use hynergy_engine::{
    Engine, EngineConfig, EngineTickError, SubscriptionId, SubscriptionValueStatus, WorldCommand,
    WorldConfig,
};
use hynergy_model::device::definition::{
    DefinitionObserverId, DeviceId, PrimitiveElementKind, TerminalId,
};
use hynergy_model::network::WireId;
use hynergy_model::parameter::ParameterId;
use std::num::NonZeroU32;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SwitchedLogicTopology {
    Chain,
    FanOut,
    SharedOutput,
}

impl SwitchedLogicTopology {
    pub const ALL: [Self; 3] = [Self::Chain, Self::FanOut, Self::SharedOutput];
    pub const fn name(self) -> &'static str {
        match self {
            Self::Chain => "chain",
            Self::FanOut => "fan_out",
            Self::SharedOutput => "shared_output",
        }
    }
}

pub struct SwitchedLogicScenario {
    engine: Engine,
    world: u32,
    ids: IdAllocator,
    input: DeviceId,
    probes: Vec<SubscriptionId>,
    voltages: Vec<f64>,
}

impl SwitchedLogicScenario {
    pub fn new(topology: SwitchedLogicTopology, size: usize) -> Self {
        assert!(size > 0);
        let mut engine = Engine::new(EngineConfig::new(1));
        let world = engine
            .new_world(WorldConfig::new(NonZeroU32::new(20).unwrap()))
            .unwrap();
        let mut scenario = Self {
            engine,
            world,
            ids: IdAllocator::new(),
            input: DeviceId::try_from(1).unwrap(),
            probes: Vec::new(),
            voltages: Vec::new(),
        };
        let ground = scenario.wire();
        let supply = scenario.wire();
        let input = scenario.wire();
        scenario.device(
            PrimitiveElementKind::VoltageSource,
            &[5.0],
            &[supply, ground],
        );
        scenario.input = scenario.device(
            PrimitiveElementKind::VoltageSource,
            &[0.0],
            &[input, ground],
        );
        let mut previous = input;
        let shared = scenario.wire();
        if topology == SwitchedLogicTopology::FanOut {
            previous = scenario.wire();
            scenario.gate(previous, supply, ground, input);
        }
        for index in 0..size {
            let output = scenario.wire();
            let gate_input = match topology {
                SwitchedLogicTopology::Chain | SwitchedLogicTopology::FanOut => previous,
                SwitchedLogicTopology::SharedOutput => {
                    if index == 0 {
                        input
                    } else {
                        supply
                    }
                }
            };
            let gate = scenario.gate(output, supply, ground, gate_input);
            if topology == SwitchedLogicTopology::SharedOutput {
                scenario.command(WorldCommand::ConnectWires {
                    wire_a: output,
                    wire_b: shared,
                });
            }
            if topology != SwitchedLogicTopology::Chain || index + 1 == size {
                scenario.probes.push(
                    scenario
                        .engine
                        .subscribe_observer(world, gate, DefinitionObserverId::new(0))
                        .unwrap(),
                );
                scenario.voltages.push(f64::NAN);
            }
            if topology == SwitchedLogicTopology::Chain {
                previous = output;
            }
        }
        scenario
    }

    pub fn drive(&mut self, high: bool) {
        self.command(WorldCommand::SetDeviceParameter {
            device: self.input,
            parameter: ParameterId::new(0),
            value: if high { 5.0 } else { 0.0 },
        });
    }

    pub fn tick(&mut self) -> Result<(), EngineTickError> {
        self.engine.tick_world(self.world)?;
        for update in self.engine.subscription_updates(self.world).unwrap() {
            assert_eq!(update.status(), SubscriptionValueStatus::Available);
            if let Some(index) = self
                .probes
                .iter()
                .position(|probe| *probe == update.subscription())
            {
                self.voltages[index] = update.value();
            }
        }
        Ok(())
    }

    pub fn output_voltages(&self) -> &[f64] {
        &self.voltages
    }

    #[cfg(feature = "solver-profiling")]
    pub fn solver_tick_profile(&self) -> hynergy_engine::SolverTickProfile {
        self.engine.solver_tick_profile(self.world).unwrap()
    }

    fn command(&mut self, command: WorldCommand) {
        self.engine
            .apply_world_command(self.world, command)
            .unwrap();
    }
    fn wire(&mut self) -> WireId {
        let wire = self.ids.wire();
        self.command(WorldCommand::AddWire { wire });
        wire
    }
    fn device(
        &mut self,
        kind: PrimitiveElementKind,
        parameters: &[f64],
        wires: &[WireId],
    ) -> DeviceId {
        let device = self.ids.device();
        self.command(WorldCommand::AddDevice {
            device,
            definition: kind.into(),
        });
        for (index, &value) in parameters.iter().enumerate() {
            self.command(WorldCommand::SetDeviceParameter {
                device,
                parameter: ParameterId::new(index as u32),
                value,
            });
        }
        for (index, &wire) in wires.iter().enumerate() {
            self.command(WorldCommand::AttachTerminal {
                device,
                terminal: TerminalId::new(index as u32),
                wire,
            });
        }
        device
    }
    fn gate(&mut self, output: WireId, supply: WireId, ground: WireId, input: WireId) -> DeviceId {
        self.device(
            PrimitiveElementKind::SwitchedNot,
            &[2.5, 1.0, 0.0, 1e-6, 1e-6],
            &[output, supply, ground, input],
        )
    }
}

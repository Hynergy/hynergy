use hynergy_benchmarks::fixtures::{SwitchedLogicScenario, SwitchedLogicTopology};

fn main() {
    for topology in SwitchedLogicTopology::ALL {
        for size in [8, 32, 128] {
            let mut scenario = SwitchedLogicScenario::new(topology, size);
            for (tick, high) in [false, true, false, true].into_iter().enumerate() {
                scenario.drive(high);
                scenario.tick().unwrap();
                let expected_high = match topology {
                    SwitchedLogicTopology::Chain => high != (size % 2 == 1),
                    SwitchedLogicTopology::FanOut => high,
                    SwitchedLogicTopology::SharedOutput => !high,
                };
                assert!(
                    scenario
                        .output_voltages()
                        .iter()
                        .all(|voltage| voltage.is_finite() && (*voltage >= 2.5) == expected_high)
                );
                let profile = scenario.solver_tick_profile();
                println!(
                    "{} size={} tick={} plans={} groups={} solves={} factorizations={} verification={} closure_ops={} fallbacks={}",
                    topology.name(),
                    size,
                    tick,
                    profile.planned_discrete_islands(),
                    profile.total_qualified_discrete_drivers(),
                    profile.total_mna_solves(),
                    profile.total_matrix_factorizations(),
                    profile.total_discrete_verification_solves(),
                    profile.total_discrete_closure_actual_iteration_ops(),
                    profile.total_discrete_fallbacks()
                );
            }
        }
    }
}

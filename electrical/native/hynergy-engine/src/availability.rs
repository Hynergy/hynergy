use hynergy_model::device::definition::DeviceId;

#[derive(Debug, Default)]
pub(crate) struct DeviceAvailability {
    unloaded: Vec<u64>,
}

impl DeviceAvailability {
    #[inline]
    pub(crate) fn is_unloaded(&self, device: DeviceId) -> bool {
        let index = device.index();

        self.unloaded
            .get(index >> 6)
            .is_some_and(|word| word & (1_u64 << (index & 63)) != 0)
    }

    pub(crate) fn set_loaded(&mut self, device: DeviceId, loaded: bool) -> bool {
        let index = device.index();
        let word_index = index >> 6;
        let mask = 1_u64 << (index & 63);

        if !loaded && word_index >= self.unloaded.len() {
            self.unloaded.resize(word_index + 1, 0);
        }

        let Some(word) = self.unloaded.get_mut(word_index) else {
            return false;
        };

        let was_unloaded = *word & mask != 0;

        if loaded {
            *word &= !mask;
        } else {
            *word |= mask;
        }

        was_unloaded == loaded
    }
}

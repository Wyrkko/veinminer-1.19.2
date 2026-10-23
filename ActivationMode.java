package com.jupkke.veinminer;

public enum ActivationMode {
    SNEAK("Sneak"),
    ALWAYS("Always On"),
    KEYBIND("Keybind");

    private final String label;

    ActivationMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public ActivationMode next() {
        ActivationMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}

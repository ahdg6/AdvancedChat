package io.github.darkkronicle.advancedchat.chat.registry;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.darkkronicle.advancedchat.config.ConfigStorage;
import io.github.darkkronicle.advancedchat.interfaces.ConfigRegistryOption;
import io.github.darkkronicle.advancedchat.interfaces.IMatchProcessor;
import io.github.darkkronicle.advancedchat.interfaces.RegistryOption;
import io.github.darkkronicle.advancedchat.interfaces.Translatable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class MatchProcessorRegistry extends AbstractRegistry<IMatchProcessor, MatchProcessorRegistry.MatchProcessorOption> {

    private MatchProcessorRegistry() {

    }

    private static final MatchProcessorRegistry INSTANCE = new MatchProcessorRegistry();


    public static MatchProcessorRegistry getInstance() {
        return INSTANCE;
    }


    public MatchProcessorOption get(String string) {
        for (MatchProcessorOption option : getAll()) {
            if (option.getStringValue().equals(string)) {
                return option;
            }
        }
        return null;
    }

    @Override
    public MatchProcessorRegistry clone() {
        MatchProcessorRegistry registry = new MatchProcessorRegistry();
        for (MatchProcessorOption o : getAll()) {
            registry.add(o.copy(registry));
        }
        return registry;
    }

    @Override
    public MatchProcessorOption constructOption(IMatchProcessor iMatchProcessor, String saveString, String translation, String infoTranslation, boolean active, boolean setDefault) {
        return new MatchProcessorOption(iMatchProcessor, saveString, translation, infoTranslation, active, this);
    }

    public static class MatchProcessorOption implements Translatable, ConfigRegistryOption<IMatchProcessor> {

        private final IMatchProcessor processor;
        private final String saveString;
        private final String translation;
        private final MatchProcessorRegistry registry;
        private final String infoTranslation;
        private final ConfigStorage.SaveableConfig<ConfigBoolean> active;

        // Only register
        private MatchProcessorOption(IMatchProcessor processor, String saveString, String translation, String infoTranslation, boolean active, MatchProcessorRegistry registry) {
            this.processor = processor;
            this.saveString = saveString;
            this.translation = translation;
            this.registry = registry;
            this.infoTranslation = infoTranslation;
            this.active = ConfigStorage.SaveableConfig.fromConfig(saveString, new ConfigBoolean(translation, active, infoTranslation));
        }

        @Override
        public ConfigStorage.SaveableConfig<ConfigBoolean> getActive() {
            return active;
        }

        @Override
        public String getStringValue() {
            return saveString;
        }

        @Override
        public String getDisplayName() {
            return StringUtils.translate(translation);
        }

        @Override
        public IConfigOptionListEntry cycle(boolean forward) {
            return registry.getNext(this, forward);
        }

        @Override
        public IConfigOptionListEntry fromString(String value) {
            return registry.fromString(value);
        }


        @Override
        public String getTranslationKey() {
            return translation;
        }

        @Override
        public IMatchProcessor getOption() {
            return processor;
        }

        @Override
        public String getSaveString() {
            return null;
        }

        @Override
        public MatchProcessorOption copy(AbstractRegistry<IMatchProcessor, ?> registry) {
            return new MatchProcessorOption(processor, saveString, translation, infoTranslation, isActive(), registry == null ? this.registry : (MatchProcessorRegistry) registry);
        }
    }
}

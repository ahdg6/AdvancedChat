package net.darkkronicle.advancedchat.filters;

import net.darkkronicle.advancedchat.config.Filter;
import net.darkkronicle.advancedchat.util.SearchUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.text.Text;

import java.util.Optional;

@Environment(EnvType.CLIENT)
public class NotifyFilter extends AbstractFilter {

    private final Filter.NotifySound notifySound;
    private final float volume;
    private final float pitch;

    public NotifyFilter(String toFind, Filter.FindType findType, Filter.NotifySound notifySound, float volume, float pitch) {
        this.notifySound = notifySound;
        super.filterString = toFind;
        super.findType = findType;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public Optional<Text> filter(Text text) {
        if (notifySound != Filter.NotifySound.NONE) {
            if (SearchUtils.isMatch(text.getString(), filterString, findType)) {
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(notifySound.event, pitch, volume));
            }
        }
        return Optional.empty();
    }
}

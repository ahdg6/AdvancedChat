package net.darkkronicle.advancedchat.chat;

import lombok.Getter;
import net.darkkronicle.advancedchat.AdvancedChat;
import net.darkkronicle.advancedchat.config.ConfigStorage;
import net.darkkronicle.advancedchat.config.Filter;
import net.darkkronicle.advancedchat.filters.AbstractFilter;
import net.darkkronicle.advancedchat.filters.ColorFilter;
import net.darkkronicle.advancedchat.filters.ForwardFilter;
import net.darkkronicle.advancedchat.filters.NotifyFilter;
import net.darkkronicle.advancedchat.filters.ReplaceFilter;
import net.darkkronicle.advancedchat.filters.processors.ChatTabProcessor;
import net.darkkronicle.advancedchat.interfaces.IMessageProcessor;
import net.darkkronicle.advancedchat.mixin.MixinChatHudInvoker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A hook into {@link MessageDispatcher} for forwarding chat events. This handles the filters
 */
@Environment(EnvType.CLIENT)
public class ChatDispatcher implements IMessageProcessor {

    @Getter
    private ArrayList<ColorFilter> colorFilters = new ArrayList<>();

    private ArrayList<AbstractFilter> filters = new ArrayList<>();

    private ArrayList<ForwardFilter> forwardFilters = new ArrayList<>();

    private final static ChatDispatcher INSTANCE = new ChatDispatcher();

    /**
     * The "Terminate" text. It has a length of zero and is non-null so it will stop the process if
     * returned by the main filter.
     */
    public final static Text TERMINATE = new LiteralText("");

    /**
     * The final processor to go to if the process hasn't been terminated.
     *
     * This is typically used to send the filtered contents back into chat.
     */
    private IMessageProcessor finalProcessor;

    public static ChatDispatcher getInstance() {
        return INSTANCE;
    }

    /**
     * Sets the processor for when the process hasn't been terminated. Is typically used for sending the message
     * back to chat if it hasn't been terminated.
     *
     * @param processor Processor to accept the text data.
     */
    public void setFinalProcessor(IMessageProcessor processor) {
        this.finalProcessor = processor;
    }

    private ChatDispatcher() {
        setFinalProcessor((text, original) -> {
            ((MixinChatHudInvoker) MinecraftClient.getInstance().inGameHud.getChatHud()).invokeAddMessage(text, 0, MinecraftClient.getInstance().inGameHud.getTicks(), false);
            return true;
        });
    }

    @Override
    public boolean process(Text text, Text original) {
        Text unfiltered = text;

        // Filter text
        for (AbstractFilter filter : filters) {
            Optional<Text> newtext = filter.filter(text);
            if (newtext.isPresent()) {
                text = newtext.get();
            }
        }

        ArrayList<IMessageProcessor> processed = new ArrayList<>();
        boolean forward = true;
        for (ForwardFilter f : forwardFilters) {
            if (f.filter(text, processed).isPresent()) {
                // Send the signal to stop
                forward = false;
            }
        }
        if (!forward) {
            return true;
        }

        if (text.getString().length() != 0) {
            finalProcessor.process(text, unfiltered);
        }
        return true;
    }

    /**
     * Loads filters that are stored in ConfigStorage.
     * Converts {@link Filter} into an {@link AbstractFilter}
     */
    public void loadFilters() {
        filters = new ArrayList<>();
        colorFilters = new ArrayList<>();
        for (Filter filter : ConfigStorage.FILTERS) {
            // If it replaces anything.
            List<AbstractFilter> afilter = createFilter(filter);
            if (afilter != null) {
                for (AbstractFilter f : afilter) {
                    if (f instanceof ColorFilter) {
                        colorFilters.add((ColorFilter) f);
                    } else {
                        filters.add(f);
                    }
                }
            }

        }
    }

    public static List<AbstractFilter> createFilter(Filter filter) {
        if (!filter.getActive().config.getBooleanValue()) {
            return null;
        }
        ArrayList<AbstractFilter> filters = new ArrayList<>();
        if (filter.getReplace() != null) {
            if (filter.getReplace().useChildren()) {
                ReplaceFilter f = new ReplaceFilter(filter.getFindString().config.getStringValue(), filter.getReplaceTo().config.getStringValue().replaceAll("&", "§"), filter.getFind(), filter.getReplace(), null);
                if (filter.getChildren() != null) {
                    for (Filter child : filter.getChildren()) {
                        List<AbstractFilter> childf = createFilter(child);
                        if (childf != null) {
                            for (AbstractFilter childfilter : childf) {
                                f.addChild(childfilter);
                            }
                        }
                    }
                }
                filters.add(f);
            } else if (filter.getReplaceTextColor().config.getBooleanValue()) {
                filters.add(new ReplaceFilter(filter.getFindString().config.getStringValue(), filter.getReplaceTo().config.getStringValue().replaceAll("&", "§"), filter.getFind(), filter.getReplace(), filter.getTextColor().config.getSimpleColor()));
            } else {
                filters.add(new ReplaceFilter(filter.getFindString().config.getStringValue(), filter.getReplaceTo().config.getStringValue().replaceAll("&", "§"), filter.getFind(), filter.getReplace(), null));
            }
        }
        if (filter.getSound() != Filter.NotifySound.NONE) {
            filters.add(new NotifyFilter(filter.getFindString().config.getStringValue(), filter.getFind(), filter.getSound(), (float) filter.getSoundVolume().config.getDoubleValue(), (float) filter.getSoundPitch().config.getDoubleValue()));
        }
        if (filter.getReplaceBackgroundColor().config.getBooleanValue()) {
            filters.add(new ColorFilter(filter.getFindString().config.getStringValue(), filter.getFind(), filter.getBackgroundColor().config.getSimpleColor()));
        }
        return filters;
    }
}

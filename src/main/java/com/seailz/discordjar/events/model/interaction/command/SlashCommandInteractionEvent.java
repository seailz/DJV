package com.seailz.discordjar.events.model.interaction.command;

import com.seailz.discordjar.DiscordJar;
import com.seailz.discordjar.model.interaction.data.command.ResolvedCommandOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SlashCommandInteractionEvent extends CommandInteractionEvent {

    public SlashCommandInteractionEvent(@NotNull DiscordJar bot, long sequence, @NotNull JSONObject data) {
        super(bot, sequence, data);
    }

    /**
     * Returns the options that were passed to the command.
     *
     * @return List of {@link ResolvedCommandOption} objects containing the options.
     */
    @Nullable
    public List<ResolvedCommandOption> getOptions() {
        System.out.println(getJson());
        if (!getJson().getJSONObject("d").getJSONObject("data").has("options")) return null;
        JSONArray options = getJson().getJSONObject("d").getJSONObject("data").getJSONArray("options");
        List<ResolvedCommandOption> decompiled = new ArrayList<>();
        for (Object option : options) {
            decompiled.add(ResolvedCommandOption.decompile((JSONObject) option, getCommandData().resolved()));
        }
        return decompiled;
    }

    public ResolvedCommandOption getOption(String name) {
        System.out.println(getJson());
        if (getOptions() == null) return null;
        for (ResolvedCommandOption option : getOptions()) {
            if (option.name().equals(name)) return option;
        }
        return null;
    }

}

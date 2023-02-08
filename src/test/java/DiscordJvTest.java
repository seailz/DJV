import com.seailz.discordjar.DiscordJar;
import com.seailz.discordjar.model.application.Intent;
import com.seailz.discordjar.model.status.Status;
import com.seailz.discordjar.model.status.StatusType;
import com.seailz.discordjar.model.status.activity.Activity;
import com.seailz.discordjar.model.status.activity.ActivityType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.concurrent.ExecutionException;

public class DiscordJvTest {

    // This method is used for testing purposes only
    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        String token = "";
        File file = new File("token.txt");
        // get first line of that file
        try (FileReader reader = new FileReader(file)) {
            token = new BufferedReader(reader).readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        DiscordJar discordJar = new DiscordJar(token, EnumSet.of(Intent.GUILDS, Intent.GUILD_MESSAGES));

        ArrayList<Activity> activities = new ArrayList<>();
        activities.add(
                new Activity("Hello World2", ActivityType.WATCHING)
        );
        Status status = new Status(0, activities.toArray(new Activity[0]), StatusType.DO_NOT_DISTURB, false);
        discordJar.setStatus(status);

        discordJar.clearCommands();
    }

}

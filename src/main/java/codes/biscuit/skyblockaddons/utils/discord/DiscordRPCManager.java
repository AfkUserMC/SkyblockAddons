package codes.biscuit.skyblockaddons.utils.discord;

import codes.biscuit.skyblockaddons.SkyblockAddons;
import codes.biscuit.skyblockaddons.core.Location;
import codes.biscuit.skyblockaddons.core.SkyblockDate;
import codes.biscuit.skyblockaddons.utils.EnumUtils;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.RichPresence;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.fml.common.FMLLog;
import org.json.JSONObject;

import java.time.OffsetDateTime;
import java.util.Timer;
import java.util.TimerTask;

public class DiscordRPCManager implements IPCListener {

    @Getter @Setter private EnumUtils.DiscordStatusEntry currentEntry;

    private static final long APPLICATION_ID = 653443797182578707L;
    private static final long UPDATE_PERIOD = 4200L;

    private final SkyblockAddons main;
    private IPCClient client;
    private DiscordStatus detailsLine;
    private DiscordStatus stateLine;
    private OffsetDateTime startTimestamp;

    private Timer updateTimer;
    private boolean connected;

    public DiscordRPCManager(final SkyblockAddons main) {
        this.main = main;
    }

    public void start() {
        try {
            FMLLog.info("Starting Discord RP...");
            if (isActive()) {
                return;
            }

            stateLine = main.getConfigValues().getDiscordStatus();
            detailsLine = main.getConfigValues().getDiscordDetails();
            startTimestamp = OffsetDateTime.now();
            client = new IPCClient(APPLICATION_ID);
            client.setListener(this);
            try {
                client.connect();
            } catch (Exception e) {
                FMLLog.warning("Failed to connect to Discord RPC: %s", e.getMessage());
            }
        } catch (Throwable ex) {
            main.getLogger().error("DiscordRP has thrown an unexpected error while trying to start...");
            ex.printStackTrace();
        }
    }

    public void stop() {
        if (isActive()) {
            client.close();
            connected = false;
        }
    }

    public boolean isActive() {
        return client != null && connected;
    }

    public void updatePresence() {
        Location location = SkyblockAddons.getInstance().getUtils().getLocation();
        SkyblockDate skyblockDate = SkyblockAddons.getInstance().getUtils().getCurrentDate();
        String skyblockDateString = skyblockDate != null ? skyblockDate.toString() : "";

        // Early Winter 10th, 12:10am - Village
        String largeImageDescription = String.format("%s - %s", skyblockDateString, location.getScoreboardName());
        String smallImageDescription = String.format("Using SkyblockAddons v%s", SkyblockAddons.VERSION+" by Biscuit | Icons by Hypixel Packs HQ");
        RichPresence presence = new RichPresence.Builder()
                .setState(stateLine.getDisplayString(EnumUtils.DiscordStatusEntry.STATE))
                .setDetails(detailsLine.getDisplayString(EnumUtils.DiscordStatusEntry.DETAILS))
                .setStartTimestamp(startTimestamp)
                .setLargeImage(location.getDiscordIconKey(), largeImageDescription)
                .setSmallImage("skyblockicon", smallImageDescription)
                .build();
        client.sendRichPresence(presence);
    }

    public void setStateLine(DiscordStatus status) {
        this.stateLine = status;
        if (isActive()) {
            updatePresence();
        }
    }

    public void setDetailsLine(DiscordStatus status) {
        this.detailsLine = status;
        if (isActive()) {
            updatePresence();
        }
    }

    @Override
    public void onReady(IPCClient client) {
        FMLLog.info("Discord RPC started");
        connected = true;
        updateTimer = new Timer();
        updateTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                updatePresence();
            }
        }, 0, UPDATE_PERIOD);
    }

    @Override
    public void onClose(IPCClient client, JSONObject json) {
        FMLLog.warning("Discord RPC closed");
        this.client = null;
        connected = false;
        cancelTimer();
    }

    @Override
    public void onDisconnect(IPCClient client, Throwable t) {
        FMLLog.warning("Discord RPC disconnected");
        this.client = null;
        connected = false;
        cancelTimer();
    }

    private void cancelTimer() {
        if(updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }
    }
}
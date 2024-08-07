package com.memeasaur.potpissersdefault.Classes;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getFocusComponent;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getNormalComponent;

public abstract class AbstractData {
    protected abstract void executeNetworkMessage(Component msg);
    public abstract boolean isInSpawn();
    protected abstract CompletableFuture<String> fetchName();

    public final UUID uuid;

    private transient final WeakHashMap<AbstractData, DuelOptions> outgoingDuelRequests = new WeakHashMap<>();
    public boolean outgoingDuelRequestsContains(AbstractData abstractData) {
        return outgoingDuelRequests.containsKey(abstractData);
    }
    protected AbstractData(UUID uuid) {
        this.uuid = uuid;
    }

    public void executeDuelRequest(AbstractData duelerArg, Player p, DuelOptions duelOptions, Player pArg) {
        if (duelerArg.outgoingDuelRequests.get(this) instanceof DuelOptions duelOptionsArg && duelOptions.getNullableFinalDuelOptions(duelOptionsArg) instanceof DuelOptions duelOptions1)
            executeHandleDuel(duelerArg, duelOptions1);
        else {
            outgoingDuelRequests.put(duelerArg, duelOptions);
            (duelerArg instanceof AbstractPartyData abstractPartyData ? abstractPartyData.fetchName() : CompletableFuture.completedFuture(null))
                    .thenAccept(pArgName -> {
                        Component pMessage = getNormalComponent(p.getName() + " has sent a duel request to " + pArgName + "(" + pArg.getName() + ")");
                        if (this instanceof AbstractPartyData abstractPartyData)
                            abstractPartyData.executeNetworkMessage(pMessage);
                        else
                            p.sendMessage(pMessage);
                    });

            (this instanceof AbstractPartyData abstractPartyData ? abstractPartyData.fetchName() : CompletableFuture.completedFuture(p.getName()))
                    .thenAccept(pName -> {
                        if (duelerArg instanceof AbstractPartyData abstractPartyData)
                            abstractPartyData.executeNetworkMessage(getFocusComponent(pName + " has sent your party a duel request. type /p duel (any " + pName + " member) to accept"));
                        else
                            pArg.sendMessage(getFocusComponent(pName + " has sent you a duel request. type /duel (" + pName + ") to accept"));
                    });
        }
    }
    protected abstract CompletableFuture<Void> fetchHandleDuelMapVoid(ConcurrentHashMap<UUID, String> map);
    public void executeHandleDuel(AbstractData duelerArg, DuelOptions duelOptions1) {
        ConcurrentHashMap<UUID, String> map = new ConcurrentHashMap<>();

        CompletableFuture.allOf(this.fetchHandleDuelMapVoid(map), duelerArg.fetchHandleDuelMapVoid(map))
                .thenAccept(v -> new DuelTracker(duelOptions1, map));
    }

    protected abstract boolean isSubclassQueued();
    public boolean isQueued() {
        return !this.outgoingDuelRequests.isEmpty() || isSubclassQueued();
    } // TODO -> private mutableNetworkParty and change all uses to these

    protected abstract void handleSubclassQueueCancel();
    public void handleQueueCancel() {
        fetchName()
                .thenAccept(name -> outgoingDuelRequests
                        .keySet().forEach(abstractData -> {
                            abstractData.executeNetworkMessage(getNormalComponent(name + " has revoked their duel request"));
                            outgoingDuelRequests.clear();
                        }));
        handleSubclassQueueCancel();
        executeNetworkMessage(getNormalComponent("dequeued"));
    }
}

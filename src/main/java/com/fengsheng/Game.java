package com.fengsheng;

import com.fengsheng.card.Deck;
import com.fengsheng.network.Network;
import com.fengsheng.skill.Skill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Game {
    private static Map<Integer, Game> gameCache = new HashMap<>();
    private static AtomicInteger increaseId = new AtomicInteger();

    private final int id;
    private final Player[] players;
    private final Deck deck = new Deck(this);
    private Fsm fsm;
    private final List<Skill> listeningSkills = new ArrayList<>();

    public Game(int totalPlayerCount) {
        id = increaseId.incrementAndGet();
        players = new Player[totalPlayerCount];
    }

    public Player[] getPlayers() {
        return players;
    }

    public Deck getDeck() {
        return deck;
    }

    public Fsm getFsm() {
        return fsm;
    }

    public static void main(String[] args) {
        Network.init();
    }
}

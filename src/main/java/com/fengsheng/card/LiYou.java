package com.fengsheng.card;

import com.fengsheng.*;
import com.fengsheng.phase.MainPhaseIdle;
import com.fengsheng.phase.OnUseCard;
import com.fengsheng.protos.Common;
import com.fengsheng.protos.Fengsheng;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class LiYou extends AbstractCard {
    private static final Logger log = Logger.getLogger(LiYou.class);

    public LiYou(int id, Common.color[] colors, Common.direction direction, boolean lockable) {
        super(id, colors, direction, lockable);
    }

    public LiYou(int id, AbstractCard card) {
        super(id, card);
    }

    @Override
    public Common.card_type getType() {
        return Common.card_type.Li_You;
    }

    @Override
    public boolean canUse(Game g, Player r, Object... args) {
        Player target = (Player) args[0];
        return LiYou.canUse(g, r, target);
    }

    public static boolean canUse(Game g, Player r, Player target) {
        if (!(g.getFsm() instanceof MainPhaseIdle fsm) || r != fsm.player()) {
            log.error("利诱的使用时机不对");
            return false;
        }
        if (!target.isAlive()) {
            log.error("目标已死亡");
            return false;
        }
        return true;
    }

    @Override
    public void execute(final Game g, final Player r, Object... args) {
        final Player target = (Player) args[0];
        log.info(r + "对" + target + "使用了" + this);
        r.deleteCard(this.id);
        LiYou.execute(this, g, r, target);
    }

    /**
     * 执行【利诱】的效果
     *
     * @param card 使用的那张【利诱】卡牌。可以为 {@code null} ，因为肥原龙川技能【诡诈】可以视为使用了【利诱】。
     */
    public static void execute(LiYou card, final Game g, final Player r, final Player target) {
        Fsm resolveFunc = () -> {
            Card[] deckCards = g.getDeck().draw(1);
            boolean joinIntoHand = false;
            if (deckCards.length > 0) {
                target.addMessageCard(deckCards);
                if (target.checkThreeSameMessageCard(deckCards[0].getColors().toArray(new Common.color[0]))) {
                    target.deleteMessageCard(deckCards[0].getId());
                    joinIntoHand = true;
                    r.addCard(deckCards);
                    log.info(Arrays.toString(deckCards) + "加入了" + r + "的手牌");
                } else {
                    log.info(Arrays.toString(deckCards) + "加入了" + target + "的情报区");
                }
            }
            for (Player player : g.getPlayers()) {
                if (player instanceof HumanPlayer p) {
                    var builder = Fengsheng.use_li_you_toc.newBuilder();
                    builder.setPlayerId(p.getAlternativeLocation(r.location()));
                    builder.setTargetPlayerId(p.getAlternativeLocation(target.location()));
                    if (card != null) builder.setLiYouCard(card.toPbCard());
                    builder.setJoinIntoHand(joinIntoHand);
                    if (deckCards.length > 0) builder.setMessageCard(deckCards[0].toPbCard());
                    p.send(builder.build());
                }
            }
            if (card != null) g.getDeck().discard(card);
            return new ResolveResult(new MainPhaseIdle(r), true);
        };
        if (card != null)
            g.resolve(new OnUseCard(r, r, card, r, resolveFunc));
        else
            g.resolve(resolveFunc);
    }

    @Override
    public String toString() {
        return Card.cardColorToString(colors) + "利诱";
    }

    public static boolean ai(MainPhaseIdle e, Card card) {
        Player player = e.player();
        List<Player> players = new ArrayList<>();
        for (Player p : player.getGame().getPlayers())
            if (p.isAlive()) players.add(p);
        if (players.isEmpty()) return false;
        Player p = players.get(ThreadLocalRandom.current().nextInt(players.size()));
        GameExecutor.post(player.getGame(), () -> card.execute(player.getGame(), player, p), 2, TimeUnit.SECONDS);
        return true;
    }
}

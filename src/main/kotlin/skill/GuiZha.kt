package com.fengsheng.skill

import com.fengsheng.*
import com.fengsheng.card.LiYou
import com.fengsheng.card.WeiBi
import com.fengsheng.card.count
import com.fengsheng.card.countTrueCard
import com.fengsheng.phase.MainPhaseIdle
import com.fengsheng.protos.Common.card_type
import com.fengsheng.protos.Common.color.Black
import com.fengsheng.protos.Common.secret_task.Disturber
import com.fengsheng.protos.Role.skill_gui_zha_toc
import com.fengsheng.protos.Role.skill_gui_zha_tos
import com.google.protobuf.GeneratedMessageV3
import org.apache.logging.log4j.kotlin.logger
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * 肥原龙川技能【诡诈】：出牌阶段限一次，你可以指定一名角色，然后视为你对其使用了一张【威逼】或【利诱】。
 */
class GuiZha : MainPhaseSkill() {
    override val skillId = SkillId.GUI_ZHA

    override val isInitialSkill = true

    override fun executeProtocol(g: Game, r: Player, message: GeneratedMessageV3) {
        if (r !== (g.fsm as? MainPhaseIdle)?.whoseTurn) {
            logger.error("现在不是出牌阶段空闲时点")
            (r as? HumanPlayer)?.sendErrorMessage("现在不是出牌阶段空闲时点")
            return
        }
        if (r.getSkillUseCount(skillId) > 0) {
            logger.error("[诡诈]一回合只能发动一次")
            (r as? HumanPlayer)?.sendErrorMessage("[诡诈]一回合只能发动一次")
            return
        }
        val pb = message as skill_gui_zha_tos
        if (r is HumanPlayer && !r.checkSeq(pb.seq)) {
            logger.error("操作太晚了, required Seq: ${r.seq}, actual Seq: ${pb.seq}")
            r.sendErrorMessage("操作太晚了")
            return
        }
        if (pb.targetPlayerId < 0 || pb.targetPlayerId >= g.players.size) {
            logger.error("目标错误")
            (r as? HumanPlayer)?.sendErrorMessage("目标错误")
            return
        }
        val target = g.players[r.getAbstractLocation(pb.targetPlayerId)]!!
        if (!target.alive) {
            logger.error("目标已死亡")
            (r as? HumanPlayer)?.sendErrorMessage("目标已死亡")
            return
        }
        if (pb.cardType == card_type.Wei_Bi) {
            if (!WeiBi.canUse(g, r, target, pb.wantType)) return
        } else if (pb.cardType == card_type.Li_You) {
            if (!LiYou.canUse(g, r, target)) return
        } else {
            logger.error("你只能视为使用了[威逼]或[利诱]：${pb.cardType}")
            (r as? HumanPlayer)?.sendErrorMessage("你只能视为使用了[威逼]或[利诱]：${pb.cardType}")
            return
        }
        r.incrSeq()
        r.addSkillUseCount(skillId)
        logger.info("${r}对${target}发动了[诡诈]")
        for (p in g.players) {
            if (p is HumanPlayer) {
                val builder = skill_gui_zha_toc.newBuilder()
                builder.playerId = p.getAlternativeLocation(r.location)
                builder.targetPlayerId = p.getAlternativeLocation(target.location)
                builder.cardType = pb.cardType
                p.send(builder.build())
            }
        }
        if (pb.cardType == card_type.Wei_Bi) WeiBi.execute(null, g, r, target, pb.wantType)
        else if (pb.cardType == card_type.Li_You) LiYou.execute(null, g, r, target)
    }

    companion object {
        fun ai(e: MainPhaseIdle, skill: ActiveSkill): Boolean {
            val player = e.whoseTurn
            player.getSkillUseCount(SkillId.GUI_ZHA) == 0 || return false
            val game = player.game!!
            val nextCard = game.deck.peek(1).firstOrNull()
            var target: Player? = null
            if (player.identity == Black && player.secretTask == Disturber) { // 如果是搅局者，优先选择真情报最少的玩家
                if (!game.isEarly) {
                    target = game.players.filter { it !== player && it!!.alive }.run {
                        minOf { it!!.messageCards.countTrueCard() }.let { minCount ->
                            filter { it!!.messageCards.countTrueCard() == minCount }.run {
                                filter { it!!.messageCards.count(Black) < 2 }.ifEmpty { this }
                            }.randomOrNull()
                        }
                    }
                }
            } else if (game.isEarly || nextCard == null || Random.nextInt(4) == 0) { // 1/4的概率选自己
                target = player
            } else {
                var value = 0
                for (p in game.sortedFrom(game.players, player.location)) {
                    p.alive || continue
                    val result = player.calculateMessageCardValue(player, p, nextCard)
                    if (result > value) {
                        value = result
                        target = p
                    }
                }
            }
            target ?: return false
            GameExecutor.post(game, {
                val builder = skill_gui_zha_tos.newBuilder()
                builder.cardType = card_type.Li_You
                builder.targetPlayerId = e.whoseTurn.getAlternativeLocation(target.location)
                skill.executeProtocol(game, e.whoseTurn, builder.build())
            }, 3, TimeUnit.SECONDS)
            return true
        }
    }
}
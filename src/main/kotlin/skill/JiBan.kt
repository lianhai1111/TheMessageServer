package com.fengsheng.skill

import com.fengsheng.*
import com.fengsheng.RobotPlayer.Companion.bestCard
import com.fengsheng.phase.MainPhaseIdle
import com.fengsheng.protos.Role.*
import com.google.protobuf.GeneratedMessageV3
import org.apache.logging.log4j.kotlin.logger
import java.util.concurrent.TimeUnit

/**
 * SP顾小梦技能【羁绊】：出牌阶段限一次，可以摸两张牌，然后将至少一张手牌交给另一名角色。
 */
class JiBan : MainPhaseSkill() {
    override val skillId = SkillId.JI_BAN

    override val isInitialSkill = true

    override fun executeProtocol(g: Game, r: Player, message: GeneratedMessageV3) {
        if (r !== (g.fsm as? MainPhaseIdle)?.whoseTurn) {
            logger.error("现在不是出牌阶段空闲时点")
            (r as? HumanPlayer)?.sendErrorMessage("现在不是出牌阶段空闲时点")
            return
        }
        if (r.getSkillUseCount(skillId) > 0) {
            logger.error("[羁绊]一回合只能发动一次")
            (r as? HumanPlayer)?.sendErrorMessage("[羁绊]一回合只能发动一次")
            return
        }
        val pb = message as skill_ji_ban_a_tos
        if (r is HumanPlayer && !r.checkSeq(pb.seq)) {
            logger.error("操作太晚了, required Seq: ${r.seq}, actual Seq: ${pb.seq}")
            r.sendErrorMessage("操作太晚了")
            return
        }
        r.incrSeq()
        r.addSkillUseCount(skillId)
        g.resolve(executeJiBan(g.fsm!!, r))
    }

    private data class executeJiBan(val fsm: Fsm, val r: Player) : WaitingFsm {
        override fun resolve(): ResolveResult? {
            val g = r.game!!
            logger.info("${r}发动了[羁绊]")
            r.draw(2)
            for (p in g.players) {
                if (p is HumanPlayer) {
                    val builder = skill_ji_ban_a_toc.newBuilder()
                    builder.playerId = p.getAlternativeLocation(r.location)
                    builder.waitingSecond = Config.WaitSecond
                    if (p === r) {
                        val seq2: Int = p.seq
                        builder.seq = seq2
                        p.timeout = GameExecutor.post(
                            g,
                            { if (p.checkSeq(seq2)) autoSelect(seq2) },
                            p.getWaitSeconds(builder.waitingSecond + 2).toLong(),
                            TimeUnit.SECONDS
                        )
                    }
                    p.send(builder.build())
                }
            }
            if (r is RobotPlayer) GameExecutor.post(g, { autoSelect(0) }, 1, TimeUnit.SECONDS)
            return null
        }

        override fun resolveProtocol(player: Player, message: GeneratedMessageV3): ResolveResult? {
            if (player !== r) {
                logger.error("不是你发技能的时机")
                (player as? HumanPlayer)?.sendErrorMessage("不是你发技能的时机")
                return null
            }
            if (message !is skill_ji_ban_b_tos) {
                logger.error("错误的协议")
                (player as? HumanPlayer)?.sendErrorMessage("错误的协议")
                return null
            }
            val g = r.game!!
            if (r is HumanPlayer && !r.checkSeq(message.seq)) {
                logger.error("操作太晚了, required Seq: ${r.seq}, actual Seq: ${message.seq}")
                r.sendErrorMessage("操作太晚了")
                return null
            }
            if (message.cardIdsCount == 0) {
                logger.error("至少需要选择一张卡牌")
                (player as? HumanPlayer)?.sendErrorMessage("至少需要选择一张卡牌")
                return null
            }
            if (message.targetPlayerId < 0 || message.targetPlayerId >= g.players.size) {
                logger.error("目标错误")
                (player as? HumanPlayer)?.sendErrorMessage("目标错误")
                return null
            }
            if (message.targetPlayerId == 0) {
                logger.error("不能以自己为目标")
                (player as? HumanPlayer)?.sendErrorMessage("不能以自己为目标")
                return null
            }
            val target = g.players[r.getAbstractLocation(message.targetPlayerId)]!!
            if (!target.alive) {
                logger.error("目标已死亡")
                (player as? HumanPlayer)?.sendErrorMessage("目标已死亡")
                return null
            }
            val cards = List(message.cardIdsCount) {
                val card = r.findCard(message.getCardIds(it))
                if (card == null) {
                    logger.error("没有这张卡")
                    (player as? HumanPlayer)?.sendErrorMessage("没有这张卡")
                    return null
                }
                card
            }
            r.incrSeq()
            logger.info("${r}将${cards.joinToString()}交给$target")
            r.cards.removeAll(cards.toSet())
            target.cards.addAll(cards)
            for (p in g.players) {
                if (p is HumanPlayer) {
                    val builder = skill_ji_ban_b_toc.newBuilder()
                    builder.playerId = p.getAlternativeLocation(r.location)
                    builder.targetPlayerId = p.getAlternativeLocation(target.location)
                    if (p === r || p === target) {
                        for (card in cards) builder.addCards(card.toPbCard())
                    } else {
                        builder.unknownCardCount = cards.size
                    }
                    p.send(builder.build())
                }
            }
            g.addEvent(GiveCardEvent(r, r, target))
            return ResolveResult(fsm, true)
        }

        private fun autoSelect(seq: Int) {
            val availableTargets = r.game!!.players.filter { it!!.alive && it !== r } // 如果所有人都死了游戏就结束了，所以这里一定不为空
            val players =
                if (seq != 0 || r.game!!.isEarly) availableTargets
                else availableTargets.filter { r.isPartner(it!!) }.ifEmpty {
                    r.weiBiSuccessfulRate = 4
                    availableTargets
                } // 机器人优先选队友
            val player = players.random()!!
            val builder = skill_ji_ban_b_tos.newBuilder()
            if (seq != 0) builder.addCardIds(r.cards.random().id)
            else builder.addCardIds(r.cards.bestCard(r.identity, true).id)
            builder.seq = seq
            builder.targetPlayerId = r.getAlternativeLocation(player.location)
            r.game!!.tryContinueResolveProtocol(r, builder.build())
        }
    }

    companion object {
        fun ai(e: MainPhaseIdle, skill: ActiveSkill): Boolean {
            if (e.whoseTurn.getSkillUseCount(SkillId.JI_BAN) > 0) return false
            GameExecutor.post(e.whoseTurn.game!!, {
                skill.executeProtocol(e.whoseTurn.game!!, e.whoseTurn, skill_ji_ban_a_tos.getDefaultInstance())
            }, 3, TimeUnit.SECONDS)
            return true
        }
    }
}
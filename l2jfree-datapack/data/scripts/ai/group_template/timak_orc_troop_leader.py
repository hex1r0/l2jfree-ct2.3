import sys
from com.l2jfree.gameserver.gameobjects.ai import CtrlIntention
from com.l2jfree.gameserver.model.quest.jython import QuestJython as JQuest
from com.l2jfree.gameserver.network.packets.server import NpcSay
from com.l2jfree.tools.random import Rnd

# timak_orc_troop_leader
class timak_orc_troop_leader(JQuest) :

    # init function.  Add in here variables that you'd like to be inherited by subclasses (if any)
    def __init__(self, id, name, descr):
        self.timak_orc_troop_leader = 20767
        self.FirstAttacked = False
        # finally, don't forget to call the parent constructor to prepare the event triggering
        # mechanisms etc.
        JQuest.__init__(self, id, name, descr)

    def onAttack (self, npc, player, damage, isPet, skill) :
        objId = npc.getObjectId()
        if self.FirstAttacked :
           if Rnd.get(50) : return
           npc.broadcastPacket(NpcSay(objId, 0, npc.getNpcId(), "Destroy the enemy, my brothers!"))
        else :
           self.FirstAttacked = True
        return 

    def onKill (self, npc, player, isPet) :
        npcId = npc.getNpcId()
        if npcId == self.timak_orc_troop_leader :
            objId = npc.getObjectId()
            self.FirstAttacked = False
        elif self.FirstAttacked :
            self.addSpawn(npcId, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), True, 0)
        return 

QUEST = timak_orc_troop_leader(-1, "timak_orc_troop_leader", "ai")

QUEST.addKillId(QUEST.timak_orc_troop_leader)

QUEST.addAttackId(QUEST.timak_orc_troop_leader)
#
# Created by DraX on 2005.07.27. updated by DrLecter.
#
import sys

from net.sf.l2j.gameserver.model.actor.instance import L2PcInstance
from net.sf.l2j.gameserver.model.quest          import State
from net.sf.l2j.gameserver.model.quest          import QuestState
from net.sf.l2j.gameserver.model.quest.jython   import QuestJython as JQuest
qn = "1101_teleport_to_race_track"

RACE_MANAGER = 30995

TELEPORTERS = {
    30059:3,    # TRISHA
    30080:4,    # CLARISSA
    30177:6,    # VALENTIA
    30233:8,    # ESMERALDA
    30256:2,    # BELLA
    30320:1,    # RICHLIN
    30848:7,    # ELISA
    30899:5,    # FLAUEN
    31275:10,   # TATIANA
    31320:9,    # ILYANA
    31964:13,   # BILIA
    30836:12,   # MINERVA
    30727:11    # VERONA
}

RETURN_LOCS = [[-80826,149775,-3043],[-12672,122776,-3116],[15670,142983,-2705],[83400,147943,-3404], \
              [111409,219364,-3545],[82956,53162,-1495],[146331,25762,-2018],[116819,76994,-2714], \
              [43835,-47749,-792],[147930,-55281,-2728],[85335,16177,-3694],[105857,109763,-3202], \
              [87386,-143246,-1293]]


class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onTalk (self,npc,player):
   st = player.getQuestState(qn)
   npcId = npc.getNpcId()
   if not st: return
   
   ###################
   # Start Locations #
   ###################
   if TELEPORTERS.has_key(npcId) :
     st.getPlayer().teleToLocation(12661,181687,-3560)
     st.setState(STARTED)
     st.set("id",str(TELEPORTERS[npcId]))     
     return

   ############################
   # Monster Derby Race Track #
   ############################
   if st.getState() == STARTED and npcId == RACE_MANAGER:
       # back to start location
       return_id = st.getInt("id") - 1
       st.getPlayer().teleToLocation(RETURN_LOCS[return_id][0],RETURN_LOCS[return_id][1],RETURN_LOCS[return_id][2])
       st.exitQuest(1)
       return

QUEST       = Quest(1101,qn,"Teleports")
CREATED     = State('Start',     QUEST)
STARTED     = State('Started',   QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

for npcId in TELEPORTERS.keys() :
    QUEST.addStartNpc(npcId)
    QUEST.addTalkId(npcId)

QUEST.addTalkId(RACE_MANAGER)

print "importing teleport data: 1101_teleport_to_race_track"
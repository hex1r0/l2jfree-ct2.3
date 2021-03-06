#
# Created by DraX on 2005.08.08
#

import sys

from com.l2jfree.gameserver.model.quest        import State
from com.l2jfree.gameserver.model.quest        import QuestState
from com.l2jfree.gameserver.model.quest.jython import QuestJython as JQuest
from com.l2jfree.gameserver.gameobjects.base import ClassId
from com.l2jfree.gameserver.gameobjects.base import Race

qn = "30358_thifiell_occupation_change"
TETRARCH_THIFIELL = 30358

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onAdvEvent(self,event,npc, player) :
   htmltext = event
   st = player.getQuestState(qn)
   if not st : return

   if event == "30358-01.htm":
     return "30358-01.htm"

   if event == "30358-02.htm":
     return "30358-02.htm"

   if event == "30358-03.htm":
     return "30358-03.htm"

   if event == "30358-04.htm":
     return "30358-04.htm"

   if event == "30358-05.htm":
     return "30358-05.htm"

   if event == "30358-06.htm":
     return "30358-06.htm"

   if event == "30358-07.htm":
     return "30358-07.htm"

   if event == "30358-08.htm":
     return "30358-08.htm"

   if event == "30358-09.htm":
     return "30358-09.htm"

   if event == "30358-10.htm":
     return "30358-10.htm"

 def onTalk (self,npc,player):
   st = player.getQuestState(qn)
   npcId = npc.getNpcId()

   race    = st.getPlayer().getRace()
   classId = st.getPlayer().getClassId()

   # DarkElfs got accepted
   if npcId == TETRARCH_THIFIELL and race in [Race.Darkelf]:
     if classId in [ClassId.DarkFighter]:
       st.setState(State.STARTED)
       return "30358-01.htm"
     if classId in [ClassId.DarkMystic]:
       st.setState(State.STARTED)
       return "30358-02.htm"
     if classId in [ClassId.DarkWizard, ClassId.ShillienOracle, ClassId.PalusKnight, ClassId.Assassin]:
       st.exitQuest(False)
       st.exitQuest(1)
       return "30358-12.htm"
     else:
       st.exitQuest(False)
       st.exitQuest(1)
       return "30358-13.htm"

   # All other Races must be out
   if npcId == TETRARCH_THIFIELL and race in [Race.Dwarf, Race.Human, Race.Elf, Race.Orc, Race.Kamael]:
     st.exitQuest(False)
     st.exitQuest(1)
     return "30358-11.htm"

QUEST     = Quest(30358,qn,"village_master")



QUEST.addStartNpc(30358)

QUEST.addTalkId(30358)